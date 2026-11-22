package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditLocationAlt
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.NominatimResult
import com.example.model.AppRole
import com.example.model.CompradorItem
import com.example.model.FincaProfile
import com.example.model.ProveedorItem
import com.example.model.UiState
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FormModalDialog
import com.example.ui.components.LocationPickerSection
import com.example.ui.components.NoticeBanner
import com.example.ui.components.SectionHeader
import com.example.ui.components.StateContainer
import com.example.ui.theme.DangerClay
import com.example.ui.theme.PinComprador
import com.example.ui.theme.PinFinca
import com.example.ui.theme.PinProveedor
import com.example.ui.theme.PinVendedor
import com.example.util.OsmLayerMode
import com.example.util.applyOsmTileSource
import com.example.util.configureOsmDroidCache
import com.example.util.createColoredMapPinDrawable
import com.example.util.fetchCurrentLocation
import com.example.util.fitMapToPoints
import com.example.util.formatCop
import com.example.util.parseCoordinateOrNull
import com.example.util.todayIsoDate
import kotlinx.coroutines.launch
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.ScaleBarOverlay
import java.util.Locale

private data class MapPlaceEntry(
    val id: String,
    val type: String, // "finca" | "comprador" | "vendedor" | "proveedor"
    val label: String,
    val code: String,
    val name: String,
    val location: String,
    val subtitle: String,
    val details: String,
    val latitude: Double,
    val longitude: Double,
    val color: Color,
    val rawFinca: FincaProfile? = null,
    val rawComprador: CompradorItem? = null,
    val rawProveedor: ProveedorItem? = null,
    val isOwned: Boolean = false,
)

@SuppressLint("ClickableViewAccessibility")
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MapScreen(
    currentUserId: String,
    appRole: AppRole,
    fincasState: UiState<List<FincaProfile>>,
    compradoresState: UiState<List<CompradorItem>>,
    proveedoresState: UiState<List<ProveedorItem>>,
    isSaving: Boolean,
    focusCoordinate: Pair<Double, Double>? = null,
    onConsumeFocusCoordinate: () -> Unit = {},
    onSaveFinca: (FincaProfile, () -> Unit) -> Unit,
    onSaveComprador: (CompradorItem, () -> Unit) -> Unit,
    onDeleteComprador: (String) -> Unit,
    onSaveProveedor: (ProveedorItem, () -> Unit) -> Unit,
    onOpenFincaProfile: (String) -> Unit,
    onRegisterVisitForFarm: (String) -> Unit = {},
    onGeocode: suspend (String) -> Result<NominatimResult?>,
    onNotify: (String) -> Unit,
) {
    val allowedPointTypes = remember(appRole) {
        when (appRole) {
            AppRole.CAFICULTOR -> listOf("finca")
            AppRole.COMPRADOR -> listOf("comprador", "vendedor")
            AppRole.PROVEEDOR -> listOf("proveedor")
            AppRole.AGRONOMO -> emptyList()
        }
    }
    val canMarkPoints = allowedPointTypes.isNotEmpty()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var search by remember { mutableStateOf("") }
    var filterType by remember { mutableStateOf("todos") }
    var layerMode by remember { mutableStateOf(OsmLayerMode.SATELLITE) }
    var isMarkingFarmMode by remember { mutableStateOf(false) }
    var pendingTapGeoPoint by remember { mutableStateOf<GeoPoint?>(null) }
    var showAddPointDialog by remember { mutableStateOf(false) }
    var selectedPointType by remember(allowedPointTypes) {
        mutableStateOf(allowedPointTypes.firstOrNull() ?: "finca")
    }
    LaunchedEffect(allowedPointTypes) {
        if (allowedPointTypes.isNotEmpty() && selectedPointType !in allowedPointTypes) {
            selectedPointType = allowedPointTypes.first()
        }
    }
    var selectedPlace by remember { mutableStateOf<MapPlaceEntry?>(null) }
    var creatingFinca by remember { mutableStateOf<FincaProfile?>(null) }
    var editingFinca by remember { mutableStateOf<FincaProfile?>(null) }
    var creatingComprador by remember { mutableStateOf<CompradorItem?>(null) }
    var editingComprador by remember { mutableStateOf<CompradorItem?>(null) }
    var creatingProveedor by remember { mutableStateOf<ProveedorItem?>(null) }
    var editingProveedor by remember { mutableStateOf<ProveedorItem?>(null) }
    var deleteCompradorTarget by remember { mutableStateOf<CompradorItem?>(null) }
    var hasAutoFitted by remember { mutableStateOf(false) }

    val fincas = (fincasState as? UiState.Success)?.data.orEmpty()
    val myRegisteredFincas = remember(fincas, currentUserId) {
        fincas.filter { it.recordType == "finca" && it.userId == currentUserId }
    }
    val fincasWithoutCoordinates = remember(myRegisteredFincas) {
        myRegisteredFincas.filter { !it.hasValidCoordinates }
    }
    val compradores = (compradoresState as? UiState.Success)?.data.orEmpty()
    val proveedores = (proveedoresState as? UiState.Success)?.data.orEmpty()

    // Strictly filter out records without valid coordinates (fixes Null Island [0,0] bug)
    val allPlaces = remember(fincas, compradores, proveedores, currentUserId) {
        buildList {
            fincas.filter { it.recordType == "finca" && it.hasValidCoordinates }.forEach { f ->
                val owned = f.userId == currentUserId
                val ownerLabel = if (owned) "Propia" else f.ownerName.ifBlank { "Comunidad" }
                add(
                    MapPlaceEntry(
                        id = "finca:${f.id}",
                        type = "finca",
                        label = "Finca · $ownerLabel",
                        code = f.recordCode.ifBlank { f.id },
                        name = f.name,
                        location = f.location,
                        subtitle = f.subtitle,
                        details = f.details,
                        latitude = f.latitude!!,
                        longitude = f.longitude!!,
                        color = PinFinca,
                        rawFinca = f,
                        isOwned = owned,
                    )
                )
            }
            compradores.filter { it.hasValidCoordinates }.forEach { c ->
                val isSeller = c.actorType == "vendedor"
                add(
                    MapPlaceEntry(
                        id = "${c.actorType}:${c.id}",
                        type = c.actorType,
                        label = if (isSeller) "Vendedor" else "Comprador",
                        code = c.recordCode.ifBlank { c.id },
                        name = c.name,
                        location = c.location,
                        subtitle = buildString {
                            if (c.purchaseType.isNotBlank()) append(c.purchaseType)
                            if (c.offerAmountCop > 0.0) {
                                if (isNotEmpty()) append(" · ")
                                append("${formatCop(c.offerAmountCop)} / ${c.priceUnit}")
                            }
                        },
                        details = c.details,
                        latitude = c.latitude!!,
                        longitude = c.longitude!!,
                        color = if (isSeller) PinVendedor else PinComprador,
                        rawComprador = c,
                        isOwned = c.userId == currentUserId,
                    )
                )
            }
            proveedores.filter { it.hasValidCoordinates }.forEach { p ->
                add(
                    MapPlaceEntry(
                        id = "proveedor:${p.id}",
                        type = "proveedor",
                        label = "Proveedor",
                        code = p.recordCode.ifBlank { p.id },
                        name = p.name,
                        location = p.location,
                        subtitle = p.productsServices,
                        details = p.details,
                        latitude = p.latitude!!,
                        longitude = p.longitude!!,
                        color = PinProveedor,
                        rawProveedor = p,
                        isOwned = p.userId == currentUserId,
                    )
                )
            }
        }
    }

    val filteredPlaces = remember(allPlaces, search, filterType) {
        allPlaces.filter { p ->
            (filterType == "todos" || p.type == filterType) &&
                (search.isBlank() || listOf(p.name, p.location, p.subtitle, p.code).any {
                    it.contains(search, ignoreCase = true)
                })
        }
    }

    val pinDrawables = remember(context) {
        mapOf(
            "finca" to createColoredMapPinDrawable(context, PinFinca.toArgb()),
            "comprador" to createColoredMapPinDrawable(context, PinComprador.toArgb()),
            "vendedor" to createColoredMapPinDrawable(context, PinVendedor.toArgb()),
            "proveedor" to createColoredMapPinDrawable(context, PinProveedor.toArgb()),
            "pending" to createColoredMapPinDrawable(context, DangerClay.toArgb()),
        )
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    // Keep MapView instance stable across search keystrokes and prevent parent LazyColumn from stealing map drags
    val mapView = remember {
        configureOsmDroidCache(context)
        MapView(context).apply {
            applyOsmTileSource(this, OsmLayerMode.SATELLITE)
            setMultiTouchControls(true)
            setBuiltInZoomControls(false)
            controller.setZoom(13.5)
            controller.setCenter(GeoPoint(1.473, -77.07))
            setOnTouchListener { v, _ ->
                v.parent?.requestDisallowInterceptTouchEvent(true)
                false
            }
        }
    }

    val scaleBarOverlay = remember(mapView) {
        ScaleBarOverlay(mapView).apply {
            setAlignBottom(true)
            setScaleBarOffset(24, 24)
        }
    }

    val gpsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            scope.launch {
                val coords = fetchCurrentLocation(context)
                if (coords != null) {
                    val point = GeoPoint(coords.latitude, coords.longitude)
                    mapView.controller.animateTo(point, 16.0, 700L)
                    onNotify("Mapa centrado en tu ubicación GPS.")
                } else {
                    onNotify("No se obtuvo señal GPS activa en este momento.")
                }
            }
        } else {
            onNotify("Permiso de ubicación necesario para centrar el mapa con tu GPS.")
        }
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        mapView.onResume()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onPause()
            mapView.onDetach()
        }
    }

    LaunchedEffect(focusCoordinate) {
        if (focusCoordinate != null) {
            hasAutoFitted = true
            mapView.controller.animateTo(GeoPoint(focusCoordinate.first, focusCoordinate.second), 16.5, 650L)
            onConsumeFocusCoordinate()
        }
    }

    LaunchedEffect(allPlaces.size) {
        if (!hasAutoFitted && allPlaces.isNotEmpty() && focusCoordinate == null) {
            hasAutoFitted = true
            fitMapToPoints(mapView, allPlaces.map { it.latitude to it.longitude })
        }
    }

    LaunchedEffect(layerMode, filteredPlaces, isMarkingFarmMode, pendingTapGeoPoint, canMarkPoints) {
        applyOsmTileSource(mapView, layerMode)
        mapView.overlays.clear()

        val eventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                if (canMarkPoints && isMarkingFarmMode) {
                    pendingTapGeoPoint = p
                    showAddPointDialog = true
                    return true
                }
                return false
            }

            override fun longPressHelper(p: GeoPoint): Boolean {
                if (canMarkPoints) {
                    pendingTapGeoPoint = p
                    showAddPointDialog = true
                    return true
                }
                return false
            }
        })
        mapView.overlays.add(eventsOverlay)
        mapView.overlays.add(scaleBarOverlay)

        filteredPlaces.forEach { place ->
            val marker = Marker(mapView).apply {
                position = GeoPoint(place.latitude, place.longitude)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                icon = pinDrawables[place.type]
                title = "${place.label}: ${place.name}"
                snippet = place.location
                setOnMarkerClickListener { _, _ ->
                    selectedPlace = place
                    true
                }
            }
            mapView.overlays.add(marker)
        }

        pendingTapGeoPoint?.let { tapPoint ->
            val previewMarker = Marker(mapView).apply {
                position = tapPoint
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                icon = pinDrawables["pending"]
                title = "Punto seleccionado"
                snippet = String.format(Locale.US, "%.5f, %.5f", tapPoint.latitude, tapPoint.longitude)
            }
            mapView.overlays.add(previewMarker)
        }

        mapView.invalidate()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionHeader(
                kicker = "TERRITORIO Y GEOLOCALIZACIÓN",
                title = "Mapa satelital y fincas",
                subtitle = when (appRole) {
                    AppRole.CAFICULTOR -> "Visualiza el terreno y ubica tus propias fincas con GPS o marcado manual en el mapa."
                    AppRole.AGRONOMO -> "Explora las fincas de la comunidad y toca un pin para abrir su ficha o registrar una visita técnica."
                    AppRole.PROVEEDOR -> "Explora el mapa territorial y marca la ubicación comercial de tu punto proveedor."
                    AppRole.COMPRADOR -> "Explora las fincas del territorio y ubica tus puntos de compra o venta con GPS o marcado manual."
                },
                action = if (canMarkPoints) {
                    {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Button(
                                onClick = {
                                    pendingTapGeoPoint = null
                                    showAddPointDialog = true
                                },
                                modifier = Modifier.testTag("map_mark_finca_button"),
                            ) {
                                Icon(Icons.Default.AddLocationAlt, contentDescription = null, modifier = Modifier.size(17.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Agregar punto")
                            }
                            OutlinedButton(
                                onClick = {
                                    isMarkingFarmMode = !isMarkingFarmMode
                                    if (!isMarkingFarmMode) pendingTapGeoPoint = null
                                },
                                modifier = Modifier.testTag("add_comprador_button"),
                            ) {
                                Icon(Icons.Default.EditLocationAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(if (isMarkingFarmMode) "Cancelar toque" else "Marcar manual")
                            }
                        }
                    }
                } else null,
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    label = { Text("Buscar lugar, dirección o código") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    listOf(
                        "todos" to "Todos",
                        "finca" to "Fincas",
                        "comprador" to "Compradores",
                        "vendedor" to "Vendedores",
                        "proveedor" to "Proveedores",
                    ).forEach { (key, label) ->
                        FilterChip(
                            selected = filterType == key,
                            onClick = { filterType = key },
                            label = { Text(label) },
                        )
                    }
                }
            }
        }

        if (appRole == AppRole.CAFICULTOR && fincasWithoutCoordinates.isNotEmpty()) {
            item {
                NoticeBanner(
                    "Tienes ${fincasWithoutCoordinates.size} finca(s) sin coordenadas aún. Activa 'Tocar para marcar' o mantén presionado un terreno para asignarlas."
                )
            }
        } else if (appRole == AppRole.AGRONOMO) {
            item {
                NoticeBanner(
                    "Modo lectura territorial para agrónomo: toca cualquier finca en el mapa para abrir su ficha técnica o registrar una visita."
                )
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Lugares ubicados: ${filteredPlaces.size}",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = "Capa activa: ${layerMode.attribution}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (canMarkPoints) {
                            FilterChip(
                                selected = isMarkingFarmMode,
                                onClick = {
                                    isMarkingFarmMode = !isMarkingFarmMode
                                    if (!isMarkingFarmMode) pendingTapGeoPoint = null
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.EditLocationAlt,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                },
                                label = {
                                    Text(if (isMarkingFarmMode) "Marcando punto" else "Tocar para marcar")
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.primary,
                                    selectedLeadingIconColor = MaterialTheme.colorScheme.primary,
                                ),
                                modifier = Modifier.testTag("map_tap_mark_toggle"),
                            )
                        }
                    }

                    // Tile source selector (Satellite Esri, OSM Mapnik, OpenTopoMap)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        OsmLayerMode.entries.forEach { mode ->
                            FilterChip(
                                selected = layerMode == mode,
                                onClick = { layerMode = mode },
                                leadingIcon = {
                                    Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(15.dp))
                                },
                                label = { Text(mode.label) },
                                modifier = Modifier.testTag("map_layer_${mode.name.lowercase()}"),
                            )
                        }
                    }

                    if (canMarkPoints && isMarkingFarmMode) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        ) {
                            Text(
                                text = "Toca directamente el punto en el mapa para registrar su ubicación mediante coordenadas exactas.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(380.dp)
                            .clip(RoundedCornerShape(14.dp)),
                    ) {
                        AndroidView(
                            factory = { mapView },
                            modifier = Modifier.matchParentSize(),
                        )

                        // Floating zoom, GPS & territory reset buttons
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Surface(
                                onClick = {
                                    val fine = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                    ) == PackageManager.PERMISSION_GRANTED
                                    val coarse = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.ACCESS_COARSE_LOCATION,
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (fine || coarse) {
                                        scope.launch {
                                            val coords = fetchCurrentLocation(context)
                                            if (coords != null) {
                                                mapView.controller.animateTo(
                                                    GeoPoint(coords.latitude, coords.longitude),
                                                    16.0,
                                                    700L,
                                                )
                                                onNotify("Mapa centrado en tu ubicación GPS.")
                                            } else {
                                                onNotify("No se obtuvo señal GPS activa.")
                                            }
                                        }
                                    } else {
                                        gpsPermissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                            )
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("map_gps_button"),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.MyLocation,
                                        contentDescription = "Mi ubicación GPS",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(19.dp),
                                    )
                                }
                            }
                            Surface(
                                onClick = {
                                    if (filteredPlaces.isNotEmpty()) {
                                        fitMapToPoints(mapView, filteredPlaces.map { it.latitude to it.longitude })
                                    } else {
                                        mapView.controller.animateTo(GeoPoint(1.473, -77.07), 13.5, 650L)
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("map_fit_all_button"),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.ZoomOutMap,
                                        contentDescription = "Ver todos los puntos",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(19.dp),
                                    )
                                }
                            }
                            Surface(
                                onClick = {
                                    mapView.controller.animateTo(GeoPoint(1.473, -77.07), 13.5, 650L)
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("map_center_alban_button"),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.CenterFocusStrong,
                                        contentDescription = "Centrar en San José de Albán",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(19.dp),
                                    )
                                }
                            }
                            Surface(
                                onClick = { mapView.controller.zoomIn() },
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("map_zoom_in_button"),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Add, contentDescription = "Acercar mapa", modifier = Modifier.size(19.dp))
                                }
                            }
                            Surface(
                                onClick = { mapView.controller.zoomOut() },
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("map_zoom_out_button"),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Remove, contentDescription = "Alejar mapa", modifier = Modifier.size(19.dp))
                                }
                            }
                        }
                    }

                    // Pin color legend
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        MapLegendDot("Finca", PinFinca)
                        MapLegendDot("Comprador / Venta", PinComprador)
                        MapLegendDot("Vendedor", PinVendedor)
                        MapLegendDot("Proveedor", PinProveedor)
                    }

                    Text(
                        text = if (canMarkPoints) {
                            "Tip: usa 'Agregar punto' para elegir GPS o marcado manual, o mantén presionado un punto en el mapa."
                        } else {
                            "Tip: toca cualquier pin en el mapa para consultar la ficha completa de la finca o punto."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (filteredPlaces.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "No hay lugares geolocalizados",
                    subtitle = if (canMarkPoints) {
                        "Usa 'Agregar punto' para registrar tu ubicación permitida según tu rol (${appRole.displayName})."
                    } else {
                        "Aún no hay fincas ni puntos geolocalizados en la comunidad."
                    },
                    icon = Icons.Default.LocationOn,
                    actionLabel = if (canMarkPoints) "Agregar punto de referencia" else null,
                    onAction = if (canMarkPoints) {
                        {
                            pendingTapGeoPoint = null
                            showAddPointDialog = true
                        }
                    } else null,
                )
            }
        } else {
            items(filteredPlaces, key = { it.id }) { place ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            mapView.controller.animateTo(GeoPoint(place.latitude, place.longitude), 15.8, 600L)
                            selectedPlace = place
                        },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(place.color),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${place.label} · ${place.name}",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = place.location.ifBlank {
                                    String.format(Locale.US, "%.5f, %.5f", place.latitude, place.longitude)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (place.subtitle.isNotBlank()) {
                                Text(
                                    text = place.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Unified "Agregar punto de referencia" dialog supporting GPS OR Manual map tap + Role-scoped Point Type selection
    if (showAddPointDialog && canMarkPoints) {
        val center = mapView.mapCenter
        AddMapPointDialog(
            initialLat = pendingTapGeoPoint?.latitude ?: center.latitude,
            initialLon = pendingTapGeoPoint?.longitude ?: center.longitude,
            initialFromMapTap = pendingTapGeoPoint != null,
            allowedPointTypes = allowedPointTypes,
            canAssignExistingFinca = appRole.capabilities.manageOwnFarms,
            selectedType = selectedPointType,
            onSelectType = { selectedPointType = it },
            registeredFincas = myRegisteredFincas,
            isSaving = isSaving,
            onRequestManualMapPick = {
                showAddPointDialog = false
                isMarkingFarmMode = true
                onNotify("Toca el punto exacto en el mapa para fijar la ubicación.")
            },
            onAssignExistingFinca = { finca, lat, lon ->
                onSaveFinca(finca.copy(latitude = lat, longitude = lon)) {
                    showAddPointDialog = false
                    pendingTapGeoPoint = null
                    isMarkingFarmMode = false
                    mapView.controller.animateTo(GeoPoint(lat, lon), 16.0, 500L)
                    onNotify("Ubicación guardada en el mapa para ${finca.name}.")
                }
            },
            onProceedNewPoint = { pointType, lat, lon ->
                showAddPointDialog = false
                pendingTapGeoPoint = null
                isMarkingFarmMode = false
                when (pointType) {
                    "finca" -> creatingFinca = FincaProfile(
                        recordType = "finca",
                        latitude = lat,
                        longitude = lon,
                    )
                    "comprador" -> creatingComprador = CompradorItem(
                        actorType = "comprador",
                        latitude = lat,
                        longitude = lon,
                    )
                    "vendedor" -> creatingComprador = CompradorItem(
                        actorType = "vendedor",
                        latitude = lat,
                        longitude = lon,
                    )
                    "proveedor" -> creatingProveedor = ProveedorItem(
                        latitude = lat,
                        longitude = lon,
                    )
                }
            },
            onNotify = onNotify,
            onDismiss = {
                showAddPointDialog = false
                pendingTapGeoPoint = null
            },
        )
    }

    selectedPlace?.let { place ->
        AlertDialog(
            onDismissRequest = { selectedPlace = null },
            title = { Text(place.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${place.label} · ${place.code}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                    val ownerName = place.rawFinca?.ownerName?.takeIf { it.isNotBlank() }
                        ?: place.rawProveedor?.ownerName?.takeIf { it.isNotBlank() }
                    if (!ownerName.isNullOrBlank()) {
                        Text(
                            text = if (place.isOwned) "Propietario: $ownerName (Tu registro)" else "Responsable: $ownerName",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    if (place.location.isNotBlank()) Text("Zona: ${place.location}", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        String.format(Locale.US, "Coordenadas: %.6f, %.6f", place.latitude, place.longitude),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    if (place.subtitle.isNotBlank()) Text("Detalle: ${place.subtitle}", style = MaterialTheme.typography.bodyMedium)
                    if (place.details.isNotBlank()) Text(place.details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                when {
                    place.rawFinca != null -> FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (place.isOwned && appRole.capabilities.manageOwnFarms) {
                            OutlinedButton(onClick = {
                                editingFinca = place.rawFinca
                                selectedPlace = null
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Editar")
                            }
                        }
                        if (appRole.capabilities.registerAgronomyVisits) {
                            OutlinedButton(onClick = {
                                selectedPlace = null
                                onRegisterVisitForFarm(place.code)
                            }) {
                                Icon(Icons.Default.Eco, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Visita ing.")
                            }
                        }
                        Button(onClick = {
                            selectedPlace = null
                            onOpenFincaProfile(place.code)
                        }) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Abrir ficha")
                        }
                    }
                    place.rawComprador != null && place.isOwned && appRole.capabilities.publishBuyerPrices -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = {
                                deleteCompradorTarget = place.rawComprador
                                selectedPlace = null
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = DangerClay),
                        ) { Text("Eliminar") }
                        Button(onClick = {
                            editingComprador = place.rawComprador
                            selectedPlace = null
                        }) { Text("Editar") }
                    }
                    place.rawProveedor != null && place.isOwned && appRole.capabilities.publishProviderCatalog -> Button(onClick = {
                        editingProveedor = place.rawProveedor
                        selectedPlace = null
                    }) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Editar")
                    }
                    else -> Button(onClick = { selectedPlace = null }) { Text("Cerrar") }
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedPlace = null }) { Text("Cerrar") }
            },
        )
    }

    val fincaFormTarget = editingFinca ?: creatingFinca
    if (fincaFormTarget != null) {
        FincaFormDialog(
            initial = fincaFormTarget,
            isEdit = editingFinca != null,
            isSaving = isSaving,
            onGeocode = onGeocode,
            onNotify = onNotify,
            onDismiss = {
                editingFinca = null
                creatingFinca = null
            },
            onSave = { updated ->
                onSaveFinca(updated) {
                    editingFinca = null
                    creatingFinca = null
                    val lat = updated.latitude
                    val lon = updated.longitude
                    if (lat != null && lon != null) {
                        mapView.controller.animateTo(GeoPoint(lat, lon), 16.0, 500L)
                    }
                }
            },
        )
    }

    val compForm = editingComprador ?: creatingComprador
    if (compForm != null) {
        CompradorFormDialog(
            initial = compForm,
            isEdit = editingComprador != null,
            isSaving = isSaving,
            onGeocode = onGeocode,
            onNotify = onNotify,
            onDismiss = {
                editingComprador = null
                creatingComprador = null
            },
            onSave = { updated ->
                onSaveComprador(updated) {
                    editingComprador = null
                    creatingComprador = null
                    val lat = updated.latitude
                    val lon = updated.longitude
                    if (lat != null && lon != null) {
                        mapView.controller.animateTo(GeoPoint(lat, lon), 16.0, 500L)
                    }
                }
            },
        )
    }

    val provForm = editingProveedor ?: creatingProveedor
    if (provForm != null) {
        ProveedorFormDialog(
            initial = provForm,
            isEdit = editingProveedor != null,
            isSaving = isSaving,
            onGeocode = onGeocode,
            onNotify = onNotify,
            onDismiss = {
                editingProveedor = null
                creatingProveedor = null
            },
            onSave = { updated ->
                onSaveProveedor(updated) {
                    editingProveedor = null
                    creatingProveedor = null
                    val lat = updated.latitude
                    val lon = updated.longitude
                    if (lat != null && lon != null) {
                        mapView.controller.animateTo(GeoPoint(lat, lon), 16.0, 500L)
                    }
                }
            },
        )
    }

    deleteCompradorTarget?.let { target ->
        ConfirmDeleteDialog(
            itemName = target.name,
            onConfirm = {
                onDeleteComprador(target.id)
                deleteCompradorTarget = null
            },
            onDismiss = { deleteCompradorTarget = null },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddMapPointDialog(
    initialLat: Double,
    initialLon: Double,
    initialFromMapTap: Boolean,
    allowedPointTypes: List<String>,
    canAssignExistingFinca: Boolean,
    selectedType: String,
    onSelectType: (String) -> Unit,
    registeredFincas: List<FincaProfile>,
    isSaving: Boolean,
    onRequestManualMapPick: () -> Unit,
    onAssignExistingFinca: (FincaProfile, Double, Double) -> Unit,
    onProceedNewPoint: (pointType: String, lat: Double, lon: Double) -> Unit,
    onNotify: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var latText by remember(initialLat) { mutableStateOf(String.format(Locale.US, "%.6f", initialLat)) }
    var lonText by remember(initialLon) { mutableStateOf(String.format(Locale.US, "%.6f", initialLon)) }
    var locationModeLabel by remember(initialFromMapTap) {
        mutableStateOf(if (initialFromMapTap) "Marcado manual en el mapa" else "Centro del mapa / editable")
    }
    var loadingGps by remember { mutableStateOf(false) }

    val gpsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            loadingGps = true
            scope.launch {
                val coords = fetchCurrentLocation(context)
                loadingGps = false
                if (coords != null) {
                    latText = String.format(Locale.US, "%.6f", coords.latitude)
                    lonText = String.format(Locale.US, "%.6f", coords.longitude)
                    locationModeLabel = "Ubicación capturada según GPS"
                } else {
                    onNotify("No se obtuvo señal GPS activa. Puedes marcar el punto manualmente en el mapa.")
                }
            }
        } else {
            onNotify("Permiso de ubicación necesario para capturar coordenadas GPS.")
        }
    }

    val parsedLat = latText.parseCoordinateOrNull()
    val parsedLon = lonText.parseCoordinateOrNull()
    val hasValidCoords = parsedLat != null && parsedLon != null &&
        parsedLat in -90.0..90.0 && parsedLon in -180.0..180.0 &&
        !(parsedLat == 0.0 && parsedLon == 0.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar punto de referencia") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "1. Selecciona el tipo de punto",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    listOf(
                        Triple("finca", "Finca", PinFinca),
                        Triple("comprador", "Punto de venta / Comprador", PinComprador),
                        Triple("vendedor", "Vendedor", PinVendedor),
                        Triple("proveedor", "Proveedor", PinProveedor),
                    )
                        .filter { (key, _, _) -> key in allowedPointTypes }
                        .forEach { (key, label, dotColor) ->
                            FilterChip(
                                selected = selectedType == key,
                                onClick = { onSelectType(key) },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(dotColor),
                                    )
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag("point_type_$key"),
                            )
                        }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Text(
                    text = "2. Define la ubicación (GPS o manual)",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = locationModeLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            val fine = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_FINE_LOCATION,
                            ) == PackageManager.PERMISSION_GRANTED
                            val coarse = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                            ) == PackageManager.PERMISSION_GRANTED
                            if (fine || coarse) {
                                loadingGps = true
                                scope.launch {
                                    val coords = fetchCurrentLocation(context)
                                    loadingGps = false
                                    if (coords != null) {
                                        latText = String.format(Locale.US, "%.6f", coords.latitude)
                                        lonText = String.format(Locale.US, "%.6f", coords.longitude)
                                        locationModeLabel = "Ubicación capturada según GPS"
                                    } else {
                                        onNotify("No se obtuvo señal GPS activa. Puedes tocar el mapa manualmente.")
                                    }
                                }
                            } else {
                                gpsLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION,
                                    )
                                )
                            }
                        },
                        enabled = !loadingGps,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("add_point_gps_button"),
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (loadingGps) "Buscando..." else "Usar GPS", maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = onRequestManualMapPick,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("add_point_manual_map_button"),
                    ) {
                        Icon(Icons.Default.EditLocationAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Marcar mapa", maxLines = 1)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = latText,
                        onValueChange = {
                            latText = it
                            locationModeLabel = "Coordenadas ingresadas manualmente"
                        },
                        label = { Text("Latitud") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = lonText,
                        onValueChange = {
                            lonText = it
                            locationModeLabel = "Coordenadas ingresadas manualmente"
                        },
                        label = { Text("Longitud") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }

                if (canAssignExistingFinca && selectedType == "finca" && registeredFincas.isNotEmpty()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Text(
                        text = "O asignar esta ubicación a una finca existente:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    registeredFincas.forEach { finca ->
                        Surface(
                            onClick = {
                                if (!hasValidCoords) {
                                    onNotify("Ingresa o captura coordenadas válidas primero.")
                                } else {
                                    onAssignExistingFinca(finca, parsedLat!!, parsedLon!!)
                                }
                            },
                            enabled = !isSaving,
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(PinFinca),
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = finca.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    Text(
                                        text = if (finca.hasValidCoordinates) {
                                            "Reemplazar ubicación (${finca.location.ifBlank { finca.recordCode }})"
                                        } else {
                                            "Sin pin en mapa · ${finca.location.ifBlank { finca.recordCode }}"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            val confirmLabel = when (selectedType) {
                "finca" -> "Crear nueva finca"
                "comprador" -> "Continuar comprador"
                "vendedor" -> "Continuar vendedor"
                else -> "Continuar proveedor"
            }
            Button(
                onClick = {
                    if (!hasValidCoords) {
                        onNotify("Coordenadas inválidas. Usa GPS o toca el mapa.")
                    } else {
                        onProceedNewPoint(selectedType, parsedLat!!, parsedLon!!)
                    }
                },
                enabled = !isSaving && !loadingGps,
                modifier = Modifier.testTag("map_create_new_finca_at_point"),
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}

@Composable
private fun MapLegendDot(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private val providerCategories = listOf(
    "Fertilizantes y abonos",
    "Semillas y almácigos",
    "Herramientas y equipos",
    "Control fitosanitario",
    "Servicios técnicos",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProvidersScreen(
    currentUserId: String,
    appRole: AppRole,
    proveedoresState: UiState<List<ProveedorItem>>,
    isSaving: Boolean,
    onSave: (ProveedorItem, () -> Unit) -> Unit,
    onDelete: (String) -> Unit,
    onGeocode: suspend (String) -> Result<NominatimResult?>,
    onNotify: (String) -> Unit,
) {
    val canPublishCatalog = appRole.capabilities.publishProviderCatalog
    var search by remember { mutableStateOf("") }
    var viewMode by remember(canPublishCatalog) {
        mutableStateOf(if (canPublishCatalog) "mios" else "explorar")
    }
    var categoryFilter by remember { mutableStateOf("Todos") }
    var editing by remember { mutableStateOf<ProveedorItem?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<ProveedorItem?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            SectionHeader(
                kicker = "GESTIÓN Y CATÁLOGO COMERCIAL",
                title = "Proveedores y productos",
                subtitle = if (canPublishCatalog) {
                    "Publica y administra tu catálogo de productos, insumos y precios de referencia."
                } else {
                    "Consulta el catálogo de insumos, herramientas y servicios publicados por los proveedores."
                },
                action = if (canPublishCatalog) {
                    {
                        Button(
                            onClick = { creating = true },
                            modifier = Modifier.testTag("add_proveedor_button"),
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Publicar producto")
                        }
                    }
                } else null,
            )
        }
        if (!canPublishCatalog) {
            item {
                NoticeBanner(
                    "Modo lectura (${appRole.displayName}): solo las cuentas con rol Proveedor pueden publicar o editar productos del catálogo."
                )
            }
        }
        item {
            val allList = (proveedoresState as? UiState.Success)?.data.orEmpty()
            val myCount = allList.count { it.userId == currentUserId }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                FilterChip(
                    selected = viewMode == "explorar",
                    onClick = { viewMode = "explorar" },
                    label = { Text("Explorar catálogo (${allList.size})") },
                    modifier = Modifier.testTag("providers_mode_explore"),
                )
                if (canPublishCatalog) {
                    FilterChip(
                        selected = viewMode == "mios",
                        onClick = { viewMode = "mios" },
                        label = { Text("Mis productos ($myCount)") },
                        modifier = Modifier.testTag("providers_mode_mine"),
                    )
                }
            }
        }
        item {
            NoticeBanner(
                "Precios de referencia, disponibilidad y contacto comercial son publicados directamente por cada proveedor. Confirma inventario y entregas antes de comprar."
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    label = { Text("Buscar por proveedor, producto, categoría o zona") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    (listOf("Todos") + providerCategories).forEach { cat ->
                        FilterChip(
                            selected = categoryFilter == cat,
                            onClick = { categoryFilter = cat },
                            label = { Text(cat) },
                        )
                    }
                }
            }
        }
        item {
            StateContainer(uiState = proveedoresState) { list ->
                val filtered = list.filter { p ->
                    val matchesScope = viewMode == "explorar" || p.userId == currentUserId
                    val matchesCategory = categoryFilter == "Todos" ||
                        p.category.equals(categoryFilter, ignoreCase = true)
                    val matchesQuery = search.isBlank() || listOf(
                        p.name,
                        p.ownerName,
                        p.category,
                        p.productsServices,
                        p.priceReference,
                        p.location,
                        p.recordCode,
                        p.details,
                    ).any { it.contains(search, ignoreCase = true) }
                    matchesScope && matchesCategory && matchesQuery
                }
                if (filtered.isEmpty()) {
                    EmptyStateCard(
                        title = if (viewMode == "mios") "Aún no publicas productos o servicios" else "Sin proveedores encontrados",
                        subtitle = if (viewMode == "mios") {
                            "Registra tus insumos, herramientas o servicios para que caficultores e ingenieros los encuentren."
                        } else if (canPublishCatalog) {
                            "Publica un producto o servicio para compartirlo en el catálogo comercial."
                        } else {
                            "Aún no hay proveedores registrados que coincidan con tu búsqueda."
                        },
                        icon = Icons.Default.Inventory2,
                        actionLabel = if (canPublishCatalog) "Publicar producto" else null,
                        onAction = if (canPublishCatalog) {
                            { creating = true }
                        } else null,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        filtered.forEach { p ->
                            val isOwner = p.userId == currentUserId
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(p.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                        Surface(
                                            color = if (isOwner) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                                            shape = RoundedCornerShape(50),
                                        ) {
                                            Text(
                                                text = if (isOwner) "Mi catálogo" else "Comunidad",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            )
                                        }
                                    }
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        if (p.category.isNotBlank()) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.background,
                                                shape = RoundedCornerShape(8.dp),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                            ) {
                                                Text(
                                                    text = p.category,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.secondary,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                )
                                            }
                                        }
                                        Text(p.recordCode, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text(p.productsServices, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                                    if (p.priceReference.isNotBlank()) {
                                        Text(
                                            text = "Precio de referencia: ${p.priceReference}",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.secondary,
                                        )
                                    }
                                    if (p.ownerName.isNotBlank()) {
                                        Text(
                                            text = "Publicado por: ${p.ownerName}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    if (p.commercialContact.isNotBlank()) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary)
                                            Text(p.commercialContact, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                    if (p.location.isNotBlank()) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(p.location, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    if (p.details.isNotBlank()) {
                                        Text(p.details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    if (isOwner && canPublishCatalog) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedButton(onClick = { editing = p }) {
                                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Editar")
                                            }
                                            OutlinedButton(
                                                onClick = { deleteTarget = p },
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerClay),
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Eliminar")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val targetForm = editing ?: if (creating) ProveedorItem(category = providerCategories.first()) else null
    if (targetForm != null) {
        ProveedorFormDialog(
            initial = targetForm,
            isEdit = editing != null,
            isSaving = isSaving,
            onGeocode = onGeocode,
            onNotify = onNotify,
            onDismiss = {
                editing = null
                creating = false
            },
            onSave = { updated ->
                onSave(updated) {
                    editing = null
                    creating = false
                }
            },
        )
    }

    deleteTarget?.let { target ->
        ConfirmDeleteDialog(
            itemName = target.name,
            onConfirm = {
                onDelete(target.id)
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProveedorFormDialog(
    initial: ProveedorItem,
    isEdit: Boolean,
    isSaving: Boolean,
    onGeocode: suspend (String) -> Result<NominatimResult?>,
    onNotify: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: (ProveedorItem) -> Unit,
) {
    var name by remember(initial.id) { mutableStateOf(initial.name) }
    var category by remember(initial.id) { mutableStateOf(initial.category.ifBlank { providerCategories.first() }) }
    var products by remember(initial.id) { mutableStateOf(initial.productsServices) }
    var priceReference by remember(initial.id) { mutableStateOf(initial.priceReference) }
    var commercialContact by remember(initial.id) { mutableStateOf(initial.commercialContact) }
    var location by remember(initial.id) { mutableStateOf(initial.location) }
    var details by remember(initial.id) { mutableStateOf(initial.details) }
    var latText by remember(initial.id, initial.latitude) {
        mutableStateOf(initial.latitude?.let { String.format(Locale.US, "%.6f", it) }.orEmpty())
    }
    var lonText by remember(initial.id, initial.longitude) {
        mutableStateOf(initial.longitude?.let { String.format(Locale.US, "%.6f", it) }.orEmpty())
    }

    FormModalDialog(
        title = "${if (isEdit) "Editar" else "Publicar"} producto o proveedor",
        subtitle = "Muestra tu catálogo, precios de referencia y ubicación comercial a toda la comunidad.",
        confirmLabel = if (isEdit) "Guardar cambios" else "Publicar proveedor",
        confirmEnabled = !isSaving,
        onDismiss = onDismiss,
        onConfirm = {
            if (name.isBlank() || products.isBlank()) {
                onNotify("Nombre y productos/servicios son obligatorios.")
                return@FormModalDialog
            }
            val latTrim = latText.trim()
            val lonTrim = lonText.trim()
            if ((latTrim.isEmpty()) != (lonTrim.isEmpty())) {
                onNotify("Ingresa ambas coordenadas o deja las dos vacías.")
                return@FormModalDialog
            }
            val lat = latTrim.parseCoordinateOrNull()
            val lon = lonTrim.parseCoordinateOrNull()
            if (latTrim.isNotEmpty() && (
                    lat == null || lon == null ||
                        lat !in -90.0..90.0 || lon !in -180.0..180.0 ||
                        (lat == 0.0 && lon == 0.0)
                    )
            ) {
                onNotify("Coordenadas geográficas inválidas.")
                return@FormModalDialog
            }
            onSave(
                initial.copy(
                    name = name.trim(),
                    category = category.trim(),
                    productsServices = products.trim(),
                    priceReference = priceReference.trim(),
                    commercialContact = commercialContact.trim(),
                    location = location.trim(),
                    details = details.trim(),
                    latitude = lat,
                    longitude = lon,
                )
            )
        },
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Nombre del negocio / proveedor *") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Text("Categoría", style = MaterialTheme.typography.labelLarge)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            providerCategories.forEach { cat ->
                FilterChip(
                    selected = category == cat,
                    onClick = { category = cat },
                    label = { Text(cat) },
                )
            }
        }
        OutlinedTextField(
            value = products,
            onValueChange = { products = it },
            label = { Text("Productos o servicios ofrecidos *") },
            minLines = 2,
            maxLines = 3,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = priceReference,
            onValueChange = { priceReference = it },
            label = { Text("Precio de referencia (ej. \$135.000 / bulto 50 kg)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = commercialContact,
            onValueChange = { commercialContact = it },
            label = { Text("Contacto comercial / WhatsApp") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = location,
            onValueChange = { location = it },
            label = { Text("Municipio / dirección / zona") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = details,
            onValueChange = { details = it },
            label = { Text("Información adicional y horarios") },
            minLines = 2,
            maxLines = 4,
            modifier = Modifier.fillMaxWidth(),
        )
        LocationPickerSection(
            addressText = location,
            latitudeText = latText,
            longitudeText = lonText,
            onLatitudeChange = { latText = it },
            onLongitudeChange = { lonText = it },
            onGeocode = onGeocode,
        )
    }
}

@Composable
fun CompradorFormDialog(
    initial: CompradorItem,
    isEdit: Boolean,
    isSaving: Boolean,
    onGeocode: suspend (String) -> Result<NominatimResult?>,
    onNotify: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: (CompradorItem) -> Unit,
) {
    val isSeller = initial.actorType == "vendedor"
    var name by remember(initial.id, initial.actorType) { mutableStateOf(initial.name) }
    var purchaseType by remember(initial.id, initial.actorType) { mutableStateOf(initial.purchaseType) }
    var offerText by remember(initial.id, initial.actorType) {
        mutableStateOf(if (initial.offerAmountCop > 0.0) initial.offerAmountCop.toLong().toString() else "")
    }
    var priceUnit by remember(initial.id, initial.actorType) {
        mutableStateOf(initial.priceUnit.ifBlank { "kg pergamino seco" })
    }
    var priceDate by remember(initial.id, initial.actorType) {
        mutableStateOf(initial.priceDate.ifBlank { todayIsoDate() })
    }
    var location by remember(initial.id, initial.actorType) { mutableStateOf(initial.location) }
    var details by remember(initial.id, initial.actorType) { mutableStateOf(initial.details) }
    var latText by remember(initial.id, initial.latitude) {
        mutableStateOf(initial.latitude?.let { String.format(Locale.US, "%.6f", it) }.orEmpty())
    }
    var lonText by remember(initial.id, initial.longitude) {
        mutableStateOf(initial.longitude?.let { String.format(Locale.US, "%.6f", it) }.orEmpty())
    }

    FormModalDialog(
        title = "${if (isEdit) "Editar" else "Publicar"} ${if (isSeller) "vendedor" else "precio de compra"}",
        subtitle = "Publica tu punto, tipo de café y precio diario de compra para comparar en el módulo de Precios.",
        confirmLabel = if (isEdit) "Guardar cambios" else "Publicar precio y punto",
        confirmEnabled = !isSaving,
        onDismiss = onDismiss,
        onConfirm = {
            if (name.isBlank()) {
                onNotify("El nombre es obligatorio.")
                return@FormModalDialog
            }
            val latTrim = latText.trim()
            val lonTrim = lonText.trim()
            if ((latTrim.isEmpty()) != (lonTrim.isEmpty())) {
                onNotify("Ingresa ambas coordenadas o deja las dos vacías.")
                return@FormModalDialog
            }
            val lat = latTrim.parseCoordinateOrNull()
            val lon = lonTrim.parseCoordinateOrNull()
            if (latTrim.isNotEmpty() && (
                    lat == null || lon == null ||
                        lat !in -90.0..90.0 || lon !in -180.0..180.0 ||
                        (lat == 0.0 && lon == 0.0)
                    )
            ) {
                onNotify("Coordenadas geográficas inválidas.")
                return@FormModalDialog
            }
            onSave(
                initial.copy(
                    name = name.trim(),
                    purchaseType = purchaseType.trim(),
                    offerAmountCop = offerText.trim().replace(',', '.').toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0,
                    priceUnit = priceUnit.trim().ifBlank { "kg pergamino seco" },
                    priceDate = priceDate.trim().ifBlank { todayIsoDate() },
                    location = location.trim(),
                    details = details.trim(),
                    latitude = lat,
                    longitude = lon,
                )
            )
        },
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(if (isSeller) "Nombre del vendedor *" else "Nombre del comprador / agencia *") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = purchaseType,
            onValueChange = { purchaseType = it },
            label = { Text(if (isSeller) "Productos ofrecidos" else "Tipo de compra (ej. Pergamino seco)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = offerText,
                onValueChange = { offerText = it },
                label = { Text("Precio diario (COP)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = priceUnit,
                onValueChange = { priceUnit = it },
                label = { Text("Unidad (ej. kg / carga)") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }
        OutlinedTextField(
            value = priceDate,
            onValueChange = { priceDate = it },
            label = { Text("Fecha de vigencia (YYYY-MM-DD)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = location,
            onValueChange = { location = it },
            label = { Text("Dirección o vereda") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = details,
            onValueChange = { details = it },
            label = { Text("Condiciones, factor de rendimiento o transporte") },
            minLines = 2,
            maxLines = 4,
            modifier = Modifier.fillMaxWidth(),
        )
        LocationPickerSection(
            addressText = location,
            latitudeText = latText,
            longitudeText = lonText,
            onLatitudeChange = { latText = it },
            onLongitudeChange = { lonText = it },
            onGeocode = onGeocode,
        )
    }
}
