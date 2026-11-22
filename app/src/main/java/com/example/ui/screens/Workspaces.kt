package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.LiveMarketSnapshot
import com.example.data.NominatimResult
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
import com.example.model.UiState
import com.example.model.UserProfile
import com.example.model.VisitaAgronomica
import com.example.viewmodel.CalculatorDraft

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductionWorkspaceScreen(
    initialTab: Int,
    currentUserId: String = "",
    appRole: AppRole = AppRole.CAFICULTOR,
    userProfile: UserProfile? = null,
    fincasState: UiState<List<FincaProfile>>,
    cosechasState: UiState<List<CosechaRecord>>,
    cultivosState: UiState<List<CultivoActivity>>,
    visitasState: UiState<List<VisitaAgronomica>>,
    inquietudesState: UiState<List<InquietudTecnica>> = UiState.Success(emptyList()),
    isSaving: Boolean,
    onSaveFinca: (FincaProfile, () -> Unit) -> Unit,
    onDeleteFinca: (String) -> Unit,
    onOpenProfile: (String) -> Unit,
    onOpenFincaInMap: (Double, Double) -> Unit,
    onRegisterVisitForFarm: (String) -> Unit = {},
    onSaveCosecha: (CosechaRecord, () -> Unit) -> Unit,
    onDeleteCosecha: (String) -> Unit,
    onSaveCultivo: (CultivoActivity, () -> Unit) -> Unit,
    onDeleteCultivo: (String) -> Unit,
    onSaveVisita: (VisitaAgronomica, () -> Unit) -> Unit,
    onDeleteVisita: (String) -> Unit,
    onSaveInquietud: (InquietudTecnica, () -> Unit) -> Unit = { _, _ -> },
    onReplyInquietud: (String, String, () -> Unit) -> Unit = { _, _, _ -> },
    onDeleteInquietud: (String) -> Unit = {},
    onGeocode: suspend (String) -> Result<NominatimResult?>,
    onAskAgronomyAi: (question: String, farmContext: String) -> Unit,
    onNotify: (String) -> Unit,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(initialTab.coerceIn(0, 3)) }
    LaunchedEffect(initialTab) {
        selectedTab = initialTab.coerceIn(0, 3)
    }

    val tabs = listOf("Fincas", "Cosechas", "Bitácora", "Agronomía")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        PrimaryTabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outline) },
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                        )
                    },
                    modifier = Modifier.testTag("prod_tab_$index"),
                )
            }
        }

        when (selectedTab) {
            0 -> FincasScreen(
                currentUserId = currentUserId,
                appRole = appRole,
                fincasState = fincasState,
                isSaving = isSaving,
                onSave = onSaveFinca,
                onDelete = onDeleteFinca,
                onOpenProfile = onOpenProfile,
                onOpenInMap = onOpenFincaInMap,
                onRegisterVisitForFarm = onRegisterVisitForFarm,
                onGeocode = onGeocode,
                onNotify = onNotify,
            )
            1 -> HarvestScreen(
                currentUserId = currentUserId,
                appRole = appRole,
                cosechasState = cosechasState,
                fincasState = fincasState,
                isSaving = isSaving,
                onSave = onSaveCosecha,
                onDelete = onDeleteCosecha,
            )
            2 -> CropLogScreen(
                currentUserId = currentUserId,
                appRole = appRole,
                cultivosState = cultivosState,
                fincasState = fincasState,
                isSaving = isSaving,
                onSave = onSaveCultivo,
                onDelete = onDeleteCultivo,
            )
            3 -> AgronomyScreen(
                currentUserId = currentUserId,
                appRole = appRole,
                userProfile = userProfile,
                visitasState = visitasState,
                inquietudesState = inquietudesState,
                fincasState = fincasState,
                isSaving = isSaving,
                onSave = onSaveVisita,
                onDelete = onDeleteVisita,
                onSaveInquietud = onSaveInquietud,
                onReplyInquietud = onReplyInquietud,
                onDeleteInquietud = onDeleteInquietud,
                onAskAgronomyAi = onAskAgronomyAi,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceWorkspaceScreen(
    initialTab: Int,
    currentUserId: String,
    appRole: AppRole = AppRole.CAFICULTOR,
    compradoresState: UiState<List<CompradorItem>>,
    preciosCafeState: UiState<List<PrecioCafeRecord>>,
    fincasState: UiState<List<FincaProfile>>,
    costosState: UiState<List<CostoCalculation>>,
    draft: CalculatorDraft,
    liveMarket: LiveMarketSnapshot,
    isLoadingMarket: Boolean,
    isSaving: Boolean,
    onRefreshLiveMarket: () -> Unit,
    onSavePrecioCafe: (PrecioCafeRecord, () -> Unit) -> Unit,
    onDeletePrecioCafe: (String) -> Unit,
    onSaveComprador: (CompradorItem, () -> Unit) -> Unit,
    onDeleteComprador: (String) -> Unit,
    onGeocode: suspend (String) -> Result<NominatimResult?>,
    onUpdateDraftMeta: (String?, String?, String?, String?, String?) -> Unit,
    onAddCalcLine: () -> Unit,
    onUpdateCalcLine: (Int, CostoLine) -> Unit,
    onRemoveCalcLine: (Int) -> Unit,
    onSaveCalculator: (() -> Unit) -> Unit,
    onResetCalculator: () -> Unit,
    onReopenInCalculator: (CostoCalculation) -> Unit,
    onDeleteCosto: (String) -> Unit,
    onAskMarketAi: () -> Unit,
    onNotify: (String) -> Unit,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(initialTab.coerceIn(0, 2)) }
    LaunchedEffect(initialTab) {
        selectedTab = initialTab.coerceIn(0, 2)
    }

    val tabs = listOf("Precios", "Calculadora", "Historial")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        PrimaryTabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outline) },
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                        )
                    },
                    modifier = Modifier.testTag("finance_tab_$index"),
                )
            }
        }

        when (selectedTab) {
            0 -> PricesScreen(
                currentUserId = currentUserId,
                appRole = appRole,
                compradoresState = compradoresState,
                preciosCafeState = preciosCafeState,
                liveMarket = liveMarket,
                isLoadingMarket = isLoadingMarket,
                isSaving = isSaving,
                onRefreshLiveMarket = onRefreshLiveMarket,
                onSavePrecioCafe = onSavePrecioCafe,
                onDeletePrecioCafe = onDeletePrecioCafe,
                onSaveComprador = onSaveComprador,
                onDeleteComprador = onDeleteComprador,
                onGeocode = onGeocode,
                onAskMarketAi = onAskMarketAi,
                onNotify = onNotify,
            )
            1 -> CostsCalculatorScreen(
                currentUserId = currentUserId,
                appRole = appRole,
                draft = draft,
                fincasState = fincasState,
                isSaving = isSaving,
                onUpdateMeta = onUpdateDraftMeta,
                onAddLine = onAddCalcLine,
                onUpdateLine = onUpdateCalcLine,
                onRemoveLine = onRemoveCalcLine,
                onSave = { onSaveCalculator { selectedTab = 2 } },
                onReset = onResetCalculator,
                onOpenHistory = { selectedTab = 2 },
                onAskCostsAi = onAskMarketAi,
            )
            2 -> CostsHistoryScreen(
                appRole = appRole,
                costosState = costosState,
                onReopenInCalculator = { calc ->
                    onReopenInCalculator(calc)
                    selectedTab = 1
                },
                onDelete = onDeleteCosto,
                onNewCalculation = { selectedTab = 1 },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityWorkspaceScreen(
    initialTab: Int,
    currentUserId: String,
    appRole: AppRole = AppRole.CAFICULTOR,
    fincasState: UiState<List<FincaProfile>>,
    compradoresState: UiState<List<CompradorItem>>,
    proveedoresState: UiState<List<ProveedorItem>>,
    recolectoresState: UiState<List<RecolectorPostulacion>>,
    isSaving: Boolean,
    focusCoordinate: Pair<Double, Double>? = null,
    onConsumeFocusCoordinate: () -> Unit = {},
    onSaveFinca: (FincaProfile, () -> Unit) -> Unit,
    onSaveComprador: (CompradorItem, () -> Unit) -> Unit,
    onDeleteComprador: (String) -> Unit,
    onSaveProveedor: (ProveedorItem, () -> Unit) -> Unit,
    onDeleteProveedor: (String) -> Unit,
    onSaveRecolector: (RecolectorPostulacion, () -> Unit) -> Unit,
    onDeleteRecolector: (String) -> Unit,
    onOpenFincaProfile: (String) -> Unit,
    onGeocode: suspend (String) -> Result<NominatimResult?>,
    onGoHome: () -> Unit,
    onNotify: (String) -> Unit,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(initialTab.coerceIn(0, 4)) }
    LaunchedEffect(initialTab) {
        selectedTab = initialTab.coerceIn(0, 4)
    }

    val tabs = listOf("Mapa", "Proveedores", "Recolectores", "Escuela", "Guía")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        PrimaryScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            edgePadding = 12.dp,
            divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outline) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                        )
                    },
                    modifier = Modifier.testTag("community_tab_$index"),
                )
            }
        }

        when (selectedTab) {
            0 -> MapScreen(
                currentUserId = currentUserId,
                appRole = appRole,
                fincasState = fincasState,
                compradoresState = compradoresState,
                proveedoresState = proveedoresState,
                isSaving = isSaving,
                focusCoordinate = focusCoordinate,
                onConsumeFocusCoordinate = onConsumeFocusCoordinate,
                onSaveFinca = onSaveFinca,
                onSaveComprador = onSaveComprador,
                onDeleteComprador = onDeleteComprador,
                onSaveProveedor = onSaveProveedor,
                onOpenFincaProfile = onOpenFincaProfile,
                onGeocode = onGeocode,
                onNotify = onNotify,
            )
            1 -> ProvidersScreen(
                currentUserId = currentUserId,
                appRole = appRole,
                proveedoresState = proveedoresState,
                isSaving = isSaving,
                onSave = onSaveProveedor,
                onDelete = onDeleteProveedor,
                onGeocode = onGeocode,
                onNotify = onNotify,
            )
            2 -> CollectorsScreen(
                appRole = appRole,
                recolectoresState = recolectoresState,
                isSaving = isSaving,
                onSave = onSaveRecolector,
                onDelete = onDeleteRecolector,
            )
            3 -> SchoolScreen()
            4 -> GuideScreen(onGoHome = onGoHome)
        }
    }
}
