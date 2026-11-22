package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CafeRepository
import com.example.data.CoffeeAiRepository
import com.example.data.FincaProduccionRepository
import com.example.data.LiveMarketSnapshot
import com.example.data.TerritoryNetworkService
import com.example.model.AiUiState
import com.example.model.AppRole
import com.example.model.CompradorItem
import com.example.model.CosechaRecord
import com.example.model.CostoCalculation
import com.example.model.CostoLine
import com.example.model.CultivoActivity
import com.example.model.FincaProfile
import com.example.model.InquietudTecnica
import com.example.model.PrecioCafeRecord
import com.example.model.ProveedorItem
import com.example.model.RecolectorPostulacion
import com.example.model.RoleDefinition
import com.example.model.UiState
import com.example.model.UserProfile
import com.example.model.VisitaAgronomica
import com.example.util.formatCop
import com.example.util.formatNumber
import com.example.util.generateRecordCode
import com.example.util.nowColombiaTimestampLabel
import com.example.util.todayIsoDate
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CalculatorDraft(
    val editingId: String = "",
    val recordCode: String = "",
    val name: String = "",
    val farmCode: String = "",
    val farmName: String = "",
    val estimatedKg: String = "0",
    val offerPricePerKg: String = "",
    val lines: List<CostoLine> = listOf(CostoLine(category = "Insumos", item = "", qty = 1.0, unit = 0.0)),
) {
    val totalAmount: Double
        get() = lines.sumOf { it.subtotal }

    val kgValue: Double
        get() = estimatedKg.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0

    val offerValue: Double?
        get() = offerPricePerKg.toDoubleOrNull()?.takeIf { it >= 0.0 }

    val costPerKg: Double
        get() = if (kgValue > 0.0) totalAmount / kgValue else 0.0

    val estimatedProfit: Double?
        get() = offerValue?.let { (it * kgValue) - totalAmount }
}

class CafeViewModel(
    private val repository: CafeRepository,
    private val networkService: TerritoryNetworkService,
    private val aiRepository: CoffeeAiRepository,
    private val currentUserId: String,
    private val sessionState: StateFlow<UserSessionUiState>? = null,
    private val produccionRepository: FincaProduccionRepository = repository.produccionRepository,
) : ViewModel() {

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private var migrationStarted = false

    val roleDefinitionsState: StateFlow<UiState<List<RoleDefinition>>> =
        repository.observeRoleDefinitions().toUiState("roles")

    val userProfileState: StateFlow<UiState<UserProfile?>> =
        (sessionState?.map { state ->
            when (state) {
                is UserSessionUiState.Authenticated -> UiState.Success(state.profile)
                is UserSessionUiState.NeedsOnboarding -> UiState.Success(null)
                is UserSessionUiState.Error -> UiState.Error(state.message)
                is UserSessionUiState.Loading -> UiState.Loading
                UserSessionUiState.Unauthenticated -> UiState.Success(null)
            }
        } ?: repository.observeUserProfile(currentUserId)
            .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w("CafeVM-user_profile", "Error observing user_profile", rootError)
                emit(UiState.Error(rootError.localizedMessage ?: "Error al cargar tu perfil"))
            })
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiState.Loading,
            )

    private val verifiedRoleFlow: Flow<AppRole> =
        (sessionState?.map { (it as? UserSessionUiState.Authenticated)?.appRole }
            ?: userProfileState.map { (it as? UiState.Success)?.data?.resolvedAppRole })
            .filterNotNull()
            .distinctUntilChanged()

    @OptIn(ExperimentalCoroutinesApi::class)
    val fincasState: StateFlow<UiState<List<FincaProfile>>> =
        verifiedRoleFlow
            .flatMapLatest { role -> produccionRepository.observeFincasPorRol(currentUserId, role) }
            .toUiState("fincas")

    @OptIn(ExperimentalCoroutinesApi::class)
    val cosechasState: StateFlow<UiState<List<CosechaRecord>>> =
        verifiedRoleFlow
            .flatMapLatest { role -> produccionRepository.observeCosechasPorRol(currentUserId, role) }
            .toUiState("cosechas")

    @OptIn(ExperimentalCoroutinesApi::class)
    val cultivosState: StateFlow<UiState<List<CultivoActivity>>> =
        verifiedRoleFlow
            .flatMapLatest { role -> produccionRepository.observeBitacoraPorRol(currentUserId, role) }
            .toUiState("cultivos")

    val costosState: StateFlow<UiState<List<CostoCalculation>>> =
        repository.observeCostos(currentUserId).toUiState("costos")

    val visitasState: StateFlow<UiState<List<VisitaAgronomica>>> =
        repository.observeVisitas(currentUserId).toUiState("visitas")

    val inquietudesState: StateFlow<UiState<List<InquietudTecnica>>> =
        repository.observeInquietudes(currentUserId).toUiState("inquietudes")

    val recolectoresState: StateFlow<UiState<List<RecolectorPostulacion>>> =
        repository.observeRecolectores(currentUserId).toUiState("recolectores")

    val proveedoresState: StateFlow<UiState<List<ProveedorItem>>> =
        repository.observeProveedores(currentUserId).toUiState("proveedores")

    val compradoresState: StateFlow<UiState<List<CompradorItem>>> =
        repository.observeCompradores(currentUserId).toUiState("compradores")

    val preciosCafeState: StateFlow<UiState<List<PrecioCafeRecord>>> =
        repository.observePreciosCafe(currentUserId).toUiState("precios_cafe")

    private val _calculatorDraft = MutableStateFlow(CalculatorDraft())
    val calculatorDraft: StateFlow<CalculatorDraft> = _calculatorDraft.asStateFlow()

    private val _liveMarketState = MutableStateFlow(LiveMarketSnapshot())
    val liveMarketState: StateFlow<LiveMarketSnapshot> = _liveMarketState.asStateFlow()

    private val _isLoadingMarket = MutableStateFlow(false)
    val isLoadingMarket: StateFlow<Boolean> = _isLoadingMarket.asStateFlow()

    private val _aiTitle = MutableStateFlow("Asesor Cafetero IA")
    val aiTitle: StateFlow<String> = _aiTitle.asStateFlow()

    private val _aiState = MutableStateFlow<AiUiState>(AiUiState.Idle)
    val aiState: StateFlow<AiUiState> = _aiState.asStateFlow()
    private var aiJob: Job? = null

    private val _mapFocusTarget = MutableStateFlow<Pair<Double, Double>?>(null)
    val mapFocusTarget: StateFlow<Pair<Double, Double>?> = _mapFocusTarget.asStateFlow()

    private val _agronomyTargetFarmCode = MutableStateFlow<String?>(null)
    val agronomyTargetFarmCode: StateFlow<String?> = _agronomyTargetFarmCode.asStateFlow()

    fun prepareAgronomyVisitForFarm(farmCodeOrId: String) {
        _agronomyTargetFarmCode.value = farmCodeOrId
    }

    fun consumeAgronomyVisitTarget() {
        _agronomyTargetFarmCode.value = null
    }

    fun focusMapAt(lat: Double, lon: Double) {
        _mapFocusTarget.value = lat to lon
    }

    fun clearMapFocusTarget() {
        _mapFocusTarget.value = null
    }

    fun triggerLegacyMigration(ownerDisplayName: String, role: AppRole) {
        if (migrationStarted) return
        migrationStarted = true
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.migrateLegacyUserRecordsIfNeeded(ownerDisplayName, role)
            }
        }
    }

    private fun <T> Flow<List<T>>.toUiState(tagSuffix: String): StateFlow<UiState<List<T>>> =
        map<List<T>, UiState<List<T>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w("CafeVM-$tagSuffix", "Error observing $tagSuffix", rootError)
                emit(UiState.Error(rootError.localizedMessage ?: "Error al cargar $tagSuffix"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiState.Loading,
            )

    fun notify(message: String) {
        _messages.tryEmit(message)
    }

    private fun activeUserDisplayName(): String {
        val authenticated = sessionState?.value as? UserSessionUiState.Authenticated
        if (authenticated != null) return authenticated.displayName
        val loaded = (userProfileState.value as? UiState.Success)?.data
        return loaded?.fullName?.takeIf { it.isNotBlank() } ?: "Productor local"
    }

    fun activeAppRole(): AppRole {
        val authenticated = sessionState?.value as? UserSessionUiState.Authenticated
        if (authenticated != null) return authenticated.appRole
        val loaded = (userProfileState.value as? UiState.Success)?.data
        return loaded?.resolvedAppRole ?: AppRole.CAFICULTOR
    }

    private fun activeUserRole(): String = activeAppRole().displayName

    private fun requireCapability(
        allowed: Boolean,
        requiredRoleLabel: String,
    ): Boolean {
        if (allowed) return true
        notify("Esta acción es exclusiva del rol $requiredRoleLabel.")
        return false
    }

    // --- CALCULATOR ACTIONS ---

    fun updateDraftMeta(
        name: String? = null,
        farmCode: String? = null,
        farmName: String? = null,
        estimatedKg: String? = null,
        offerPricePerKg: String? = null,
    ) {
        val current = _calculatorDraft.value
        _calculatorDraft.value = current.copy(
            name = name ?: current.name,
            farmCode = farmCode ?: current.farmCode,
            farmName = farmName ?: current.farmName,
            estimatedKg = estimatedKg ?: current.estimatedKg,
            offerPricePerKg = offerPricePerKg ?: current.offerPricePerKg,
        )
    }

    fun addCalcLine() {
        val current = _calculatorDraft.value
        if (current.lines.size >= 30) {
            notify("Máximo 30 partidas por cálculo.")
            return
        }
        _calculatorDraft.value = current.copy(
            lines = current.lines + CostoLine(category = "Otros", item = "", qty = 1.0, unit = 0.0)
        )
    }

    fun updateCalcLine(index: Int, line: CostoLine) {
        val current = _calculatorDraft.value
        if (index !in current.lines.indices) return
        val mutable = current.lines.toMutableList()
        mutable[index] = line
        _calculatorDraft.value = current.copy(lines = mutable)
    }

    fun removeCalcLine(index: Int) {
        val current = _calculatorDraft.value
        if (current.lines.size <= 1) {
            notify("Debe quedar al menos una partida de gasto.")
            return
        }
        val mutable = current.lines.toMutableList()
        mutable.removeAt(index)
        _calculatorDraft.value = current.copy(lines = mutable)
    }

    fun loadCalculationIntoEditor(calc: CostoCalculation) {
        _calculatorDraft.value = CalculatorDraft(
            editingId = calc.id,
            recordCode = calc.recordCode,
            name = calc.name,
            farmCode = calc.farmCode,
            farmName = calc.farmName,
            estimatedKg = if (calc.estimatedKg > 0.0) calc.estimatedKg.toString() else "0",
            offerPricePerKg = if (calc.offerPricePerKg > 0.0) calc.offerPricePerKg.toInt().toString() else "",
            lines = calc.lines.ifEmpty { listOf(CostoLine()) },
        )
    }

    fun resetCalculator() {
        _calculatorDraft.value = CalculatorDraft()
    }

    fun saveCalculator(onSaved: () -> Unit) {
        if (!requireCapability(activeAppRole().capabilities.calculateOwnCosts, AppRole.CAFICULTOR.displayName)) return
        val draft = _calculatorDraft.value
        if (draft.name.isBlank()) {
            notify("Escribe un nombre para el cálculo.")
            return
        }
        val validLines = draft.lines.filter { it.item.isNotBlank() }
        if (validLines.isEmpty()) {
            notify("Añade al menos una partida con concepto.")
            return
        }
        val detailsSummary = validLines.joinToString(" | ") {
            "${it.category}: ${it.item} (${formatNumber(it.qty)} × ${formatCop(it.unit)})"
        }.take(1000)

        val entity = CostoCalculation(
            id = draft.editingId,
            userId = currentUserId,
            recordCode = draft.recordCode.ifBlank { generateRecordCode("COS") },
            name = draft.name.trim(),
            farmCode = draft.farmCode.trim(),
            farmName = draft.farmName.trim(),
            date = todayIsoDate(),
            estimatedKg = draft.kgValue,
            offerPricePerKg = draft.offerValue ?: 0.0,
            totalAmount = validLines.sumOf { it.subtotal },
            lines = validLines,
            details = detailsSummary,
        )
        runWrite(
            successMsg = if (draft.editingId.isBlank()) "Cálculo guardado en tu historial." else "Cálculo actualizado.",
            onSuccess = {
                resetCalculator()
                onSaved()
            }
        ) {
            repository.saveCosto(entity)
        }
    }

    // --- CRUD WRITES ---

    fun saveUserProfile(
        profile: UserProfile,
        onError: () -> Unit = {},
        onDone: () -> Unit = {},
    ) {
        if (profile.fullName.trim().length < 2) {
            notify("Ingresa el nombre de la persona (mínimo 2 caracteres).")
            onError()
            return
        }
        if (profile.phone.trim().length < 5) {
            notify("Ingresa un número de contacto celular válido.")
            onError()
            return
        }
        if (profile.location.trim().length < 2) {
            notify("Ingresa tu lugar (vereda o municipio).")
            onError()
            return
        }
        runWrite(
            successMsg = "Datos personales guardados.",
            onError = onError,
            onSuccess = onDone,
        ) { repository.saveUserProfile(profile) }
    }

    fun saveFinca(item: FincaProfile, onDone: () -> Unit = {}) {
        val role = activeAppRole()
        if (!requireCapability(role.capabilities.manageOwnFarms, AppRole.CAFICULTOR.displayName)) return
        val clean = item.copy(
            ownerName = item.ownerName.ifBlank { activeUserDisplayName() },
            recordCode = item.recordCode.ifBlank {
                generateRecordCode(if (item.recordType == "caficultor") "CAF" else "FIN")
            },
        )
        runWrite(
            successMsg = if (item.id.isBlank()) "Perfil publicado en tu finca y comunidad." else "Perfil actualizado.",
            onSuccess = onDone
        ) { produccionRepository.saveFinca(clean, role) }
    }

    fun deleteFinca(id: String) {
        val role = activeAppRole()
        if (!requireCapability(role.capabilities.manageOwnFarms, AppRole.CAFICULTOR.displayName)) return
        val ownerUid = (fincasState.value as? UiState.Success)?.data
            ?.firstOrNull { it.id == id }?.userId
        runWrite("Registro eliminado.") { produccionRepository.deleteFinca(id, role, ownerUid) }
    }

    fun saveCosecha(item: CosechaRecord, onDone: () -> Unit = {}) {
        val role = activeAppRole()
        if (!requireCapability(role.capabilities.recordOwnHarvests, AppRole.CAFICULTOR.displayName)) return
        if (item.quantityKg <= 0.0) {
            notify("Ingresa un kilaje mayor que cero.")
            return
        }
        val clean = item.copy(recordCode = item.recordCode.ifBlank { generateRecordCode("CSE") })
        runWrite(
            successMsg = if (item.id.isBlank()) "Cosecha registrada." else "Cosecha actualizada.",
            onSuccess = onDone
        ) { produccionRepository.saveCosecha(clean, role) }
    }

    fun deleteCosecha(id: String) {
        val role = activeAppRole()
        if (!requireCapability(role.capabilities.recordOwnHarvests, AppRole.CAFICULTOR.displayName)) return
        runWrite("Cosecha eliminada.") { produccionRepository.deleteCosecha(id, role) }
    }

    fun saveCultivo(item: CultivoActivity, onDone: () -> Unit = {}) {
        val role = activeAppRole()
        if (!requireCapability(role.capabilities.logOwnCropActivities, AppRole.CAFICULTOR.displayName)) return
        val ownedFarms = (fincasState.value as? UiState.Success)?.data.orEmpty()
            .filter { it.userId == currentUserId }
        val resolvedFarm = ownedFarms.firstOrNull {
            (item.farmId.isNotBlank() && it.id == item.farmId) ||
                (item.farmCode.isNotBlank() && it.recordCode == item.farmCode)
        }
        val clean = item.copy(
            authorName = item.authorName.ifBlank { activeUserDisplayName() },
            farmId = resolvedFarm?.id ?: item.farmId,
            farmName = resolvedFarm?.name ?: item.farmName,
            recordCode = item.recordCode.ifBlank { generateRecordCode("CUL") },
        )
        runWrite(
            successMsg = if (item.id.isBlank()) "Actividad guardada en la bitácora." else "Actividad actualizada.",
            onSuccess = onDone
        ) { produccionRepository.saveCultivo(clean, role) }
    }

    fun deleteCultivo(id: String) {
        val role = activeAppRole()
        if (!requireCapability(role.capabilities.logOwnCropActivities, AppRole.CAFICULTOR.displayName)) return
        val ownerUid = (cultivosState.value as? UiState.Success)?.data
            ?.firstOrNull { it.id == id }?.userId
        runWrite("Actividad eliminada.") { produccionRepository.deleteCultivo(id, role, ownerUid) }
    }

    fun deleteCosto(id: String) {
        if (!requireCapability(activeAppRole().capabilities.calculateOwnCosts, AppRole.CAFICULTOR.displayName)) return
        runWrite("Cálculo eliminado.") { repository.deleteCosto(id) }
    }

    fun saveVisita(item: VisitaAgronomica, onDone: () -> Unit = {}) {
        if (!requireCapability(activeAppRole().capabilities.registerAgronomyVisits, AppRole.AGRONOMO.displayName)) return
        val allFarms = (fincasState.value as? UiState.Success)?.data.orEmpty()
        val resolvedFarm = allFarms.firstOrNull {
            (item.farmId.isNotBlank() && it.id == item.farmId) ||
                (item.farmCode.isNotBlank() && it.recordCode == item.farmCode)
        }
        val clean = item.copy(
            engineerName = item.engineerName.ifBlank { activeUserDisplayName() },
            farmId = resolvedFarm?.id ?: item.farmId,
            farmName = resolvedFarm?.name ?: item.farmName,
            farmOwnerId = resolvedFarm?.userId ?: item.farmOwnerId,
            farmOwnerName = resolvedFarm?.ownerName?.ifBlank { resolvedFarm.name } ?: item.farmOwnerName,
            location = item.location.ifBlank { resolvedFarm?.location.orEmpty() },
            recordCode = item.recordCode.ifBlank { generateRecordCode("AGR") },
        )
        runWrite(
            successMsg = if (item.id.isBlank()) "Visita agronómica registrada y visible para la finca." else "Visita actualizada.",
            onSuccess = onDone
        ) { repository.saveVisita(clean) }
    }

    fun deleteVisita(id: String) {
        if (!requireCapability(activeAppRole().capabilities.registerAgronomyVisits, AppRole.AGRONOMO.displayName)) return
        runWrite("Visita eliminada.") { repository.deleteVisita(id) }
    }

    fun saveInquietud(item: InquietudTecnica, onDone: () -> Unit = {}) {
        if (!requireCapability(activeAppRole().capabilities.createInquiries, AppRole.CAFICULTOR.displayName)) return
        if (item.subject.trim().length < 2 || item.question.trim().length < 3) {
            notify("Escribe un asunto y el detalle de tu inquietud.")
            return
        }
        val allFarms = (fincasState.value as? UiState.Success)?.data.orEmpty()
        val resolvedFarm = allFarms.firstOrNull {
            (item.farmId.isNotBlank() && it.id == item.farmId) ||
                (item.farmCode.isNotBlank() && it.recordCode == item.farmCode)
        }
        val clean = item.copy(
            authorName = item.authorName.ifBlank { activeUserDisplayName() },
            authorRole = item.authorRole.ifBlank { activeUserRole() },
            farmId = resolvedFarm?.id ?: item.farmId,
            farmName = resolvedFarm?.name ?: item.farmName,
            date = item.date.ifBlank { todayIsoDate() },
            recordCode = item.recordCode.ifBlank { generateRecordCode("INQ") },
        )
        runWrite(
            successMsg = if (item.id.isBlank()) "Inquietud enviada a ingenieros agrónomos." else "Inquietud actualizada.",
            onSuccess = onDone
        ) { repository.saveInquietud(clean) }
    }

    fun replyInquietud(inquietudId: String, replyText: String, onDone: () -> Unit = {}) {
        if (!requireCapability(activeAppRole().capabilities.replyInquiries, AppRole.AGRONOMO.displayName)) return
        if (replyText.trim().length < 2) {
            notify("Escribe una respuesta clara (mínimo 2 caracteres).")
            return
        }
        runWrite(
            successMsg = "Respuesta publicada correctamente.",
            onSuccess = onDone
        ) {
            repository.replyToInquietud(
                inquietudId = inquietudId,
                replyText = replyText.trim(),
                replyByName = activeUserDisplayName(),
                replyDate = todayIsoDate(),
            )
        }
    }

    fun deleteInquietud(id: String) {
        if (!requireCapability(activeAppRole().capabilities.createInquiries, AppRole.CAFICULTOR.displayName)) return
        runWrite("Inquietud eliminada.") { repository.deleteInquietud(id) }
    }

    fun saveRecolector(item: RecolectorPostulacion, onDone: () -> Unit = {}) {
        if (!requireCapability(activeAppRole().capabilities.manageCollectors, AppRole.CAFICULTOR.displayName)) return
        val clean = item.copy(recordCode = item.recordCode.ifBlank { generateRecordCode("REC") })
        runWrite(
            successMsg = "Inscripción guardada de forma privada.",
            onSuccess = onDone
        ) { repository.saveRecolector(clean) }
    }

    fun deleteRecolector(id: String) {
        if (!requireCapability(activeAppRole().capabilities.manageCollectors, AppRole.CAFICULTOR.displayName)) return
        runWrite("Postulación eliminada.") { repository.deleteRecolector(id) }
    }

    fun saveProveedor(item: ProveedorItem, onDone: () -> Unit = {}) {
        if (!requireCapability(activeAppRole().capabilities.publishProviderCatalog, AppRole.PROVEEDOR.displayName)) return
        val clean = item.copy(
            ownerName = item.ownerName.ifBlank { activeUserDisplayName() },
            recordCode = item.recordCode.ifBlank { generateRecordCode("PRO") },
        )
        runWrite(
            successMsg = if (item.id.isBlank()) "Producto o servicio publicado." else "Producto actualizado.",
            onSuccess = onDone
        ) { repository.saveProveedor(clean) }
    }

    fun deleteProveedor(id: String) {
        if (!requireCapability(activeAppRole().capabilities.publishProviderCatalog, AppRole.PROVEEDOR.displayName)) return
        runWrite("Producto eliminado.") { repository.deleteProveedor(id) }
    }

    fun saveComprador(item: CompradorItem, onDone: () -> Unit = {}) {
        if (!requireCapability(activeAppRole().capabilities.publishBuyerPrices, AppRole.COMPRADOR.displayName)) return
        val clean = item.copy(
            priceDate = item.priceDate.ifBlank { todayIsoDate() },
            recordCode = item.recordCode.ifBlank {
                generateRecordCode(if (item.actorType == "vendedor") "VEN" else "COM")
            },
        )
        runWrite(
            successMsg = if (item.id.isBlank()) "Precio de compra publicado." else "Precio de compra actualizado.",
            onSuccess = onDone
        ) { repository.saveComprador(clean) }
    }

    fun deleteComprador(id: String) {
        if (!requireCapability(activeAppRole().capabilities.publishBuyerPrices, AppRole.COMPRADOR.displayName)) return
        runWrite("Registro eliminado.") { repository.deleteComprador(id) }
    }

    fun savePrecioCafe(item: PrecioCafeRecord, onDone: () -> Unit = {}) {
        if (!requireCapability(activeAppRole().capabilities.recordCoffeePrices, "Caficultor o Comprador")) return
        if (item.pricePerCargaCop <= 0.0 || item.pricePerKgCop <= 0.0) {
            notify("Ingresa un precio válido mayor que cero.")
            return
        }
        val clean = item.copy(
            date = item.date.ifBlank { todayIsoDate() },
            source = item.source.ifBlank { "FNC" },
            recordCode = item.recordCode.ifBlank { generateRecordCode("PRC") },
        )
        runWrite(
            successMsg = if (item.id.isBlank()) "Precio registrado en tu historial." else "Registro de precio actualizado.",
            onSuccess = onDone
        ) { repository.savePrecioCafe(clean) }
    }

    fun deletePrecioCafe(id: String) {
        if (!requireCapability(activeAppRole().capabilities.recordCoffeePrices, "Caficultor o Comprador")) return
        runWrite("Registro de precio eliminado.") { repository.deletePrecioCafe(id) }
    }

    private fun runWrite(
        successMsg: String,
        onError: () -> Unit = {},
        onSuccess: () -> Unit = {},
        block: suspend () -> Result<Unit>,
    ) {
        if (_isSaving.value) {
            onError()
            return
        }
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val res = block()
                res.onSuccess {
                    notify(successMsg)
                    onSuccess()
                }.onFailure { err ->
                    notify(err.localizedMessage ?: "No se pudo guardar en Firestore.")
                    onError()
                }
            } finally {
                _isSaving.value = false
            }
        }
    }

    // --- NETWORK & GEOCODING ---

    suspend fun geocodeAddress(address: String) = networkService.geocodeInNarino(address)

    fun refreshLiveMarket() {
        if (_isLoadingMarket.value) return
        viewModelScope.launch {
            _isLoadingMarket.value = true
            try {
                _liveMarketState.value = networkService.fetchLiveMarketSnapshot(nowColombiaTimestampLabel())
            } finally {
                _isLoadingMarket.value = false
            }
        }
    }

    // --- GEMINI AI ADVISORY (FIREBASE AI LOGIC) ---

    fun analyzeCostsAndMarket() {
        val draft = _calculatorDraft.value
        val market = _liveMarketState.value
        val latestSavedPrice = (preciosCafeState.value as? UiState.Success)?.data?.firstOrNull()
        val buyers = (compradoresState.value as? UiState.Success)?.data.orEmpty()
            .filter { it.offerAmountCop > 0.0 }
            .take(5)
        val buyerLines = if (buyers.isEmpty()) {
            "Sin ofertas de compradores registradas."
        } else {
            buyers.joinToString("\n") {
                "- ${it.name} (${it.location.ifBlank { "Albán" }}): ${formatCop(it.offerAmountCop)} por ${it.priceUnit}"
            }
        }
        val marketSummary = buildString {
            append("- TRM Dólar USD/COP: ")
            append(market.trmCop?.let { formatCop(it) } ?: "No consultada aún")
            append("\n- Bolsa Nueva York Café C (KC=F): ")
            append(market.nyCoffeeCentsLb?.let { "${formatNumber(it, 2)} ¢/lb" } ?: "No consultada aún")
            if (latestSavedPrice != null) {
                append("\n- Último precio registrado en historial (${latestSavedPrice.date}, ${latestSavedPrice.source}): ")
                append("${formatCop(latestSavedPrice.pricePerCargaCop)}/carga (${formatCop(latestSavedPrice.pricePerKgCop)}/kg)")
            }
        }
        val prompt = """
            Eres un asesor cafetero práctico para productores en San José de Albán, Nariño, Colombia.
            Responde en español claro, breve y estructurado en 3 puntos concretos (máximo 180 palabras).
            Indicadores de mercado:
            $marketSummary
            Datos del cálculo actual:
            - Nombre: ${draft.name.ifBlank { "Cálculo en curso" }}
            - Costo total estimado: ${formatCop(draft.totalAmount)}
            - Producción estimada: ${formatNumber(draft.kgValue)} kg
            - Costo por kg: ${formatCop(draft.costPerKg)}
            - Oferta ingresada: ${draft.offerValue?.let { formatCop(it) } ?: "Ninguna"}
            Ofertas registradas en la plataforma:
            $buyerLines

            Indica:
            1) Punto de equilibrio por kilogramo y carga (125 kg) frente al precio actual y futuros NY/TRM.
            2) Qué revisar antes de aceptar una oferta (factor de rendimiento, humedad, transporte y descuentos).
            3) Una recomendación práctica para optimizar el costo de producción sin sacrificar calidad en taza.
        """.trimIndent()
        launchAi("Análisis de Costos y Mercado", prompt)
    }

    fun analyzeAgronomyAdvice(question: String, farmContext: String) {
        val prompt = """
            Eres un ingeniero agrónomo especialista en café de altura en San José de Albán, Nariño, Colombia (1.600 - 2.100 msnm).
            Responde en español sencillo, responsable y práctico (máximo 180 palabras).
            Contexto de finca/registro: $farmContext
            Consulta del caficultor: $question

            Da pasos claros de manejo integrado, buenas prácticas y recuerda verificar presencialmente en campo con un agrónomo antes de aplicar agroquímicos.
        """.trimIndent()
        launchAi("Asesoría Agronómica IA", prompt)
    }

    fun dismissAi() {
        aiJob?.cancel()
        _aiState.value = AiUiState.Idle
    }

    private fun launchAi(title: String, prompt: String) {
        aiJob?.cancel()
        _aiTitle.value = title
        aiJob = viewModelScope.launch {
            aiRepository.streamAdvisory(prompt).collect { state ->
                _aiState.value = state
            }
        }
    }
}
