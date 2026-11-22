const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const FARMER_ALICE_UID = "alice_caficultor_123";
const FARMER_BOB_UID = "bob_caficultor_456";
const ENGINEER_UID = "ing_carlos_789";
const PROVIDER_UID = "prov_insumos_321";
const BUYER_UID = "comp_coop_654";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

const ROLE_LABELS = {
  caficultor: "Caficultor",
  agronomo: "Ingeniero agrónomo",
  proveedor: "Proveedor",
  comprador: "Comprador",
};

function permissionsFor(roleId) {
  return {
    manageOwnFarms: roleId === "caficultor",
    logOwnCropActivities: roleId === "caficultor",
    recordOwnHarvests: roleId === "caficultor",
    calculateOwnCosts: roleId === "caficultor",
    manageCollectors: roleId === "caficultor",
    createInquiries: roleId === "caficultor",
    registerAgronomyVisits: roleId === "agronomo",
    replyInquiries: roleId === "agronomo",
    publishProviderCatalog: roleId === "proveedor",
    publishBuyerPrices: roleId === "comprador",
    recordCoffeePrices: roleId === "caficultor" || roleId === "comprador",
    viewCommunity: true,
  };
}

function buildUserDoc(uid, roleId, fullName, location = "San José de Albán, Nariño", ts = new Date(Date.now() - 10000)) {
  return {
    userId: uid,
    fullName,
    email: `${uid}@example.com`,
    phone: "3124567890",
    location,
    roleId,
    role: ROLE_LABELS[roleId],
    roleLocked: true,
    permissions: permissionsFor(roleId),
    createdAt: ts,
    updatedAt: ts,
  };
}

async function seedUsersWithRoles() {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("roles").doc("caficultor").set({
      roleId: "caficultor",
      displayName: "Caficultor",
      aliasLabel: "Agricultor",
      description: "Gestiona sus propias fincas, bitácoras, cosechas y costos.",
      isActive: true,
      sortOrder: 1,
      permissions: permissionsFor("caficultor"),
    });
    await db.collection("roles").doc("agronomo").set({
      roleId: "agronomo",
      displayName: "Ingeniero agrónomo",
      aliasLabel: "Agrónomo",
      description: "Registra visitas técnicas y responde inquietudes.",
      isActive: true,
      sortOrder: 2,
      permissions: permissionsFor("agronomo"),
    });
    await db.collection("users").doc(FARMER_ALICE_UID).set(buildUserDoc(FARMER_ALICE_UID, "caficultor", "Alicia Gómez"));
    await db.collection("users").doc(FARMER_BOB_UID).set(buildUserDoc(FARMER_BOB_UID, "caficultor", "Roberto Díaz"));
    await db.collection("users").doc(ENGINEER_UID).set(buildUserDoc(ENGINEER_UID, "agronomo", "Ing. Carlos Muñoz"));
    await db.collection("users").doc(PROVIDER_UID).set(buildUserDoc(PROVIDER_UID, "proveedor", "Agroinsumos Nariño"));
    await db.collection("users").doc(BUYER_UID).set(buildUserDoc(BUYER_UID, "comprador", "Cooperativa del Norte"));
  });
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write any collection including /roles", async () => {
  await seedUsersWithRoles();
  const unauth = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 1000);

  await assertFails(unauth.collection("roles").where("isActive", "==", true).get());
  await assertFails(unauth.collection("fincas").where("isPublic", "==", true).get());
  await assertFails(unauth.collection("cultivos").where("isPublic", "==", true).get());
  await assertFails(unauth.collection("visitas_agronomicas").where("isPublic", "==", true).get());
  await assertFails(unauth.collection("inquietudes").where("isPublic", "==", true).get());
  await assertFails(unauth.collection("proveedores").where("isPublic", "==", true).get());
  await assertFails(unauth.collection("compradores").where("isPublic", "==", true).get());
  await assertFails(unauth.collection("users").doc(FARMER_ALICE_UID).collection("fincas").get());
  await assertFails(
    unauth.collection("fincas").doc("finca_1").set({
      userId: FARMER_ALICE_UID,
      isPublic: true,
      recordType: "finca",
      recordCode: "FIN-001",
      name: "La Esperanza",
      location: "Vereda El Rosal",
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("/roles catalog is readable by authenticated users (filtered) and immutable from client", async () => {
  await seedUsersWithRoles();
  const aliceDb = testEnv.authenticatedContext(FARMER_ALICE_UID).firestore();

  await assertSucceeds(aliceDb.collection("roles").doc("caficultor").get());
  await assertSucceeds(aliceDb.collection("roles").where("isActive", "==", true).get());
  await assertFails(aliceDb.collection("roles").get());
  await assertFails(
    aliceDb.collection("roles").doc("admin").set({
      roleId: "admin",
      displayName: "Admin",
      isActive: true,
    })
  );
  await assertFails(
    aliceDb.collection("roles").doc("caficultor").update({
      isActive: false,
    })
  );
});

test("User registration locks role permanently; forged permissions rejected and post-signup role switch denied", async () => {
  const newUserUid = "nuevo_usuario_1";
  const newUserDb = testEnv.authenticatedContext(newUserUid).firestore();
  const otherDb = testEnv.authenticatedContext(FARMER_BOB_UID).firestore();
  const ts = new Date(Date.now() - 2000);

  // Reject registration with mismatched permissions or roleLocked != true
  await assertFails(
    newUserDb.collection("users").doc(newUserUid).set({
      ...buildUserDoc(newUserUid, "caficultor", "María Fernanda Gómez", "Vereda El Rosal", ts),
      permissions: permissionsFor("agronomo"),
    })
  );
  await assertFails(
    newUserDb.collection("users").doc(newUserUid).set({
      ...buildUserDoc(newUserUid, "caficultor", "María Fernanda Gómez", "Vereda El Rosal", ts),
      roleLocked: false,
    })
  );
  await assertFails(
    newUserDb.collection("users").doc(newUserUid).set({
      ...buildUserDoc(newUserUid, "caficultor", "María Fernanda Gómez", "Vereda El Rosal", ts),
      role: "Proveedor",
    })
  );

  // Valid creation succeeds
  await assertSucceeds(
    newUserDb.collection("users").doc(newUserUid).set(
      buildUserDoc(newUserUid, "caficultor", "María Fernanda Gómez", "Vereda El Rosal", ts)
    )
  );

  // Profile editable fields (name, phone, location, farmName) can be updated
  await assertSucceeds(
    newUserDb.collection("users").doc(newUserUid).update({
      fullName: "María F. Gómez",
      farmName: "Finca La Palma",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Role, roleId, roleLocked, and permissions MUST be strictly immutable after registration
  await assertFails(
    newUserDb.collection("users").doc(newUserUid).update({
      role: "Proveedor",
      updatedAt: new Date(Date.now() - 300),
    })
  );
  await assertFails(
    newUserDb.collection("users").doc(newUserUid).update({
      roleId: "agronomo",
      role: "Ingeniero agrónomo",
      permissions: permissionsFor("agronomo"),
      updatedAt: new Date(Date.now() - 200),
    })
  );
  await assertFails(
    newUserDb.collection("users").doc(newUserUid).update({
      roleLocked: false,
      updatedAt: new Date(Date.now() - 100),
    })
  );

  // Listing /users or reading another user's private profile must fail
  await assertFails(newUserDb.collection("users").get());
  await assertFails(otherDb.collection("users").doc(newUserUid).get());
});

test("Shared /fincas: only Caficultor can CRUD own farms; Agronomo/Proveedor/Comprador & peers can read only", async () => {
  await seedUsersWithRoles();
  const aliceDb = testEnv.authenticatedContext(FARMER_ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(FARMER_BOB_UID).firestore();
  const ingDb = testEnv.authenticatedContext(ENGINEER_UID).firestore();
  const provDb = testEnv.authenticatedContext(PROVIDER_UID).firestore();
  const compDb = testEnv.authenticatedContext(BUYER_UID).firestore();
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("fincas").doc("finca_alice_1").set({
      userId: FARMER_ALICE_UID,
      isPublic: true,
      ownerName: "Alicia Gómez",
      recordType: "finca",
      recordCode: "FIN-001",
      name: "Finca El Mirador",
      subtitle: "Caturra & Castillo",
      location: "San José de Albán, Nariño",
      areaHectares: 4.5,
      latitude: 1.473,
      longitude: -77.07,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Non-caficultor roles CANNOT create farms
  for (const [uid, db] of [
    [ENGINEER_UID, ingDb],
    [PROVIDER_UID, provDb],
    [BUYER_UID, compDb],
  ]) {
    await assertFails(
      db.collection("fincas").doc(`finca_bad_${uid}`).set({
        userId: uid,
        isPublic: true,
        recordType: "finca",
        recordCode: "FIN-999",
        name: "Finca No Autorizada",
        location: "Albán",
        createdAt: created,
        updatedAt: created,
      })
    );
  }

  // All authenticated roles can read community farms (filtered)
  await assertSucceeds(bobDb.collection("fincas").where("isPublic", "==", true).get());
  await assertSucceeds(ingDb.collection("fincas").where("isPublic", "==", true).get());
  await assertSucceeds(provDb.collection("fincas").where("isPublic", "==", true).get());
  await assertSucceeds(compDb.collection("fincas").where("isPublic", "==", true).get());
  await assertFails(bobDb.collection("fincas").get());

  // Owner partial update succeeds; another farmer cannot update/delete
  await assertSucceeds(
    aliceDb.collection("fincas").doc("finca_alice_1").update({
      areaHectares: 5.0,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    bobDb.collection("fincas").doc("finca_alice_1").update({
      name: "Secuestrada",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(bobDb.collection("fincas").doc("finca_alice_1").delete());

  // Reject ghost field / @DocumentId id / incomplete coordinates
  await assertFails(
    aliceDb.collection("fincas").doc("finca_bad_ghost").set({
      userId: FARMER_ALICE_UID,
      isPublic: true,
      recordType: "finca",
      recordCode: "FIN-002",
      name: "Finca Fantasma",
      location: "Albán",
      isVerified: true,
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertFails(
    aliceDb.collection("fincas").doc("finca_bad_geo").set({
      userId: FARMER_ALICE_UID,
      isPublic: true,
      recordType: "finca",
      recordCode: "FIN-003",
      name: "Finca Incompleta",
      location: "Albán",
      latitude: 1.47,
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertFails(
    aliceDb.collection("fincas").doc("finca_bad_id").set({
      id: "finca_bad_id",
      userId: FARMER_ALICE_UID,
      isPublic: true,
      recordType: "finca",
      recordCode: "FIN-004",
      name: "Finca ID",
      location: "Albán",
      createdAt: created,
      updatedAt: created,
    })
  );
});

test("Shared /cultivos: only Caficultor owner logs own farm activities; non-caficultor writes and cross-farm writes rejected", async () => {
  await seedUsersWithRoles();
  const aliceDb = testEnv.authenticatedContext(FARMER_ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(FARMER_BOB_UID).firestore();
  const ingDb = testEnv.authenticatedContext(ENGINEER_UID).firestore();
  const ts = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("fincas").doc("finca_alice_1").set({
      userId: FARMER_ALICE_UID,
      isPublic: true,
      ownerName: "Alicia Gómez",
      recordType: "finca",
      recordCode: "FIN-001",
      name: "Finca El Mirador",
      location: "San José de Albán",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("cultivos").doc("cul_1").set({
      userId: FARMER_ALICE_UID,
      isPublic: true,
      authorName: "Alicia Gómez",
      recordCode: "CUL-001",
      activityType: "Poda",
      name: "Poda de zoca lote sur",
      farmId: "finca_alice_1",
      farmCode: "FIN-001",
      farmName: "Finca El Mirador",
      date: "2026-11-22",
      startTime: "07:00",
      endTime: "11:30",
      quantity: 120,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Community can visualize cultivation logbook
  await assertSucceeds(bobDb.collection("cultivos").where("isPublic", "==", true).get());
  await assertSucceeds(ingDb.collection("cultivos").where("isPublic", "==", true).get());

  // Bob cannot add a cultivo activity claiming Alice's farmId
  await assertFails(
    bobDb.collection("cultivos").doc("cul_forged").set({
      userId: FARMER_BOB_UID,
      isPublic: true,
      authorName: "Roberto",
      recordCode: "CUL-002",
      activityType: "Abono",
      name: "Falsa actividad",
      farmId: "finca_alice_1",
      farmCode: "FIN-001",
      date: "2026-11-22",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Engineer cannot log a cultivo activity (role restriction)
  await assertFails(
    ingDb.collection("cultivos").doc("cul_eng").set({
      userId: ENGINEER_UID,
      isPublic: true,
      authorName: "Ing. Carlos",
      recordCode: "CUL-003",
      activityType: "Abono",
      name: "Actividad de ingeniero",
      farmCode: "FIN-001",
      date: "2026-11-22",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertFails(
    bobDb.collection("cultivos").doc("cul_1").update({
      name: "Modificado",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(bobDb.collection("cultivos").doc("cul_1").delete());

  await assertSucceeds(
    aliceDb.collection("cultivos").doc("cul_1").update({
      activityType: "Control fitosanitario",
      updatedAt: new Date(Date.now() - 300),
    })
  );
});

test("Shared /visitas_agronomicas & /inquietudes: strict role enforcement between Agronomo and Caficultor", async () => {
  await seedUsersWithRoles();
  const aliceDb = testEnv.authenticatedContext(FARMER_ALICE_UID).firestore();
  const ingDb = testEnv.authenticatedContext(ENGINEER_UID).firestore();
  const bobDb = testEnv.authenticatedContext(FARMER_BOB_UID).firestore();
  const provDb = testEnv.authenticatedContext(PROVIDER_UID).firestore();
  const ts = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("fincas").doc("finca_alice_1").set({
      userId: FARMER_ALICE_UID,
      isPublic: true,
      ownerName: "Alicia Gómez",
      recordType: "finca",
      recordCode: "FIN-001",
      name: "Finca El Mirador",
      location: "Vereda El Rosal",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Engineer logs agronomic visit on Alice's farm
  await assertSucceeds(
    ingDb.collection("visitas_agronomicas").doc("vis_1").set({
      userId: ENGINEER_UID,
      isPublic: true,
      recordCode: "AGR-001",
      engineerName: "Ing. Carlos Muñoz",
      farmId: "finca_alice_1",
      farmCode: "FIN-001",
      farmName: "Finca El Mirador",
      farmOwnerId: FARMER_ALICE_UID,
      farmOwnerName: "Alicia Gómez",
      location: "Vereda El Rosal",
      date: "2026-11-22",
      diagnosis: "Incidencia leve de roya en lote alto (4%)",
      activitiesPerformed: "Muestreo en zig-zag de 50 árboles y calibración de boquilla",
      recommendations: "Aplicar caldo bordelés y fertilización con potasio tras poda.",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Caficultor or Proveedor CANNOT register agronomic visits
  await assertFails(
    aliceDb.collection("visitas_agronomicas").doc("vis_bad_farmer").set({
      userId: FARMER_ALICE_UID,
      isPublic: true,
      recordCode: "AGR-002",
      engineerName: "Alicia",
      farmId: "finca_alice_1",
      date: "2026-11-22",
      diagnosis: "Intento de caficultor",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(aliceDb.collection("visitas_agronomicas").where("isPublic", "==", true).get());
  await assertSucceeds(provDb.collection("visitas_agronomicas").where("isPublic", "==", true).get());

  // Caficultor creates inquiry; Proveedor cannot create inquiry
  await assertSucceeds(
    aliceDb.collection("inquietudes").doc("inq_1").set({
      userId: FARMER_ALICE_UID,
      isPublic: true,
      recordCode: "INQ-001",
      authorName: "Alicia Gómez",
      authorRole: "Caficultor",
      farmId: "finca_alice_1",
      farmCode: "FIN-001",
      farmName: "Finca El Mirador",
      subject: "Hojas amarillas en lote joven",
      question: "¿Puede ser falta de nitrógeno o exceso de humedad?",
      status: "abierta",
      date: "2026-11-22",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertFails(
    provDb.collection("inquietudes").doc("inq_prov").set({
      userId: PROVIDER_UID,
      isPublic: true,
      recordCode: "INQ-002",
      authorName: "Proveedor",
      authorRole: "Proveedor",
      subject: "Consulta no permitida",
      question: "Texto de prueba",
      status: "abierta",
      date: "2026-11-22",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Only Agronomo can answer inquiries (Bob farmer cannot reply)
  await assertFails(
    bobDb.collection("inquietudes").doc("inq_1").update({
      replyText: "Respuesta no autorizada",
      replyByUid: FARMER_BOB_UID,
      replyByName: "Roberto",
      replyDate: "2026-11-22",
      status: "respondida",
      updatedAt: new Date(Date.now() - 400),
    })
  );
  await assertSucceeds(
    ingDb.collection("inquietudes").doc("inq_1").update({
      replyText: "Revisa drenaje en cabeceras y realiza análisis foliar antes de aplicar urea.",
      replyByUid: ENGINEER_UID,
      replyByName: "Ing. Carlos Muñoz",
      replyDate: "2026-11-22",
      status: "respondida",
      updatedAt: new Date(Date.now() - 300),
    })
  );
});

test("Private user-scoped collections enforce role ownership (Cosechas, Costos, Recolectores, Precios Cafe)", async () => {
  await seedUsersWithRoles();
  const aliceDb = testEnv.authenticatedContext(FARMER_ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(FARMER_BOB_UID).firestore();
  const provDb = testEnv.authenticatedContext(PROVIDER_UID).firestore();
  const compDb = testEnv.authenticatedContext(BUYER_UID).firestore();
  const ts = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("users").doc(FARMER_ALICE_UID).collection("cosechas").doc("cos_1").set({
      userId: FARMER_ALICE_UID,
      recordCode: "CSE-001",
      workerName: "Carlos Muñoz",
      farmCode: "FIN-001",
      farmName: "Finca El Mirador",
      date: "2026-11-21",
      quantityKg: 84.5,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertFails(
    provDb.collection("users").doc(PROVIDER_UID).collection("cosechas").doc("cos_prov").set({
      userId: PROVIDER_UID,
      recordCode: "CSE-002",
      workerName: "Carlos",
      farmCode: "FIN-001",
      date: "2026-11-21",
      quantityKg: 50,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(FARMER_ALICE_UID).collection("costos").doc("cost_1").set({
      userId: FARMER_ALICE_UID,
      recordCode: "COS-001",
      name: "Cosecha principal",
      farmCode: "FIN-001",
      date: "2026-11-21",
      estimatedKg: 500,
      offerPricePerKg: 18500,
      totalAmount: 4200000,
      lines: [{ category: "Insumos", item: "Fertilizante NPK", qty: 10, unit: 180000 }],
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(FARMER_ALICE_UID).collection("recolectores").doc("rec_1").set({
      userId: FARMER_ALICE_UID,
      recordCode: "REC-001",
      contactName: "Mateo Ortega",
      contactEmail: "mateo@example.com",
      contactPhone: "3120001122",
      location: "San José de Albán",
      experience: "1 a 3 años",
      availability: "Desde diciembre",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Coffee price history allowed for Caficultor and Comprador, denied for Proveedor
  await assertSucceeds(
    compDb.collection("users").doc(BUYER_UID).collection("precios_cafe").doc("prc_1").set({
      userId: BUYER_UID,
      recordCode: "PRC-001",
      date: "2026-11-21",
      source: "FNC",
      pricePerCargaCop: 2150000,
      pricePerKgCop: 17200,
      nyFuturesCentsLb: 285.45,
      usdCopTrm: 4380.25,
      notes: "Factor 94 pergamino seco",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertFails(
    provDb.collection("users").doc(PROVIDER_UID).collection("precios_cafe").doc("prc_prov").set({
      userId: PROVIDER_UID,
      recordCode: "PRC-002",
      date: "2026-11-21",
      source: "FNC",
      pricePerCargaCop: 2150000,
      pricePerKgCop: 17200,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertFails(bobDb.collection("users").doc(FARMER_ALICE_UID).collection("recolectores").get());
  await assertFails(bobDb.collection("users").doc(FARMER_ALICE_UID).collection("cosechas").get());
  await assertFails(bobDb.collection("users").doc(FARMER_ALICE_UID).collection("costos").get());
  await assertFails(aliceDb.collection("users").doc(BUYER_UID).collection("precios_cafe").get());
});

test("Proveedores & Compradores: strictly role-scoped writes, community read-only", async () => {
  await seedUsersWithRoles();
  const aliceDb = testEnv.authenticatedContext(FARMER_ALICE_UID).firestore();
  const provDb = testEnv.authenticatedContext(PROVIDER_UID).firestore();
  const compDb = testEnv.authenticatedContext(BUYER_UID).firestore();
  const ts = new Date(Date.now() - 2000);

  // Only Proveedor can write /proveedores
  await assertSucceeds(
    provDb.collection("proveedores").doc("prov_1").set({
      userId: PROVIDER_UID,
      isPublic: true,
      ownerName: "Agroinsumos Nariño",
      recordCode: "PRO-001",
      name: "Fertilizante Producción 17-6-18-2",
      category: "Fertilizantes y abonos",
      productsServices: "Bulto 50 kg NPK granulado para cafetales en producción",
      priceReference: "$168.000 COP / bulto",
      commercialContact: "Calle Real Centro · 3104445566",
      location: "Centro, San José de Albán",
      latitude: 1.474,
      longitude: -77.072,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertFails(
    aliceDb.collection("proveedores").doc("prov_bad").set({
      userId: FARMER_ALICE_UID,
      isPublic: true,
      recordCode: "PRO-002",
      name: "No permitido",
      productsServices: "Solo proveedor",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Only Comprador can write /compradores
  await assertSucceeds(
    compDb.collection("compradores").doc("comp_1").set({
      userId: BUYER_UID,
      isPublic: true,
      actorType: "comprador",
      recordCode: "COM-001",
      name: "Cooperativa Cafetera del Norte",
      purchaseType: "Pergamino seco",
      offerAmountCop: 19200,
      priceUnit: "kg pergamino seco",
      priceDate: "2026-11-22",
      location: "San José de Albán",
      latitude: 1.475,
      longitude: -77.071,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertFails(
    provDb.collection("compradores").doc("comp_bad").set({
      userId: PROVIDER_UID,
      isPublic: true,
      actorType: "comprador",
      recordCode: "COM-002",
      name: "No permitido",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // All signed-in roles can browse
  await assertSucceeds(aliceDb.collection("proveedores").where("isPublic", "==", true).get());
  await assertSucceeds(aliceDb.collection("compradores").where("isPublic", "==", true).get());
  await assertFails(aliceDb.collection("proveedores").get());
  await assertFails(aliceDb.collection("compradores").get());
});
