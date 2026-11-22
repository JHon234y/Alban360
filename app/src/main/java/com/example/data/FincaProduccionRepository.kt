package com.example.data

import android.content.Context
import android.util.Log
import com.example.R
import com.example.firebase.OperationType
import com.example.firebase.handleFirestoreError
import com.example.model.AppRole
import com.example.model.BitacoraRecord
import com.example.model.CosechaRecord
import com.example.model.CultivoActivity
import com.example.model.FincaProfile
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.concurrent.ConcurrentHashMap

/**
 * Repositorio CRUD optimizado por rol de usuario para las colecciones de producción cafetera
 * en Cloud Firestore:
 * - **Fincas** (`/fincas`): Lectura comunitaria (`isPublic == true`) para todos los roles;
 *   escritura y eliminación exclusivas del rol [AppRole.CAFICULTOR] sobre fincas propias.
 * - **Cosechas** (`/users/{userId}/cosechas`): Subcolección privada exclusiva de [AppRole.CAFICULTOR].
 *   Para [AppRole.AGRONOMO], [AppRole.PROVEEDOR] y [AppRole.COMPRADOR] evita abrir listeners
 *   innecesarios en Firestore emitiendo listas vacías de forma inmediata.
 * - **Bitácora de cultivo** (`/cultivos`, [BitacoraRecord]): Lectura comunitaria (`isPublic == true`)
 *   para todos los roles (incluyendo consulta por finca para visitas de agrónomos); creación,
 *   edición y eliminación exclusivas de [AppRole.CAFICULTOR] sobre sus registros y fincas propias.
 */
class FincaProduccionRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    companion object {
        const val COLLECTION_FINCAS = "fincas"
        const val COLLECTION_CULTIVOS = "cultivos" // Colección de bitácora de cultivo
        const val COLLECTION_USERS = "users"
        const val SUBCOLLECTION_COSECHAS = "cosechas"
        const val SUBCOLLECTION_LEGACY_FINCAS = "fincas"
        const val SUBCOLLECTION_LEGACY_CULTIVOS = "cultivos"

        val VALID_CULTIVO_ACTIVITY_TYPES = setOf(
            "Siembra",
            "Abono",
            "Poda",
            "Control fitosanitario",
            "Cosecha",
        )
    }

    private val cachedRolesByUid = ConcurrentHashMap<String, AppRole>()

    fun cacheUserRole(userId: String, role: AppRole) {
        if (userId.isNotBlank()) cachedRolesByUid[userId] = role
    }

    fun clearCachedRole(userId: String? = null) {
        if (userId.isNullOrBlank()) cachedRolesByUid.clear()
        else cachedRolesByUid.remove(userId)
    }

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Debes iniciar sesión con Google para operar en Firestore.")
    }

    private suspend fun resolveEffectiveRole(uid: String, explicitRole: AppRole?): AppRole {
        if (explicitRole != null) {
            cachedRolesByUid[uid] = explicitRole
            return explicitRole
        }
        cachedRolesByUid[uid]?.let { return it }
        val userSnap = db.collection(COLLECTION_USERS).document(uid).get().await()
        if (!userSnap.exists()) {
            throw SecurityException("Tu perfil y rol permanente aún no están registrados en Firestore.")
        }
        val resolved = AppRole.fromRoleOrId(
            userSnap.getString("roleId"),
            userSnap.getString("role"),
        )
        cachedRolesByUid[uid] = resolved
        return resolved
    }

    private fun requireRoleCapability(
        role: AppRole,
        allowed: Boolean,
        actionDescription: String,
    ) {
        if (!allowed) {
            throw SecurityException(
                "El rol ${role.displayName} no tiene permisos para $actionDescription."
            )
        }
    }

    private fun requireOwnership(
        currentUid: String,
        recordUserId: String?,
        entityLabel: String,
    ) {
        if (!recordUserId.isNullOrBlank() && recordUserId != currentUid) {
            throw SecurityException("Solo el caficultor propietario puede modificar o eliminar $entityLabel.")
        }
    }

    private fun <T> Flow<T>.logAndRethrowOnError(
        path: String,
        uid: String?,
        operationType: OperationType = OperationType.LIST,
    ): Flow<T> = catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && (uid == null || auth.currentUser?.uid == uid)) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
        throw error
    }

    // =========================================================================
    // FINCAS (/fincas) — CRUD optimizado por rol
    // =========================================================================

    /**
     * Observa en tiempo real las fincas visibles según el rol:
     * - Para todos los roles con `viewCommunity`: lectura de fincas públicas (`isPublic == true`).
     * - Si `onlyOwned == true` y es [AppRole.CAFICULTOR], filtra en servidor por `userId == userId`.
     */
    fun observeFincasPorRol(
        userId: String,
        role: AppRole,
        onlyOwned: Boolean = false,
    ): Flow<List<FincaProfile>> {
        if (!role.capabilities.viewCommunity && !role.capabilities.manageOwnFarms) {
            return flowOf(emptyList())
        }
        if (onlyOwned && !role.capabilities.manageOwnFarms) {
            return flowOf(emptyList())
        }
        return if (onlyOwned) observeFincasDeUsuario(userId) else observeFincas(userId)
    }

    /**
     * Observa en tiempo real todas las fincas públicas de la red comunitaria.
     * Filtra por `isPublic == true` para coincidir 1:1 con `firestore.rules`.
     */
    fun observeFincas(userId: String): Flow<List<FincaProfile>> = flow {
        require(userId.isNotBlank()) { "El userId no puede estar vacío." }
        emitAll(
            db.collection(COLLECTION_FINCAS)
                .whereEqualTo("isPublic", true)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        FincaProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .logAndRethrowOnError(COLLECTION_FINCAS, userId)
        )
    }

    /**
     * Observa en tiempo real únicamente las fincas pertenecientes al caficultor indicado.
     */
    fun observeFincasDeUsuario(userId: String): Flow<List<FincaProfile>> = flow {
        require(userId.isNotBlank()) { "El userId no puede estar vacío." }
        emitAll(
            db.collection(COLLECTION_FINCAS)
                .whereEqualTo("isPublic", true)
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        FincaProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .logAndRethrowOnError(COLLECTION_FINCAS, userId)
        )
    }

    /**
     * Observa en tiempo real una finca específica por su ID de documento.
     */
    fun observeFincaPorId(fincaId: String, userId: String): Flow<FincaProfile?> = flow {
        require(userId.isNotBlank()) { "El userId no puede estar vacío." }
        require(fincaId.isNotBlank()) { "El fincaId no puede estar vacío." }
        val docRef = db.collection(COLLECTION_FINCAS).document(fincaId)
        emitAll(
            docRef.snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(
                            FincaProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
                .logAndRethrowOnError(docRef.path, userId, OperationType.GET)
        )
    }

    /**
     * Lectura puntual (one-shot) de fincas optimizada según el rol.
     */
    suspend fun readFincasPorRol(
        role: AppRole? = null,
        onlyOwned: Boolean = false,
    ): Result<List<FincaProfile>> {
        return try {
            val uid = requireUserId()
            val effectiveRole = resolveEffectiveRole(uid, role)
            if (onlyOwned && !effectiveRole.capabilities.manageOwnFarms) {
                return Result.success(emptyList())
            }
            val baseQuery = db.collection(COLLECTION_FINCAS).whereEqualTo("isPublic", true)
            val query = if (onlyOwned) baseQuery.whereEqualTo("userId", uid) else baseQuery
            val snap = query.get().await()
            val items = snap.toObjects(
                FincaProfile::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ).sortedByDescending { it.createdAt?.seconds ?: 0L }
            Result.success(items)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, COLLECTION_FINCAS)
            Result.failure(e)
        }
    }

    /**
     * Lectura puntual de una finca por su ID de documento.
     */
    suspend fun obtenerFincaPorId(fincaId: String): Result<FincaProfile?> {
        val docRef = db.collection(COLLECTION_FINCAS).document(fincaId.ifBlank { "_" })
        return try {
            requireUserId()
            require(fincaId.isNotBlank()) { "El identificador de la finca es obligatorio." }
            val snap = docRef.get().await()
            val finca = if (snap.exists()) {
                snap.toObject(
                    FincaProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            } else null
            Result.success(finca)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    /**
     * Crea una nueva finca en `/fincas` validando que el usuario tenga rol [AppRole.CAFICULTOR].
     * Retorna el ID del documento generado.
     */
    suspend fun createFinca(item: FincaProfile, role: AppRole? = null): Result<String> {
        val docRef = db.collection(COLLECTION_FINCAS).document()
        return runValueMutation(OperationType.CREATE, docRef.path) {
            val uid = requireUserId()
            val effectiveRole = resolveEffectiveRole(uid, role)
            requireRoleCapability(
                role = effectiveRole,
                allowed = effectiveRole.capabilities.manageOwnFarms,
                actionDescription = "crear fincas",
            )
            validateFinca(item)
            docRef.set(item.toCreateMap(uid)).await()
            docRef.id
        }
    }

    /**
     * Actualiza una finca existente en `/fincas` conservando `userId` y `createdAt` inmutables.
     */
    suspend fun updateFinca(item: FincaProfile, role: AppRole? = null): Result<Unit> {
        val docRef = db.collection(COLLECTION_FINCAS).document(item.id.ifBlank { "_" })
        return runMutation(OperationType.UPDATE, docRef.path) {
            val uid = requireUserId()
            require(item.id.isNotBlank()) { "El ID de la finca es obligatorio para actualizar." }
            val effectiveRole = resolveEffectiveRole(uid, role)
            requireRoleCapability(
                role = effectiveRole,
                allowed = effectiveRole.capabilities.manageOwnFarms,
                actionDescription = "editar fincas",
            )
            requireOwnership(uid, item.userId, "esta finca")
            validateFinca(item)
            docRef.update(item.toUpdateMap()).await()
        }
    }

    /**
     * Crea o actualiza una finca en `/fincas` según si `item.id` está vacío o no.
     */
    suspend fun saveFinca(item: FincaProfile, role: AppRole? = null): Result<Unit> {
        return if (item.id.isBlank()) {
            createFinca(item, role).map { }
        } else {
            updateFinca(item, role)
        }
    }

    /**
     * Actualiza únicamente las coordenadas geográficas de una finca existente del caficultor.
     */
    suspend fun updateFincaCoordenadas(
        fincaId: String,
        latitude: Double,
        longitude: Double,
        role: AppRole? = null,
        ownerUserId: String? = null,
    ): Result<Unit> {
        val docRef = db.collection(COLLECTION_FINCAS).document(fincaId.ifBlank { "_" })
        return runMutation(OperationType.UPDATE, docRef.path) {
            val uid = requireUserId()
            require(fincaId.isNotBlank()) { "El ID de la finca es obligatorio." }
            val effectiveRole = resolveEffectiveRole(uid, role)
            requireRoleCapability(
                role = effectiveRole,
                allowed = effectiveRole.capabilities.manageOwnFarms,
                actionDescription = "actualizar la ubicación de fincas",
            )
            requireOwnership(uid, ownerUserId, "esta finca")
            require(
                latitude in -90.0..90.0 && longitude in -180.0..180.0 &&
                    !(latitude == 0.0 && longitude == 0.0)
            ) {
                "Las coordenadas geográficas están fuera de rango."
            }
            docRef.update(
                mapOf(
                    "latitude" to latitude,
                    "longitude" to longitude,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
        }
    }

    /**
     * Elimina una finca propia en `/fincas` (exclusivo de [AppRole.CAFICULTOR]).
     */
    suspend fun deleteFinca(
        id: String,
        role: AppRole? = null,
        ownerUserId: String? = null,
    ): Result<Unit> {
        val docRef = db.collection(COLLECTION_FINCAS).document(id.ifBlank { "_" })
        return runMutation(OperationType.DELETE, docRef.path) {
            val uid = requireUserId()
            require(id.isNotBlank()) { "El ID de la finca es obligatorio para eliminar." }
            val effectiveRole = resolveEffectiveRole(uid, role)
            requireRoleCapability(
                role = effectiveRole,
                allowed = effectiveRole.capabilities.manageOwnFarms,
                actionDescription = "eliminar fincas",
            )
            requireOwnership(uid, ownerUserId, "esta finca")
            docRef.delete().await()
        }
    }

    // =========================================================================
    // COSECHAS (/users/{userId}/cosechas) — CRUD optimizado por rol
    // =========================================================================

    /**
     * Observa en tiempo real las cosechas según el rol:
     * Solo el rol [AppRole.CAFICULTOR] posee subcolección de cosechas propia; otros roles
     * reciben `emptyList()` sin consumir lecturas ni abrir listeners en Firestore.
     */
    fun observeCosechasPorRol(userId: String, role: AppRole): Flow<List<CosechaRecord>> {
        if (!role.capabilities.recordOwnHarvests) {
            return flowOf(emptyList())
        }
        return observeCosechas(userId)
    }

    /**
     * Observa en tiempo real los registros de cosecha privados del usuario autenticado.
     */
    fun observeCosechas(userId: String): Flow<List<CosechaRecord>> = flow {
        require(userId.isNotBlank()) { "El userId no puede estar vacío." }
        val path = "$COLLECTION_USERS/$userId/$SUBCOLLECTION_COSECHAS"
        emitAll(
            db.collection(COLLECTION_USERS)
                .document(userId)
                .collection(SUBCOLLECTION_COSECHAS)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        CosechaRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(
                        compareByDescending<CosechaRecord> { it.date }
                            .thenByDescending { it.createdAt?.seconds ?: 0L }
                    )
                }
                .logAndRethrowOnError(path, userId)
        )
    }

    /**
     * Observa en tiempo real los registros de cosecha filtrados por una finca específica.
     */
    fun observeCosechasPorFinca(
        userId: String,
        farmCode: String,
        role: AppRole = AppRole.CAFICULTOR,
    ): Flow<List<CosechaRecord>> {
        if (!role.capabilities.recordOwnHarvests) {
            return flowOf(emptyList())
        }
        return flow {
            require(userId.isNotBlank()) { "El userId no puede estar vacío." }
            require(farmCode.isNotBlank()) { "El código de finca no puede estar vacío." }
            val path = "$COLLECTION_USERS/$userId/$SUBCOLLECTION_COSECHAS"
            emitAll(
                db.collection(COLLECTION_USERS)
                    .document(userId)
                    .collection(SUBCOLLECTION_COSECHAS)
                    .whereEqualTo("farmCode", farmCode)
                    .snapshots()
                    .map { snap ->
                        snap.toObjects(
                            CosechaRecord::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        ).sortedWith(
                            compareByDescending<CosechaRecord> { it.date }
                                .thenByDescending { it.createdAt?.seconds ?: 0L }
                        )
                    }
                    .logAndRethrowOnError(path, userId)
            )
        }
    }

    /**
     * Lectura puntual de cosechas optimizada por rol (retorna lista vacía sin consultas si el rol
     * no administra cosechas).
     */
    suspend fun readCosechasPorRol(
        role: AppRole? = null,
        farmCode: String? = null,
    ): Result<List<CosechaRecord>> {
        return try {
            val uid = requireUserId()
            val effectiveRole = resolveEffectiveRole(uid, role)
            if (!effectiveRole.capabilities.recordOwnHarvests) {
                return Result.success(emptyList())
            }
            val col = db.collection(COLLECTION_USERS).document(uid).collection(SUBCOLLECTION_COSECHAS)
            val snap = if (!farmCode.isNullOrBlank()) {
                col.whereEqualTo("farmCode", farmCode).get().await()
            } else {
                col.get().await()
            }
            val items = snap.toObjects(
                CosechaRecord::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ).sortedWith(
                compareByDescending<CosechaRecord> { it.date }
                    .thenByDescending { it.createdAt?.seconds ?: 0L }
            )
            Result.success(items)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "$COLLECTION_USERS/$SUBCOLLECTION_COSECHAS")
            Result.failure(e)
        }
    }

    /**
     * Lectura puntual de una cosecha privada por ID (exclusiva de [AppRole.CAFICULTOR]).
     */
    suspend fun readCosechaById(cosechaId: String, role: AppRole? = null): Result<CosechaRecord?> {
        return try {
            val uid = requireUserId()
            require(cosechaId.isNotBlank()) { "El identificador de la cosecha es obligatorio." }
            val effectiveRole = resolveEffectiveRole(uid, role)
            if (!effectiveRole.capabilities.recordOwnHarvests) {
                return Result.success(null)
            }
            val docRef = db.collection(COLLECTION_USERS)
                .document(uid)
                .collection(SUBCOLLECTION_COSECHAS)
                .document(cosechaId)
            val snap = docRef.get().await()
            val record = if (snap.exists()) {
                snap.toObject(
                    CosechaRecord::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            } else null
            Result.success(record)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "$COLLECTION_USERS/$SUBCOLLECTION_COSECHAS/$cosechaId")
            Result.failure(e)
        }
    }

    /**
     * Crea un registro de cosecha en `/users/{uid}/cosechas` (exclusivo de [AppRole.CAFICULTOR]).
     */
    suspend fun createCosecha(item: CosechaRecord, role: AppRole? = null): Result<String> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Sesión no iniciada"))
        val docRef = db.collection(COLLECTION_USERS).document(uid)
            .collection(SUBCOLLECTION_COSECHAS).document()
        return runValueMutation(OperationType.CREATE, docRef.path) {
            val effectiveRole = resolveEffectiveRole(uid, role)
            requireRoleCapability(
                role = effectiveRole,
                allowed = effectiveRole.capabilities.recordOwnHarvests,
                actionDescription = "registrar cosechas",
            )
            validateCosecha(item)
            docRef.set(item.toCreateMap(uid)).await()
            docRef.id
        }
    }

    /**
     * Actualiza un registro de cosecha existente en `/users/{uid}/cosechas`.
     */
    suspend fun updateCosecha(item: CosechaRecord, role: AppRole? = null): Result<Unit> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Sesión no iniciada"))
        val docRef = db.collection(COLLECTION_USERS).document(uid)
            .collection(SUBCOLLECTION_COSECHAS).document(item.id.ifBlank { "_" })
        return runMutation(OperationType.UPDATE, docRef.path) {
            require(item.id.isNotBlank()) { "El ID de la cosecha es obligatorio para actualizar." }
            val effectiveRole = resolveEffectiveRole(uid, role)
            requireRoleCapability(
                role = effectiveRole,
                allowed = effectiveRole.capabilities.recordOwnHarvests,
                actionDescription = "actualizar cosechas",
            )
            requireOwnership(uid, item.userId, "esta cosecha")
            validateCosecha(item)
            docRef.update(item.toUpdateMap()).await()
        }
    }

    /**
     * Registra o actualiza una cosecha en `/users/{uid}/cosechas`.
     */
    suspend fun saveCosecha(item: CosechaRecord, role: AppRole? = null): Result<Unit> {
        return if (item.id.isBlank()) {
            createCosecha(item, role).map { }
        } else {
            updateCosecha(item, role)
        }
    }

    /**
     * Elimina un registro de cosecha de `/users/{uid}/cosechas`.
     */
    suspend fun deleteCosecha(id: String, role: AppRole? = null): Result<Unit> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Sesión no iniciada"))
        val docRef = db.collection(COLLECTION_USERS)
            .document(uid)
            .collection(SUBCOLLECTION_COSECHAS)
            .document(id.ifBlank { "_" })
        return runMutation(OperationType.DELETE, docRef.path) {
            require(id.isNotBlank()) { "El ID de la cosecha es obligatorio para eliminar." }
            val effectiveRole = resolveEffectiveRole(uid, role)
            requireRoleCapability(
                role = effectiveRole,
                allowed = effectiveRole.capabilities.recordOwnHarvests,
                actionDescription = "eliminar cosechas",
            )
            docRef.delete().await()
        }
    }

    // =========================================================================
    // BITÁCORA DE CULTIVO (/cultivos) — CRUD optimizado por rol
    // =========================================================================

    /**
     * Observa en tiempo real la bitácora de cultivo según el rol:
     * - Todos los roles pueden consultar las bitácoras públicas comunitarias (`isPublic == true`).
     * - Si `onlyOwned == true`, solo el [AppRole.CAFICULTOR] consulta sus registros propios.
     */
    fun observeBitacoraPorRol(
        userId: String,
        role: AppRole,
        onlyOwned: Boolean = false,
    ): Flow<List<BitacoraRecord>> {
        if (!role.capabilities.viewCommunity && !role.capabilities.logOwnCropActivities) {
            return flowOf(emptyList())
        }
        if (onlyOwned && !role.capabilities.logOwnCropActivities) {
            return flowOf(emptyList())
        }
        return if (onlyOwned) observeCultivosDeUsuario(userId) else observeCultivos(userId)
    }

    /**
     * Observa en tiempo real toda la bitácora comunitaria de actividades de cultivo.
     * Filtra por `isPublic == true` para cumplir estrictamente con `firestore.rules`.
     */
    fun observeCultivos(userId: String): Flow<List<CultivoActivity>> = flow {
        require(userId.isNotBlank()) { "El userId no puede estar vacío." }
        emitAll(
            db.collection(COLLECTION_CULTIVOS)
                .whereEqualTo("isPublic", true)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        CultivoActivity::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(
                        compareByDescending<CultivoActivity> { it.date }
                            .thenByDescending { it.createdAt?.seconds ?: 0L }
                    )
                }
                .logAndRethrowOnError(COLLECTION_CULTIVOS, userId)
        )
    }

    /**
     * Observa en tiempo real únicamente las actividades registradas por el usuario indicado.
     */
    fun observeCultivosDeUsuario(userId: String): Flow<List<CultivoActivity>> = flow {
        require(userId.isNotBlank()) { "El userId no puede estar vacío." }
        emitAll(
            db.collection(COLLECTION_CULTIVOS)
                .whereEqualTo("isPublic", true)
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        CultivoActivity::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(
                        compareByDescending<CultivoActivity> { it.date }
                            .thenByDescending { it.createdAt?.seconds ?: 0L }
                    )
                }
                .logAndRethrowOnError(COLLECTION_CULTIVOS, userId)
        )
    }

    /**
     * Observa en tiempo real la bitácora de cultivo asociada a una finca por su código.
     */
    fun observeBitacoraPorFinca(userId: String, farmCode: String): Flow<List<CultivoActivity>> = flow {
        require(userId.isNotBlank()) { "El userId no puede estar vacío." }
        require(farmCode.isNotBlank()) { "El código de finca no puede estar vacío." }
        emitAll(
            db.collection(COLLECTION_CULTIVOS)
                .whereEqualTo("isPublic", true)
                .whereEqualTo("farmCode", farmCode)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        CultivoActivity::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(
                        compareByDescending<CultivoActivity> { it.date }
                            .thenByDescending { it.createdAt?.seconds ?: 0L }
                    )
                }
                .logAndRethrowOnError(COLLECTION_CULTIVOS, userId)
        )
    }

    /**
     * Lectura puntual de la bitácora de cultivo optimizada por rol y filtros opcionales.
     */
    suspend fun readBitacoraPorRol(
        role: AppRole? = null,
        farmCode: String? = null,
        onlyOwned: Boolean = false,
    ): Result<List<BitacoraRecord>> {
        return try {
            val uid = requireUserId()
            val effectiveRole = resolveEffectiveRole(uid, role)
            if (onlyOwned && !effectiveRole.capabilities.logOwnCropActivities) {
                return Result.success(emptyList())
            }
            var query = db.collection(COLLECTION_CULTIVOS).whereEqualTo("isPublic", true)
            if (onlyOwned) {
                query = query.whereEqualTo("userId", uid)
            }
            if (!farmCode.isNullOrBlank()) {
                query = query.whereEqualTo("farmCode", farmCode)
            }
            val snap = query.get().await()
            val items = snap.toObjects(
                CultivoActivity::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ).sortedWith(
                compareByDescending<CultivoActivity> { it.date }
                    .thenByDescending { it.createdAt?.seconds ?: 0L }
            )
            Result.success(items)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, COLLECTION_CULTIVOS)
            Result.failure(e)
        }
    }

    /**
     * Lectura puntual de una entrada de bitácora por su ID.
     */
    suspend fun readBitacoraById(bitacoraId: String): Result<BitacoraRecord?> {
        val docRef = db.collection(COLLECTION_CULTIVOS).document(bitacoraId.ifBlank { "_" })
        return try {
            requireUserId()
            require(bitacoraId.isNotBlank()) { "El identificador de la actividad es obligatorio." }
            val snap = docRef.get().await()
            val record = if (snap.exists()) {
                snap.toObject(
                    CultivoActivity::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            } else null
            Result.success(record)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    /**
     * Crea una entrada en la bitácora de cultivo (`/cultivos`) validando que el usuario sea
     * [AppRole.CAFICULTOR] y que, si asocia un `farmId`, la finca le pertenezca.
     */
    suspend fun createBitacora(item: BitacoraRecord, role: AppRole? = null): Result<String> {
        val docRef = db.collection(COLLECTION_CULTIVOS).document()
        return runValueMutation(OperationType.CREATE, docRef.path) {
            val uid = requireUserId()
            val effectiveRole = resolveEffectiveRole(uid, role)
            requireRoleCapability(
                role = effectiveRole,
                allowed = effectiveRole.capabilities.logOwnCropActivities,
                actionDescription = "registrar labores en la bitácora de cultivo",
            )
            validateCultivo(item)
            if (item.farmId.isNotBlank()) {
                val farmSnap = db.collection(COLLECTION_FINCAS).document(item.farmId).get().await()
                if (!farmSnap.exists() || farmSnap.getString("userId") != uid) {
                    throw SecurityException("Solo puedes registrar labores en fincas de tu propiedad.")
                }
            }
            docRef.set(item.toCreateMap(uid)).await()
            docRef.id
        }
    }

    /**
     * Actualiza una labor existente en la bitácora de cultivo (`/cultivos`).
     */
    suspend fun updateBitacora(item: BitacoraRecord, role: AppRole? = null): Result<Unit> {
        val docRef = db.collection(COLLECTION_CULTIVOS).document(item.id.ifBlank { "_" })
        return runMutation(OperationType.UPDATE, docRef.path) {
            val uid = requireUserId()
            require(item.id.isNotBlank()) { "El ID de la actividad es obligatorio para actualizar." }
            val effectiveRole = resolveEffectiveRole(uid, role)
            requireRoleCapability(
                role = effectiveRole,
                allowed = effectiveRole.capabilities.logOwnCropActivities,
                actionDescription = "editar labores en la bitácora de cultivo",
            )
            requireOwnership(uid, item.userId, "esta labor de bitácora")
            validateCultivo(item)
            docRef.update(item.toUpdateMap()).await()
        }
    }

    /**
     * Crea o actualiza una labor en la bitácora de cultivo (`/cultivos`).
     */
    suspend fun saveCultivo(item: CultivoActivity, role: AppRole? = null): Result<Unit> {
        return if (item.id.isBlank()) {
            createBitacora(item, role).map { }
        } else {
            updateBitacora(item, role)
        }
    }

    /**
     * Elimina una actividad de la bitácora de cultivo (`/cultivos`).
     */
    suspend fun deleteCultivo(
        id: String,
        role: AppRole? = null,
        ownerUserId: String? = null,
    ): Result<Unit> {
        val docRef = db.collection(COLLECTION_CULTIVOS).document(id.ifBlank { "_" })
        return runMutation(OperationType.DELETE, docRef.path) {
            val uid = requireUserId()
            require(id.isNotBlank()) { "El ID de la actividad es obligatorio para eliminar." }
            val effectiveRole = resolveEffectiveRole(uid, role)
            requireRoleCapability(
                role = effectiveRole,
                allowed = effectiveRole.capabilities.logOwnCropActivities,
                actionDescription = "eliminar labores de la bitácora",
            )
            requireOwnership(uid, ownerUserId, "esta labor de bitácora")
            docRef.delete().await()
        }
    }

    // =========================================================================
    // MIGRACIÓN DE SUBCOLECCIONES LEGADAS DEL CAFICULTOR
    // =========================================================================

    /**
     * Migra de forma idempotente las fincas y bitácoras desde las subcolecciones privadas
     * (`/users/{uid}/fincas` y `/users/{uid}/cultivos`) hacia `/fincas` y `/cultivos`.
     */
    suspend fun migrarRegistrosLegadosCaficultor(ownerDisplayName: String) {
        val uid = auth.currentUser?.uid ?: return
        try {
            val farmCodeToId = mutableMapOf<String, String>()

            // 1) Migrar fincas primero para que las reglas exists() en /cultivos pasen al asociar farmId
            val legacyFincasSnap = db.collection(COLLECTION_USERS)
                .document(uid)
                .collection(SUBCOLLECTION_LEGACY_FINCAS)
                .get()
                .await()
            for (doc in legacyFincasSnap.documents) {
                val finca = doc.toObject(FincaProfile::class.java) ?: continue
                val targetRef = db.collection(COLLECTION_FINCAS).document(doc.id)
                val targetSnap = targetRef.get().await()
                if (!targetSnap.exists()) {
                    val payload = finca.copy(
                        ownerName = finca.ownerName.ifBlank { ownerDisplayName },
                    ).toCreateMap(uid).toMutableMap()
                    (doc.getTimestamp("createdAt") ?: finca.createdAt)?.let { ts ->
                        if (ts <= Timestamp.now()) payload["createdAt"] = ts
                    }
                    targetRef.set(payload).await()
                }
                if (finca.recordCode.isNotBlank()) {
                    farmCodeToId[finca.recordCode] = doc.id
                }
            }

            // Incluir fincas propias ya existentes en /fincas
            val ownedPublicFincasSnap = db.collection(COLLECTION_FINCAS)
                .whereEqualTo("isPublic", true)
                .whereEqualTo("userId", uid)
                .get()
                .await()
            for (doc in ownedPublicFincasSnap.documents) {
                val code = doc.getString("recordCode").orEmpty()
                if (code.isNotBlank()) farmCodeToId[code] = doc.id
            }

            // 2) Migrar bitácora de cultivo (/cultivos)
            val legacyCultivosSnap = db.collection(COLLECTION_USERS)
                .document(uid)
                .collection(SUBCOLLECTION_LEGACY_CULTIVOS)
                .get()
                .await()
            for (doc in legacyCultivosSnap.documents) {
                val cultivo = doc.toObject(CultivoActivity::class.java) ?: continue
                val targetRef = db.collection(COLLECTION_CULTIVOS).document(doc.id)
                val targetSnap = targetRef.get().await()
                if (!targetSnap.exists()) {
                    val resolvedFarmId = cultivo.farmId.ifBlank { farmCodeToId[cultivo.farmCode].orEmpty() }
                    val safeType = if (cultivo.activityType in VALID_CULTIVO_ACTIVITY_TYPES) {
                        cultivo.activityType
                    } else {
                        "Siembra"
                    }
                    val payload = cultivo.copy(
                        authorName = cultivo.authorName.ifBlank { ownerDisplayName },
                        activityType = safeType,
                        farmId = resolvedFarmId,
                    ).toCreateMap(uid).toMutableMap()
                    (doc.getTimestamp("createdAt") ?: cultivo.createdAt)?.let { ts ->
                        if (ts <= Timestamp.now()) payload["createdAt"] = ts
                    }
                    targetRef.set(payload).await()
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("FincaProduccionRepo", "Migración legada omitida o parcial", e)
        }
    }

    // =========================================================================
    // VALIDACIONES ALINEADAS CON firestore.rules
    // =========================================================================

    private fun validateFinca(item: FincaProfile) {
        require(item.recordType in setOf("finca", "caficultor")) {
            "Tipo de registro inválido para finca."
        }
        require(item.recordCode.isNotBlank()) { "El código de la finca es obligatorio." }
        require(item.name.trim().isNotEmpty()) { "El nombre de la finca es obligatorio." }
        require(item.location.trim().isNotEmpty()) { "La ubicación de la finca es obligatoria." }
        val lat = item.latitude
        val lon = item.longitude
        if (lat != null || lon != null) {
            require(
                lat != null && lon != null &&
                    lat in -90.0..90.0 && lon in -180.0..180.0 &&
                    !(lat == 0.0 && lon == 0.0)
            ) {
                "Las coordenadas geográficas de la finca son inválidas."
            }
        }
    }

    private fun validateCosecha(item: CosechaRecord) {
        require(item.recordCode.isNotBlank()) { "El código de cosecha es obligatorio." }
        require(item.workerName.trim().isNotEmpty()) { "El nombre del recolector es obligatorio." }
        require(item.farmCode.trim().isNotEmpty()) { "La finca asociada es obligatoria." }
        require(item.date.trim().isNotEmpty()) { "La fecha de cosecha es obligatoria." }
        require(item.quantityKg > 0.0) { "La cantidad cosechada debe ser mayor a cero." }
    }

    private fun validateCultivo(item: CultivoActivity) {
        require(item.recordCode.isNotBlank()) { "El código de actividad es obligatorio." }
        require(item.activityType in VALID_CULTIVO_ACTIVITY_TYPES) {
            "Tipo de labor inválido en bitácora: ${item.activityType}"
        }
        require(item.name.trim().isNotEmpty()) { "El nombre o descripción de la labor es obligatorio." }
        require(item.farmCode.trim().isNotEmpty()) { "La finca asociada es obligatoria." }
        require(item.date.trim().isNotEmpty()) { "La fecha de la labor es obligatoria." }
    }

    private suspend inline fun runMutation(
        op: OperationType,
        path: String,
        crossinline block: suspend () -> Unit,
    ): Result<Unit> = runValueMutation(op, path) { block() }

    private suspend inline fun <T> runValueMutation(
        op: OperationType,
        path: String,
        crossinline block: suspend () -> T,
    ): Result<T> = try {
        requireUserId()
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, op, path)
        Result.failure(e)
    }
}
