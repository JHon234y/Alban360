package com.example.data

import android.content.Context
import android.util.Log
import com.example.R
import com.example.firebase.OperationType
import com.example.firebase.handleFirestoreError
import com.example.model.AppRole
import com.example.model.CompradorItem
import com.example.model.CosechaRecord
import com.example.model.CostoCalculation
import com.example.model.CultivoActivity
import com.example.model.FincaProfile
import com.example.model.InquietudTecnica
import com.example.model.PrecioCafeRecord
import com.example.model.ProveedorItem
import com.example.model.RecolectorPostulacion
import com.example.model.RoleDefinition
import com.example.model.UserProfile
import com.example.model.VisitaAgronomica
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.auth.userProfileChangeRequest
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class CafeRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
    val produccionRepository: FincaProduccionRepository = FincaProduccionRepository(db, auth),
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Debes iniciar sesión con Google para operar en Firestore.")
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

    // --- USER PROFILE & PRIVATE STREAMS ---

    fun observeRoleDefinitions(): Flow<List<RoleDefinition>> = flow {
        val defaults = AppRole.entries.map { it.toDefaultDefinition() }
        val path = "roles"
        emitAll(
            db.collection("roles")
                .whereEqualTo("isActive", true)
                .snapshots()
                .map { snap ->
                    val remote = snap.toObjects(RoleDefinition::class.java)
                        .associateBy { it.roleId.ifBlank { it.id } }
                    if (remote.isEmpty()) {
                        defaults
                    } else {
                        AppRole.entries.map { appRole ->
                            remote[appRole.roleId] ?: appRole.toDefaultDefinition()
                        }.sortedBy { it.sortOrder }
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    Log.w("CafeRepository", "Fallback to local role catalog ($path)", error)
                    emit(defaults)
                }
        )
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        require(userId.isNotBlank())
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(
                            UserProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
                .logAndRethrowOnError(path, userId, OperationType.GET)
        )
    }

    fun observeCosechas(userId: String): Flow<List<CosechaRecord>> =
        produccionRepository.observeCosechas(userId)

    fun observeCostos(userId: String): Flow<List<CostoCalculation>> = flow {
        require(userId.isNotBlank())
        val path = "users/$userId/costos"
        emitAll(
            db.collection("users").document(userId).collection("costos")
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        CostoCalculation::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.date }
                }
                .logAndRethrowOnError(path, userId)
        )
    }

    fun observeRecolectores(userId: String): Flow<List<RecolectorPostulacion>> = flow {
        require(userId.isNotBlank())
        val path = "users/$userId/recolectores"
        emitAll(
            db.collection("users").document(userId).collection("recolectores")
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        RecolectorPostulacion::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .logAndRethrowOnError(path, userId)
        )
    }

    fun observePreciosCafe(userId: String): Flow<List<PrecioCafeRecord>> = flow {
        require(userId.isNotBlank())
        val path = "users/$userId/precios_cafe"
        emitAll(
            db.collection("users").document(userId).collection("precios_cafe")
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        PrecioCafeRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(
                        compareByDescending<PrecioCafeRecord> { it.date }
                            .thenByDescending { it.createdAt?.seconds ?: 0L }
                    )
                }
                .logAndRethrowOnError(path, userId)
        )
    }

    // --- SHARED COMMUNITY STREAMS (FILTERED BY isPublic == true TO MATCH RULES) ---

    fun observeFincas(userId: String): Flow<List<FincaProfile>> =
        produccionRepository.observeFincas(userId)

    fun observeCultivos(userId: String): Flow<List<CultivoActivity>> =
        produccionRepository.observeCultivos(userId)

    fun observeVisitas(userId: String): Flow<List<VisitaAgronomica>> = flow {
        require(userId.isNotBlank())
        val path = "visitas_agronomicas"
        emitAll(
            db.collection("visitas_agronomicas")
                .whereEqualTo("isPublic", true)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        VisitaAgronomica::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(
                        compareByDescending<VisitaAgronomica> { it.date }
                            .thenByDescending { it.createdAt?.seconds ?: 0L }
                    )
                }
                .logAndRethrowOnError(path, userId)
        )
    }

    fun observeInquietudes(userId: String): Flow<List<InquietudTecnica>> = flow {
        require(userId.isNotBlank())
        val path = "inquietudes"
        emitAll(
            db.collection("inquietudes")
                .whereEqualTo("isPublic", true)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        InquietudTecnica::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(
                        compareBy<InquietudTecnica> { if (it.status == "abierta") 0 else 1 }
                            .thenByDescending { it.date }
                            .thenByDescending { it.createdAt?.seconds ?: 0L }
                    )
                }
                .logAndRethrowOnError(path, userId)
        )
    }

    fun observeProveedores(userId: String): Flow<List<ProveedorItem>> = flow {
        require(userId.isNotBlank())
        val path = "proveedores"
        emitAll(
            db.collection("proveedores")
                .whereEqualTo("isPublic", true)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        ProveedorItem::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .logAndRethrowOnError(path, userId)
        )
    }

    fun observeCompradores(userId: String): Flow<List<CompradorItem>> = flow {
        require(userId.isNotBlank())
        val path = "compradores"
        emitAll(
            db.collection("compradores")
                .whereEqualTo("isPublic", true)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        CompradorItem::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(
                        compareByDescending<CompradorItem> { it.priceDate }
                            .thenByDescending { it.offerAmountCop }
                    )
                }
                .logAndRethrowOnError(path, userId)
        )
    }

    // --- IDEMPOTENT LEGACY SUBCOLLECTION MIGRATION TO SHARED COLLECTIONS ---

    suspend fun migrateLegacyUserRecordsIfNeeded(ownerDisplayName: String, role: AppRole) {
        val uid = auth.currentUser?.uid ?: return
        try {
            val farmCodeToId = mutableMapOf<String, String>()

            if (role == AppRole.CAFICULTOR) {
                produccionRepository.migrarRegistrosLegadosCaficultor(ownerDisplayName)
            } else if (role == AppRole.AGRONOMO) {
                val allFincasSnap = db.collection("fincas").whereEqualTo("isPublic", true).get().await()
                for (doc in allFincasSnap.documents) {
                    val code = doc.getString("recordCode").orEmpty()
                    if (code.isNotBlank()) farmCodeToId[code] = doc.id
                }
                // 3) Migrate legacy visitas_agronomicas
                val legacyVisitasSnap = db.collection("users").document(uid).collection("visitas_agronomicas").get().await()
                for (doc in legacyVisitasSnap.documents) {
                    val visita = doc.toObject(VisitaAgronomica::class.java) ?: continue
                    val targetRef = db.collection("visitas_agronomicas").document(doc.id)
                    val targetSnap = targetRef.get().await()
                    if (!targetSnap.exists()) {
                        val resolvedFarmId = visita.farmId.ifBlank { farmCodeToId[visita.farmCode].orEmpty() }
                        val payload = visita.copy(
                            farmId = resolvedFarmId,
                        ).toCreateMap(uid).toMutableMap()
                        (doc.getTimestamp("createdAt") ?: visita.createdAt)?.let { ts ->
                            if (ts <= Timestamp.now()) payload["createdAt"] = ts
                        }
                        targetRef.set(payload).await()
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("CafeRepository", "Legacy migration skipped or partial", e)
        }
    }

    // --- MUTATIONS (ALL RETURN Result<Unit>, RETHROW CancellationException) ---

    suspend fun saveUserProfile(profile: UserProfile): Result<Unit> {
        val currentUser = auth.currentUser
            ?: return Result.failure(IllegalStateException("Sesión no iniciada"))
        val uid = currentUser.uid
        val authEmail = currentUser.email.orEmpty()
        val docRef = db.collection("users").document(uid)
        return runMutation(OperationType.UPDATE, docRef.path) {
            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.update(profile.toUpdateMap(authEmail)).await()
            } else {
                docRef.set(profile.toCreateMap(uid, authEmail)).await()
            }
            val cleanName = profile.fullName.trim()
            if (cleanName.isNotBlank() && cleanName != currentUser.displayName) {
                runCatching {
                    currentUser.updateProfile(
                        userProfileChangeRequest { displayName = cleanName }
                    ).await()
                }
            }
        }
    }

    suspend fun saveFinca(item: FincaProfile): Result<Unit> =
        produccionRepository.saveFinca(item)

    suspend fun deleteFinca(id: String): Result<Unit> =
        produccionRepository.deleteFinca(id)

    suspend fun saveCosecha(item: CosechaRecord): Result<Unit> =
        produccionRepository.saveCosecha(item)

    suspend fun deleteCosecha(id: String): Result<Unit> =
        produccionRepository.deleteCosecha(id)

    suspend fun saveCultivo(item: CultivoActivity): Result<Unit> =
        produccionRepository.saveCultivo(item)

    suspend fun deleteCultivo(id: String): Result<Unit> =
        produccionRepository.deleteCultivo(id)

    suspend fun saveCosto(item: CostoCalculation): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Sesión no iniciada"))
        val isNew = item.id.isBlank()
        val col = db.collection("users").document(uid).collection("costos")
        val docRef = if (isNew) col.document() else col.document(item.id)
        return runMutation(if (isNew) OperationType.CREATE else OperationType.UPDATE, docRef.path) {
            if (isNew) docRef.set(item.toCreateMap(uid)).await()
            else docRef.update(item.toUpdateMap()).await()
        }
    }

    suspend fun deleteCosto(id: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Sesión no iniciada"))
        val docRef = db.collection("users").document(uid).collection("costos").document(id)
        return runMutation(OperationType.DELETE, docRef.path) {
            docRef.delete().await()
        }
    }

    suspend fun saveVisita(item: VisitaAgronomica): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Sesión no iniciada"))
        val isNew = item.id.isBlank()
        val col = db.collection("visitas_agronomicas")
        val docRef = if (isNew) col.document() else col.document(item.id)
        return runMutation(if (isNew) OperationType.CREATE else OperationType.UPDATE, docRef.path) {
            if (isNew) docRef.set(item.toCreateMap(uid)).await()
            else docRef.update(item.toUpdateMap()).await()
        }
    }

    suspend fun deleteVisita(id: String): Result<Unit> {
        val docRef = db.collection("visitas_agronomicas").document(id)
        return runMutation(OperationType.DELETE, docRef.path) {
            requireUserId()
            docRef.delete().await()
        }
    }

    suspend fun saveInquietud(item: InquietudTecnica): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Sesión no iniciada"))
        val isNew = item.id.isBlank()
        val col = db.collection("inquietudes")
        val docRef = if (isNew) col.document() else col.document(item.id)
        return runMutation(if (isNew) OperationType.CREATE else OperationType.UPDATE, docRef.path) {
            if (isNew) docRef.set(item.toCreateMap(uid)).await()
            else docRef.update(item.toAuthorUpdateMap()).await()
        }
    }

    suspend fun replyToInquietud(
        inquietudId: String,
        replyText: String,
        replyByName: String,
        replyDate: String,
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Sesión no iniciada"))
        val docRef = db.collection("inquietudes").document(inquietudId)
        return runMutation(OperationType.UPDATE, docRef.path) {
            docRef.update(
                InquietudTecnica.toReplyMap(
                    replyText = replyText,
                    replyByUid = uid,
                    replyByName = replyByName,
                    replyDate = replyDate,
                )
            ).await()
        }
    }

    suspend fun deleteInquietud(id: String): Result<Unit> {
        val docRef = db.collection("inquietudes").document(id)
        return runMutation(OperationType.DELETE, docRef.path) {
            requireUserId()
            docRef.delete().await()
        }
    }

    suspend fun saveRecolector(item: RecolectorPostulacion): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Sesión no iniciada"))
        val isNew = item.id.isBlank()
        val col = db.collection("users").document(uid).collection("recolectores")
        val docRef = if (isNew) col.document() else col.document(item.id)
        return runMutation(if (isNew) OperationType.CREATE else OperationType.UPDATE, docRef.path) {
            if (isNew) docRef.set(item.toCreateMap(uid)).await()
            else docRef.update(item.toUpdateMap()).await()
        }
    }

    suspend fun deleteRecolector(id: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Sesión no iniciada"))
        val docRef = db.collection("users").document(uid).collection("recolectores").document(id)
        return runMutation(OperationType.DELETE, docRef.path) {
            docRef.delete().await()
        }
    }

    suspend fun savePrecioCafe(item: PrecioCafeRecord): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Sesión no iniciada"))
        val isNew = item.id.isBlank()
        val col = db.collection("users").document(uid).collection("precios_cafe")
        val docRef = if (isNew) col.document() else col.document(item.id)
        return runMutation(if (isNew) OperationType.CREATE else OperationType.UPDATE, docRef.path) {
            if (isNew) docRef.set(item.toCreateMap(uid)).await()
            else docRef.update(item.toUpdateMap()).await()
        }
    }

    suspend fun deletePrecioCafe(id: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Sesión no iniciada"))
        val docRef = db.collection("users").document(uid).collection("precios_cafe").document(id)
        return runMutation(OperationType.DELETE, docRef.path) {
            docRef.delete().await()
        }
    }

    suspend fun saveProveedor(item: ProveedorItem): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Sesión no iniciada"))
        val isNew = item.id.isBlank()
        val col = db.collection("proveedores")
        val docRef = if (isNew) col.document() else col.document(item.id)
        return runMutation(if (isNew) OperationType.CREATE else OperationType.UPDATE, docRef.path) {
            if (isNew) docRef.set(item.toCreateMap(uid)).await()
            else docRef.update(item.toUpdateMap()).await()
        }
    }

    suspend fun deleteProveedor(id: String): Result<Unit> {
        val docRef = db.collection("proveedores").document(id)
        return runMutation(OperationType.DELETE, docRef.path) {
            requireUserId()
            docRef.delete().await()
        }
    }

    suspend fun saveComprador(item: CompradorItem): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Sesión no iniciada"))
        val isNew = item.id.isBlank()
        val col = db.collection("compradores")
        val docRef = if (isNew) col.document() else col.document(item.id)
        return runMutation(if (isNew) OperationType.CREATE else OperationType.UPDATE, docRef.path) {
            if (isNew) docRef.set(item.toCreateMap(uid)).await()
            else docRef.update(item.toUpdateMap()).await()
        }
    }

    suspend fun deleteComprador(id: String): Result<Unit> {
        val docRef = db.collection("compradores").document(id)
        return runMutation(OperationType.DELETE, docRef.path) {
            requireUserId()
            docRef.delete().await()
        }
    }

    private suspend inline fun runMutation(
        op: OperationType,
        path: String,
        crossinline block: suspend () -> Unit,
    ): Result<Unit> = try {
        requireUserId()
        block()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, op, path)
        Result.failure(e)
    }
}
