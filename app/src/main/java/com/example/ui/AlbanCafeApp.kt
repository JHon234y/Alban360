package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Landscape
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.R
import com.example.data.CafeRepository
import com.example.data.CoffeeAiRepository
import com.example.data.TerritoryNetworkService
import com.example.model.AppRole
import com.example.model.UiState
import com.example.model.UserProfile
import com.example.ui.components.AiAdvisoryDialog
import com.example.ui.components.AsyncDrawableImage
import com.example.ui.screens.AgronomyScreen
import com.example.ui.screens.CollectorsScreen
import com.example.ui.screens.CostsCalculatorScreen
import com.example.ui.screens.CostsHistoryScreen
import com.example.ui.screens.CropLogScreen
import com.example.ui.screens.FincasScreen
import com.example.ui.screens.GuideScreen
import com.example.ui.screens.HarvestScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MapScreen
import com.example.ui.screens.PricesScreen
import com.example.ui.screens.ProfileDetailScreen
import com.example.ui.screens.ProvidersScreen
import com.example.ui.screens.RoleProfileScreen
import com.example.ui.screens.SchoolScreen
import com.example.ui.theme.DangerClay
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.ForestGreenDeep
import com.example.ui.theme.ForestGreenLight
import com.example.ui.theme.ForestGreenMid
import com.example.ui.theme.RoastedBrown
import com.example.ui.theme.SagePale
import com.example.ui.theme.WarmGold
import com.example.util.configureOsmDroidCache
import com.example.viewmodel.CafeViewModel
import com.example.viewmodel.UserSessionUiState
import com.example.viewmodel.UserSessionViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLDecoder
import java.net.URLEncoder

private data class SidebarModuleItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val matchPrefixes: List<String> = listOf(route),
)

private val primarySidebarModules = listOf(
    SidebarModuleItem("inicio", "Resumen", Icons.Default.Dashboard),
    SidebarModuleItem("guia", "Folleto de orientación", Icons.Default.Book),
    SidebarModuleItem("fincas", "Fincas y caficultores", Icons.Default.Landscape, listOf("fincas", "detalle/")),
    SidebarModuleItem("proveedores", "Proveedores", Icons.Default.Inventory2),
    SidebarModuleItem("mapa", "Mapa satelital", Icons.Default.Map),
    SidebarModuleItem("precios", "Precios del café", Icons.Default.TrendingUp),
    SidebarModuleItem("escuela", "Escuela Cafetera", Icons.Default.School),
    SidebarModuleItem("costos", "Costos de producción", Icons.Default.Calculate, listOf("costos", "historial")),
    SidebarModuleItem("cosecha", "Cosecha", Icons.Default.Scale),
    SidebarModuleItem("cultivo", "Bitácora de cultivo", Icons.Default.CalendarMonth),
    SidebarModuleItem("recolectores", "Recolectores", Icons.Default.Groups),
    SidebarModuleItem("agronomia", "Ingenieros agrónomos", Icons.Default.Eco),
)

private val roleSidebarProfiles = listOf(
    SidebarModuleItem("perfil-caficultor", "Caficultor", Icons.Default.Person),
    SidebarModuleItem("perfil-ingeniero", "Ingeniero agrónomo", Icons.Default.Eco),
    SidebarModuleItem("perfil-proveedor", "Proveedor", Icons.Default.Inventory2),
    SidebarModuleItem("perfil-comprador", "Comprador", Icons.Default.ShoppingBag),
)

private data class MobileBottomTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

private fun mobileBottomTabsForRole(appRole: AppRole): List<MobileBottomTab> = when (appRole) {
    AppRole.CAFICULTOR -> listOf(
        MobileBottomTab("inicio", "Inicio", Icons.Filled.Home, Icons.Outlined.Home),
        MobileBottomTab("fincas", "Fincas", Icons.Filled.Landscape, Icons.Outlined.Landscape),
        MobileBottomTab("cultivo", "Bitácora", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
        MobileBottomTab("precios", "Precios", Icons.Filled.TrendingUp, Icons.Outlined.TrendingUp),
    )
    AppRole.AGRONOMO -> listOf(
        MobileBottomTab("inicio", "Inicio", Icons.Filled.Home, Icons.Outlined.Home),
        MobileBottomTab("agronomia", "Agronomía", Icons.Filled.Eco, Icons.Outlined.Eco),
        MobileBottomTab("fincas", "Fincas", Icons.Filled.Landscape, Icons.Outlined.Landscape),
        MobileBottomTab("mapa", "Mapa", Icons.Filled.Map, Icons.Outlined.Map),
    )
    AppRole.PROVEEDOR -> listOf(
        MobileBottomTab("inicio", "Inicio", Icons.Filled.Home, Icons.Outlined.Home),
        MobileBottomTab("proveedores", "Productos", Icons.Filled.Inventory2, Icons.Outlined.Inventory2),
        MobileBottomTab("mapa", "Mapa", Icons.Filled.Map, Icons.Outlined.Map),
        MobileBottomTab("fincas", "Fincas", Icons.Filled.Landscape, Icons.Outlined.Landscape),
    )
    AppRole.COMPRADOR -> listOf(
        MobileBottomTab("inicio", "Inicio", Icons.Filled.Home, Icons.Outlined.Home),
        MobileBottomTab("precios", "Precios", Icons.Filled.TrendingUp, Icons.Outlined.TrendingUp),
        MobileBottomTab("fincas", "Fincas", Icons.Filled.Landscape, Icons.Outlined.Landscape),
        MobileBottomTab("mapa", "Mapa", Icons.Filled.Map, Icons.Outlined.Map),
    )
}

private fun topBarTitleForRoute(route: String?): Pair<String, String> = when {
    route == null || route == "inicio" -> "Resumen general" to "San José de Albán · Nariño"
    route == "guia" -> "Folleto de orientación" to "Guía rápida de Albán Café 360°"
    route == "fincas" -> "Fincas y caficultores" to "Propiedad privada + red comunitaria"
    route == "proveedores" -> "Proveedores y catálogo" to "Productos, insumos y servicios"
    route == "mapa" -> "Mapa satelital" to "Fincas, puntos de venta y proveedores"
    route == "precios" -> "Precios del café" to "Bolsa NY · TRM · FNC · Compradores"
    route == "escuela" -> "Escuela Cafetera" to "Cursos y agenda FNC"
    route == "costos" -> "Costos de producción" to "Calculadora por lote y kilogramo"
    route == "historial" -> "Historial de costos" to "Cálculos guardados en la nube"
    route == "cosecha" -> "Registro de cosecha" to "Control diario de pesajes"
    route == "cultivo" -> "Bitácora de cultivo" to "Mi finca + manejo en otras fincas"
    route == "recolectores" -> "Inscripción de recolectores" to "Talento en cosecha"
    route == "agronomia" -> "Panel agronómico" to "Visitas a fincas, inquietudes e IA"
    route == "perfil-caficultor" -> "Perfil caficultor" to "Del cultivo a una mejor decisión"
    route == "perfil-ingeniero" -> "Perfil ingeniero agrónomo" to "Visitas técnicas e inquietudes"
    route == "perfil-proveedor" -> "Perfil proveedor" to "Catálogo de insumos y servicios"
    route == "perfil-comprador" -> "Perfil comprador" to "Precios diarios y trazabilidad QR"
    route.startsWith("detalle/") -> "Perfil de finca" to "Ficha de origen y visitas técnicas"
    else -> "Albán Café 360°" to "San José de Albán · Nariño"
}

private fun normalizeRoute(target: String): String = when (target) {
    "produccion/0" -> "fincas"
    "produccion/1" -> "cosecha"
    "produccion/2" -> "cultivo"
    "produccion/3" -> "agronomia"
    "finanzas/0" -> "precios"
    "finanzas/1" -> "costos"
    "finanzas/2" -> "historial"
    "comunidad/0" -> "mapa"
    "comunidad/1" -> "proveedores"
    "comunidad/2" -> "recolectores"
    "comunidad/3" -> "escuela"
    "comunidad/4" -> "guia"
    else -> target
}

@Composable
fun AlbanCafeRoot() {
    val context = LocalContext.current
    var runtimeReady by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        withFrameNanos { }
        withContext(Dispatchers.IO) {
            val appCtx = context.applicationContext
            runCatching {
                FirebaseFirestore.getInstance(appCtx.getString(R.string.firestore_database_id))
            }
            runCatching { Firebase.auth }
            runCatching { configureOsmDroidCache(appCtx) }
        }
        runtimeReady = true
    }

    if (!runtimeReady) {
        SessionLoadingView(message = "Iniciando Albán Café 360°...")
    } else {
        AlbanCafeSessionHost()
    }
}

@Composable
private fun AlbanCafeSessionHost() {
    val context = LocalContext.current
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val repository = remember(context) { CafeRepository(context.applicationContext) }
    val sessionViewModel: UserSessionViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                UserSessionViewModel(repository = repository)
            }
        }
    )

    val sessionState by sessionViewModel.sessionState.collectAsStateWithLifecycle()
    val isSavingProfile by sessionViewModel.isSavingProfile.collectAsStateWithLifecycle()
    val rootSnackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(sessionViewModel, sessionState) {
        if (sessionState !is UserSessionUiState.Authenticated) {
            sessionViewModel.messages.collect { msg ->
                rootSnackbarHostState.showSnackbar(msg)
            }
        }
    }

    when (val state = sessionState) {
        is UserSessionUiState.Unauthenticated -> {
            AuthScreen(
                credentialManager = credentialManager,
                onPrepareRegistration = sessionViewModel::prepareRegistration,
                onAuthSuccess = {},
            )
        }
        is UserSessionUiState.Loading -> {
            SessionLoadingView(message = state.message)
        }
        is UserSessionUiState.NeedsOnboarding -> {
            Box(modifier = Modifier.fillMaxSize()) {
                ProfileOnboardingScreen(
                    googleUser = state.user,
                    isSaving = isSavingProfile,
                    onSaveProfile = { profile -> sessionViewModel.saveUserProfile(profile) },
                    onSignOut = { sessionViewModel.signOut(credentialManager) },
                )
                SnackbarHost(
                    hostState = rootSnackbarHostState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding(),
                )
            }
        }
        is UserSessionUiState.Error -> {
            SessionErrorView(
                message = state.message,
                onRetry = sessionViewModel::retryProfileObservation,
                onSignOut = { sessionViewModel.signOut(credentialManager) },
            )
        }
        is UserSessionUiState.Authenticated -> {
            key(state.user.uid) {
                AuthenticatedAppShell(
                    repository = repository,
                    sessionViewModel = sessionViewModel,
                    sessionState = state,
                    credentialManager = credentialManager,
                    isSavingProfile = isSavingProfile,
                )
            }
        }
    }
}

@Composable
private fun SessionLoadingView(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(24.dp),
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SessionErrorView(
    message: String,
    onRetry: () -> Unit,
    onSignOut: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .padding(20.dp),
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = Icons.Default.CloudOff,
                    contentDescription = null,
                    tint = DangerClay,
                    modifier = Modifier.size(40.dp),
                )
                Text(
                    text = "No pudimos verificar tu perfil y rol",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = onRetry,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("session_error_retry_button"),
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Reintentar conexión")
                }
                OutlinedButton(
                    onClick = onSignOut,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerClay),
                    border = BorderStroke(1.dp, DangerClay.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("session_error_signout_button"),
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Cerrar sesión de Google")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedAppShell(
    repository: CafeRepository,
    sessionViewModel: UserSessionViewModel,
    sessionState: UserSessionUiState.Authenticated,
    credentialManager: CredentialManager,
    isSavingProfile: Boolean,
) {
    val scope = rememberCoroutineScope()
    val user = sessionState.user
    val userProfile = sessionState.profile
    val activeAppRole = sessionState.appRole
    val activeDisplayName = sessionState.displayName

    val viewModel: CafeViewModel = viewModel(
        key = user.uid,
        factory = viewModelFactory {
            initializer {
                CafeViewModel(
                    repository = repository,
                    networkService = TerritoryNetworkService(),
                    aiRepository = CoffeeAiRepository(),
                    currentUserId = user.uid,
                    sessionState = sessionViewModel.sessionState,
                )
            }
        }
    )

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val canPopBack = navController.previousBackStackEntry != null

    val fincasState by viewModel.fincasState.collectAsStateWithLifecycle()
    val cosechasState by viewModel.cosechasState.collectAsStateWithLifecycle()
    val cultivosState by viewModel.cultivosState.collectAsStateWithLifecycle()
    val costosState by viewModel.costosState.collectAsStateWithLifecycle()
    val visitasState by viewModel.visitasState.collectAsStateWithLifecycle()
    val inquietudesState by viewModel.inquietudesState.collectAsStateWithLifecycle()
    val recolectoresState by viewModel.recolectoresState.collectAsStateWithLifecycle()
    val proveedoresState by viewModel.proveedoresState.collectAsStateWithLifecycle()
    val compradoresState by viewModel.compradoresState.collectAsStateWithLifecycle()
    val preciosCafeState by viewModel.preciosCafeState.collectAsStateWithLifecycle()
    val calculatorDraft by viewModel.calculatorDraft.collectAsStateWithLifecycle()
    val liveMarket by viewModel.liveMarketState.collectAsStateWithLifecycle()
    val isLoadingMarket by viewModel.isLoadingMarket.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val aiTitle by viewModel.aiTitle.collectAsStateWithLifecycle()
    val aiState by viewModel.aiState.collectAsStateWithLifecycle()
    val mapFocusTarget by viewModel.mapFocusTarget.collectAsStateWithLifecycle()
    val agronomyTargetFarmCode by viewModel.agronomyTargetFarmCode.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        withFrameNanos { }
        if (liveMarket.fetchedAt == "Sin consultar") {
            viewModel.refreshLiveMarket()
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.messages.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }
    LaunchedEffect(sessionViewModel) {
        sessionViewModel.messages.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    var showAccountSheet by remember { mutableStateOf(false) }
    var editingUserProfile by remember { mutableStateOf(false) }

    LaunchedEffect(userProfile.userId, activeAppRole) {
        withFrameNanos { }
        viewModel.triggerLegacyMigration(activeDisplayName, activeAppRole)
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val navigateTo: (String) -> Unit = remember(navController, drawerState, scope) {
        { logicalRoute ->
            val destination = normalizeRoute(logicalRoute)
            scope.launch { drawerState.close() }
            if (destination == "inicio") {
                navController.navigate("inicio") {
                    popUpTo("inicio") { inclusive = true }
                    launchSingleTop = true
                }
            } else {
                navController.navigate(destination) {
                    launchSingleTop = true
                }
            }
        }
    }

    val openProfileDetail: (String) -> Unit = remember(navController, drawerState, scope) {
        { code ->
            scope.launch { drawerState.close() }
            val encoded = URLEncoder.encode(code, "UTF-8")
            navController.navigate("detalle/$encoded") {
                launchSingleTop = true
            }
        }
    }

    val registerVisitForFarm: (String) -> Unit = remember(viewModel, navigateTo) {
        { farmCodeOrId ->
            viewModel.prepareAgronomyVisitForFarm(farmCodeOrId)
            navigateTo("agronomia")
        }
    }

    val userInitials = remember(activeDisplayName, user.email) {
        val src = activeDisplayName.trim().takeIf { it.isNotEmpty() && it != "Caficultor" } ?: user.email ?: "C"
        src.split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }
            .ifBlank { "C" }
    }

    val scaffoldContent: @Composable (isWideScreen: Boolean) -> Unit = { isWideScreen ->
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                val (title, subtitle) = topBarTitleForRoute(currentRoute)
                Column {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        },
                        navigationIcon = {
                            when {
                                canPopBack -> {
                                    IconButton(
                                        onClick = { navController.popBackStack() },
                                        modifier = Modifier.testTag("topbar_back_button"),
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Volver",
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                                !isWideScreen -> {
                                    IconButton(
                                        onClick = { scope.launch { drawerState.open() } },
                                        modifier = Modifier.testTag("topbar_menu_button"),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Menu,
                                            contentDescription = "Abrir menú de módulos",
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                            }
                        },
                        actions = {
                            if (currentRoute != "inicio" && currentRoute != null) {
                                TextButton(
                                    onClick = { navigateTo("inicio") },
                                    modifier = Modifier.testTag("topbar_home_button"),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Home,
                                        contentDescription = "Inicio",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "Inicio",
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                    )
                                }
                            }
                            IconButton(
                                onClick = { viewModel.analyzeCostsAndMarket() },
                                modifier = Modifier.testTag("topbar_ai_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Asesor IA",
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                            Surface(
                                onClick = { showAccountSheet = true },
                                shape = CircleShape,
                                color = Color(0xFFE8D7BD),
                                border = BorderStroke(1.dp, RoastedBrown.copy(alpha = 0.25f)),
                                modifier = Modifier
                                    .padding(end = 10.dp)
                                    .size(36.dp)
                                    .testTag("account_menu_button"),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = userInitials,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = RoastedBrown,
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color(0xFFFFFDF9),
                        ),
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                }
            },
            bottomBar = {
                if (!isWideScreen) {
                    val roleBottomTabs = mobileBottomTabsForRole(activeAppRole)
                    val primaryBottomRoutes = roleBottomTabs.map { it.route }
                    val isMoreActive = drawerState.isOpen ||
                        (currentRoute != null && currentRoute !in primaryBottomRoutes)
                    Column {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        NavigationBar(
                            containerColor = Color(0xFFFFFDF9),
                            tonalElevation = 0.dp,
                        ) {
                            roleBottomTabs.forEach { tab ->
                                val selected = currentRoute == tab.route && !drawerState.isOpen
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateTo(tab.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label,
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = tab.label,
                                            maxLines = 1,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = ForestGreen,
                                        selectedTextColor = ForestGreen,
                                        indicatorColor = SagePale,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    ),
                                    modifier = Modifier.testTag("bottom_tab_${tab.route}"),
                                )
                            }
                            NavigationBarItem(
                                selected = isMoreActive,
                                onClick = {
                                    scope.launch {
                                        if (drawerState.isOpen) drawerState.close() else drawerState.open()
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.GridView,
                                        contentDescription = "Más secciones",
                                    )
                                },
                                label = {
                                    Text(
                                        text = "Más",
                                        maxLines = 1,
                                        fontWeight = if (isMoreActive) FontWeight.Bold else FontWeight.Medium,
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = ForestGreen,
                                    selectedTextColor = ForestGreen,
                                    indicatorColor = SagePale,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                                modifier = Modifier.testTag("bottom_tab_mas"),
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "inicio",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                composable("inicio") {
                    HomeScreen(
                        currentUserId = user.uid,
                        appRole = activeAppRole,
                        userDisplayName = user.displayName,
                        userProfile = userProfile,
                        fincasState = fincasState,
                        cosechasState = cosechasState,
                        cultivosState = cultivosState,
                        costosState = costosState,
                        visitasState = visitasState,
                        inquietudesState = inquietudesState,
                        proveedoresState = proveedoresState,
                        compradoresState = compradoresState,
                        preciosCafeState = preciosCafeState,
                        liveMarket = liveMarket,
                        onNavigate = navigateTo,
                        onEditProfile = { editingUserProfile = true },
                        onAskMarketAi = { viewModel.analyzeCostsAndMarket() },
                    )
                }

                composable("guia") {
                    GuideScreen(onGoHome = { navigateTo("inicio") })
                }

                composable("fincas") {
                    FincasScreen(
                        currentUserId = user.uid,
                        appRole = activeAppRole,
                        fincasState = fincasState,
                        isSaving = isSaving,
                        onSave = { profile, done -> viewModel.saveFinca(profile, done) },
                        onDelete = { id -> viewModel.deleteFinca(id) },
                        onOpenProfile = openProfileDetail,
                        onOpenInMap = { lat, lon ->
                            viewModel.focusMapAt(lat, lon)
                            navigateTo("mapa")
                        },
                        onRegisterVisitForFarm = registerVisitForFarm,
                        onGeocode = { addr -> viewModel.geocodeAddress(addr) },
                        onNotify = { msg -> viewModel.notify(msg) },
                    )
                }

                composable("proveedores") {
                    ProvidersScreen(
                        currentUserId = user.uid,
                        appRole = activeAppRole,
                        proveedoresState = proveedoresState,
                        isSaving = isSaving,
                        onSave = { item, done -> viewModel.saveProveedor(item, done) },
                        onDelete = { id -> viewModel.deleteProveedor(id) },
                        onGeocode = { addr -> viewModel.geocodeAddress(addr) },
                        onNotify = { msg -> viewModel.notify(msg) },
                    )
                }

                composable("mapa") {
                    MapScreen(
                        currentUserId = user.uid,
                        appRole = activeAppRole,
                        fincasState = fincasState,
                        compradoresState = compradoresState,
                        proveedoresState = proveedoresState,
                        isSaving = isSaving,
                        focusCoordinate = mapFocusTarget,
                        onConsumeFocusCoordinate = { viewModel.clearMapFocusTarget() },
                        onSaveFinca = { profile, done -> viewModel.saveFinca(profile, done) },
                        onSaveComprador = { item, done -> viewModel.saveComprador(item, done) },
                        onDeleteComprador = { id -> viewModel.deleteComprador(id) },
                        onSaveProveedor = { item, done -> viewModel.saveProveedor(item, done) },
                        onOpenFincaProfile = openProfileDetail,
                        onRegisterVisitForFarm = registerVisitForFarm,
                        onGeocode = { addr -> viewModel.geocodeAddress(addr) },
                        onNotify = { msg -> viewModel.notify(msg) },
                    )
                }

                composable("precios") {
                    PricesScreen(
                        currentUserId = user.uid,
                        appRole = activeAppRole,
                        compradoresState = compradoresState,
                        preciosCafeState = preciosCafeState,
                        liveMarket = liveMarket,
                        isLoadingMarket = isLoadingMarket,
                        isSaving = isSaving,
                        onRefreshLiveMarket = { viewModel.refreshLiveMarket() },
                        onSavePrecioCafe = { item, done -> viewModel.savePrecioCafe(item, done) },
                        onDeletePrecioCafe = { id -> viewModel.deletePrecioCafe(id) },
                        onSaveComprador = { item, done -> viewModel.saveComprador(item, done) },
                        onDeleteComprador = { id -> viewModel.deleteComprador(id) },
                        onGeocode = { addr -> viewModel.geocodeAddress(addr) },
                        onAskMarketAi = { viewModel.analyzeCostsAndMarket() },
                        onNotify = { msg -> viewModel.notify(msg) },
                    )
                }

                composable("escuela") {
                    SchoolScreen()
                }

                composable("costos") {
                    CostsCalculatorScreen(
                        currentUserId = user.uid,
                        appRole = activeAppRole,
                        draft = calculatorDraft,
                        fincasState = fincasState,
                        isSaving = isSaving,
                        onUpdateMeta = { name, code, farmName, kg, offer ->
                            viewModel.updateDraftMeta(name, code, farmName, kg, offer)
                        },
                        onAddLine = { viewModel.addCalcLine() },
                        onUpdateLine = { idx, ln -> viewModel.updateCalcLine(idx, ln) },
                        onRemoveLine = { idx -> viewModel.removeCalcLine(idx) },
                        onSave = { viewModel.saveCalculator { navigateTo("historial") } },
                        onReset = { viewModel.resetCalculator() },
                        onOpenHistory = { navigateTo("historial") },
                        onAskCostsAi = { viewModel.analyzeCostsAndMarket() },
                    )
                }

                composable("historial") {
                    CostsHistoryScreen(
                        appRole = activeAppRole,
                        costosState = costosState,
                        onReopenInCalculator = { calc ->
                            viewModel.loadCalculationIntoEditor(calc)
                            navigateTo("costos")
                        },
                        onDelete = { id -> viewModel.deleteCosto(id) },
                        onNewCalculation = { navigateTo("costos") },
                    )
                }

                composable("cosecha") {
                    HarvestScreen(
                        currentUserId = user.uid,
                        appRole = activeAppRole,
                        cosechasState = cosechasState,
                        fincasState = fincasState,
                        isSaving = isSaving,
                        onSave = { rec, done -> viewModel.saveCosecha(rec, done) },
                        onDelete = { id -> viewModel.deleteCosecha(id) },
                    )
                }

                composable("cultivo") {
                    CropLogScreen(
                        currentUserId = user.uid,
                        appRole = activeAppRole,
                        cultivosState = cultivosState,
                        fincasState = fincasState,
                        isSaving = isSaving,
                        onSave = { act, done -> viewModel.saveCultivo(act, done) },
                        onDelete = { id -> viewModel.deleteCultivo(id) },
                    )
                }

                composable("recolectores") {
                    CollectorsScreen(
                        appRole = activeAppRole,
                        recolectoresState = recolectoresState,
                        isSaving = isSaving,
                        onSave = { post, done -> viewModel.saveRecolector(post, done) },
                        onDelete = { id -> viewModel.deleteRecolector(id) },
                    )
                }

                composable("agronomia") {
                    AgronomyScreen(
                        currentUserId = user.uid,
                        appRole = activeAppRole,
                        userProfile = userProfile,
                        visitasState = visitasState,
                        inquietudesState = inquietudesState,
                        fincasState = fincasState,
                        isSaving = isSaving,
                        preselectedFarmCode = agronomyTargetFarmCode,
                        onConsumePreselectedFarm = { viewModel.consumeAgronomyVisitTarget() },
                        onSave = { visit, done -> viewModel.saveVisita(visit, done) },
                        onDelete = { id -> viewModel.deleteVisita(id) },
                        onSaveInquietud = { inq, done -> viewModel.saveInquietud(inq, done) },
                        onReplyInquietud = { id, reply, done -> viewModel.replyInquietud(id, reply, done) },
                        onDeleteInquietud = { id -> viewModel.deleteInquietud(id) },
                        onAskAgronomyAi = { question, ctx -> viewModel.analyzeAgronomyAdvice(question, ctx) },
                    )
                }

                composable("perfil-caficultor") {
                    RoleProfileScreen(
                        role = "perfil-caficultor",
                        activeAppRole = activeAppRole,
                        fincasState = fincasState,
                        visitasState = visitasState,
                        proveedoresState = proveedoresState,
                        compradoresState = compradoresState,
                        onNavigate = navigateTo,
                        onOpenProfile = openProfileDetail,
                    )
                }

                composable("perfil-ingeniero") {
                    RoleProfileScreen(
                        role = "perfil-ingeniero",
                        activeAppRole = activeAppRole,
                        fincasState = fincasState,
                        visitasState = visitasState,
                        proveedoresState = proveedoresState,
                        compradoresState = compradoresState,
                        onNavigate = navigateTo,
                        onOpenProfile = openProfileDetail,
                    )
                }

                composable("perfil-proveedor") {
                    RoleProfileScreen(
                        role = "perfil-proveedor",
                        activeAppRole = activeAppRole,
                        fincasState = fincasState,
                        visitasState = visitasState,
                        proveedoresState = proveedoresState,
                        compradoresState = compradoresState,
                        onNavigate = navigateTo,
                        onOpenProfile = openProfileDetail,
                    )
                }

                composable("perfil-comprador") {
                    RoleProfileScreen(
                        role = "perfil-comprador",
                        activeAppRole = activeAppRole,
                        fincasState = fincasState,
                        visitasState = visitasState,
                        proveedoresState = proveedoresState,
                        compradoresState = compradoresState,
                        onNavigate = navigateTo,
                        onOpenProfile = openProfileDetail,
                    )
                }

                composable(
                    route = "detalle/{code}",
                    arguments = listOf(navArgument("code") { type = NavType.StringType }),
                ) { entry ->
                    val rawCode = entry.arguments?.getString("code").orEmpty()
                    val decoded = runCatching { URLDecoder.decode(rawCode, "UTF-8") }.getOrDefault(rawCode)
                    ProfileDetailScreen(
                        codeOrId = decoded,
                        currentUserId = user.uid,
                        appRole = activeAppRole,
                        fincasState = fincasState,
                        cultivosState = cultivosState,
                        visitasState = visitasState,
                        inquietudesState = inquietudesState,
                        onBack = { navController.popBackStack() },
                        onNavigate = navigateTo,
                        onRegisterVisit = registerVisitForFarm,
                        onNotify = { msg -> viewModel.notify(msg) },
                    )
                }
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 720.dp
        if (isWideScreen) {
            PermanentNavigationDrawer(
                drawerContent = {
                    PermanentDrawerSheet(
                        modifier = Modifier.width(262.dp),
                        drawerContainerColor = ForestGreenDeep,
                        windowInsets = WindowInsets(0, 0, 0, 0),
                    ) {
                        ForestGreenSidebarContent(
                            currentRoute = currentRoute,
                            activeAppRole = activeAppRole,
                            onNavigate = navigateTo,
                        )
                    }
                },
            ) {
                scaffoldContent(true)
            }
        } else {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(
                        modifier = Modifier.width(292.dp),
                        drawerContainerColor = ForestGreenDeep,
                        windowInsets = WindowInsets(0, 0, 0, 0),
                    ) {
                        ForestGreenSidebarContent(
                            currentRoute = currentRoute,
                            activeAppRole = activeAppRole,
                            onNavigate = navigateTo,
                        )
                    }
                },
            ) {
                scaffoldContent(false)
            }
        }
    }

    // Account & Role Switcher Sheet
    if (showAccountSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showAccountSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.background,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = ForestGreen,
                            modifier = Modifier.size(48.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = userInitials,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = activeDisplayName,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = user.email.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(4.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(50),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(12.dp),
                                    )
                                    Text(
                                        text = "${sessionState.roleDisplayLabel} · Rol permanente",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(14.dp))
                                Text(
                                    text = userProfile.phone,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                                Text(
                                    text = userProfile.location,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = {
                        showAccountSheet = false
                        editingUserProfile = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("account_edit_profile_button"),
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Editar nombre, celular y lugar")
                }

                Text(
                    text = "EXPLORAR PERFILES",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
                listOf(
                    Triple("perfil-caficultor", Icons.Default.Person, "Caficultor · Mi finca y comunidad"),
                    Triple("perfil-ingeniero", Icons.Default.Eco, "Ingeniero agrónomo · Visitas e inquietudes"),
                    Triple("perfil-proveedor", Icons.Default.Inventory2, "Proveedor · Productos y catálogo"),
                    Triple("perfil-comprador", Icons.Default.ShoppingBag, "Comprador · Precios diarios y QR"),
                    Triple("guia", Icons.Default.Book, "Folleto de orientación"),
                ).forEach { (route, icon, label) ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showAccountSheet = false
                                navigateTo(route)
                            },
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Text(label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                OutlinedButton(
                    onClick = {
                        showAccountSheet = false
                        sessionViewModel.signOut(credentialManager)
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerClay),
                    border = BorderStroke(1.dp, DangerClay.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("sign_out_button"),
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Cerrar sesión de Google")
                }

                Spacer(Modifier.height(12.dp))
            }
        }
    }

    if (editingUserProfile) {
        UserProfileFormDialog(
            initial = userProfile,
            googleEmail = user.email.orEmpty(),
            isSaving = isSavingProfile,
            onDismiss = { editingUserProfile = false },
            onSave = { updated ->
                sessionViewModel.saveUserProfile(updated) {
                    editingUserProfile = false
                }
            },
        )
    }

    AiAdvisoryDialog(
        title = aiTitle,
        aiState = aiState,
        onDismiss = { viewModel.dismissAi() },
    )
}

@Composable
private fun ForestGreenSidebarContent(
    currentRoute: String?,
    activeAppRole: AppRole,
    onNavigate: (String) -> Unit,
) {
    val ownRouteSet = remember(activeAppRole) {
        when (activeAppRole) {
            AppRole.CAFICULTOR -> setOf("inicio", "fincas", "cultivo", "cosecha", "costos", "recolectores", "mapa", "agronomia", "precios")
            AppRole.AGRONOMO -> setOf("inicio", "agronomia", "fincas", "mapa")
            AppRole.PROVEEDOR -> setOf("inicio", "proveedores", "mapa")
            AppRole.COMPRADOR -> setOf("inicio", "precios", "mapa")
        }
    }
    val myModules = remember(ownRouteSet) {
        primarySidebarModules.filter { it.route in ownRouteSet }
    }
    val communityModules = remember(ownRouteSet) {
        primarySidebarModules.filter { it.route !in ownRouteSet }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(ForestGreenDeep, ForestGreenMid, ForestGreenLight)
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Brand Identity Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFFFDF7),
                modifier = Modifier.size(44.dp),
            ) {
                AsyncDrawableImage(
                    resId = R.drawable.ic_launcher_art,
                    contentDescription = "Logo de ALBÁN CAFÉ 360°",
                    contentScale = ContentScale.Crop,
                    maxDimPx = 256,
                    placeholderColor = Color(0xFFFFFDF7),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(3.dp)
                        .clip(RoundedCornerShape(10.dp)),
                )
            }
            Column {
                Text(
                    text = "ALBÁN CAFÉ 360°",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                )
                Text(
                    text = "CAFÉ CONECTA · NARIÑO",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFC9D5C5),
                    fontSize = 10.sp,
                    letterSpacing = 1.1.sp,
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Text(
            text = "MI ESPACIO · ${activeAppRole.shortUpperLabel}",
            style = MaterialTheme.typography.labelSmall,
            color = WarmGold,
            letterSpacing = 1.3.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )

        myModules.forEach { item ->
            val selected = currentRoute != null && item.matchPrefixes.any { prefix ->
                currentRoute == prefix || currentRoute.startsWith(prefix)
            }
            Surface(
                onClick = { onNavigate(item.route) },
                color = if (selected) Color.White.copy(alpha = 0.14f) else Color.Transparent,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sidebar_item_${item.route}"),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp, horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (selected) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(20.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(WarmGold),
                        )
                    }
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = if (selected) WarmGold else Color(0xFFC9D9CE),
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (selected) Color.White else Color(0xFFC9D9CE),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Text(
            text = "COMUNIDAD (SOLO LECTURA)",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFA5BCB0),
            letterSpacing = 1.3.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )

        communityModules.forEach { item ->
            val selected = currentRoute != null && item.matchPrefixes.any { prefix ->
                currentRoute == prefix || currentRoute.startsWith(prefix)
            }
            Surface(
                onClick = { onNavigate(item.route) },
                color = if (selected) Color.White.copy(alpha = 0.14f) else Color.Transparent,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sidebar_item_${item.route}"),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp, horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (selected) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(20.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(WarmGold),
                        )
                    }
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = if (selected) WarmGold else Color(0xFFC9D9CE),
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (selected) Color.White else Color(0xFFC9D9CE),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        Text(
            text = "ROL PERMANENTE · ${activeAppRole.shortUpperLabel}",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFC4D5C5),
            letterSpacing = 1.2.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )

        roleSidebarProfiles.forEach { roleItem ->
            val selected = currentRoute == roleItem.route
            Surface(
                onClick = { onNavigate(roleItem.route) },
                color = if (selected) WarmGold else Color.White.copy(alpha = 0.06f),
                border = BorderStroke(
                    1.dp,
                    if (selected) WarmGold else Color.White.copy(alpha = 0.14f),
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
                    .testTag("sidebar_item_${roleItem.route}"),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = roleItem.icon,
                        contentDescription = null,
                        tint = if (selected) ForestGreen else Color(0xFFC9D9CE),
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = roleItem.label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) ForestGreen else Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Original Sidebar Bottom Location Card
        Surface(
            color = Color.White.copy(alpha = 0.06f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
            shape = RoundedCornerShape(13.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "San José de Albán · Nariño",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Text(
                    text = "Del cultivo a la comercialización: información para decidir con confianza.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFBDD0C1),
                )
            }
        }
    }
}
