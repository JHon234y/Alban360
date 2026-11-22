package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.annotation.DrawableRes
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.NominatimResult
import com.example.model.AiUiState
import com.example.model.FincaProfile
import com.example.model.UiState
import com.example.ui.theme.DangerClay
import com.example.ui.theme.NoticeBorder
import com.example.ui.theme.NoticeSand
import com.example.ui.theme.NoticeText
import com.example.ui.theme.PinFinca
import com.example.util.OsmLayerMode
import com.example.util.applyOsmTileSource
import com.example.util.configureOsmDroidCache
import com.example.util.createColoredMapPinDrawable
import com.example.util.fetchCurrentLocation
import com.example.util.generateQrBitmap
import com.example.util.shareText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import java.util.Locale

@Composable
fun SectionHeader(
    kicker: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = kicker.uppercase(Locale.getDefault()),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (action != null) {
            Spacer(Modifier.height(12.dp))
            action()
        }
    }
}

@Composable
fun NoticeBanner(
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = NoticeSand,
        border = BorderStroke(1.dp, NoticeBorder),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = NoticeText,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = NoticeText,
            )
        }
    }
}

@Composable
fun EmptyStateCard(
    title: String,
    subtitle: String,
    icon: ImageVector = Icons.Default.Inbox,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(14.dp))
                Button(onClick = onAction) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
fun <T> StateContainer(
    uiState: UiState<T>,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit,
) {
    when (uiState) {
        is UiState.Loading -> {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
        is UiState.Error -> {
            Card(
                modifier = modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    Text(
                        text = uiState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        }
        is UiState.Success -> content(uiState.data)
    }
}

@Composable
fun QrCodeImage(
    content: String,
    contentDescription: String,
    size: Dp = 160.dp,
    modifier: Modifier = Modifier,
) {
    val bitmapState by produceState<ImageBitmap?>(initialValue = null, key1 = content) {
        value = withContext(Dispatchers.Default) {
            generateQrBitmap(content, 384)
        }
    }
    val bitmap = bitmapState
    if (bitmap != null) {
        Surface(
            modifier = modifier.size(size),
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
            Image(
                bitmap = bitmap,
                contentDescription = contentDescription,
                modifier = Modifier.padding(8.dp),
            )
        }
    } else {
        Box(
            modifier = modifier.size(size),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        }
    }
}

private object DrawableBitmapCache {
    private val cache = object : LruCache<Long, ImageBitmap>(12) {}

    private fun key(@DrawableRes resId: Int, maxDimPx: Int): Long =
        (resId.toLong() shl 32) or (maxDimPx.toLong() and 0xFFFFFFFFL)

    fun get(@DrawableRes resId: Int, maxDimPx: Int): ImageBitmap? =
        cache.get(key(resId, maxDimPx))

    fun put(@DrawableRes resId: Int, maxDimPx: Int, bitmap: ImageBitmap) {
        cache.put(key(resId, maxDimPx), bitmap)
    }

    fun decodeSampled(context: Context, @DrawableRes resId: Int, maxDimPx: Int): ImageBitmap? {
        get(resId, maxDimPx)?.let { return it }
        val res = context.resources
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeResource(res, resId, bounds)
        var sampleSize = 1
        val maxSide = maxOf(bounds.outWidth, bounds.outHeight)
        if (maxSide > maxDimPx && maxDimPx > 0) {
            while (maxSide / (sampleSize * 2) >= maxDimPx) {
                sampleSize *= 2
            }
        }
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
        }
        val decoded = BitmapFactory.decodeResource(res, resId, opts)?.asImageBitmap()
        if (decoded != null) {
            put(resId, maxDimPx, decoded)
        }
        return decoded
    }
}

@Composable
fun AsyncDrawableImage(
    @DrawableRes resId: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    maxDimPx: Int = 900,
    placeholderColor: Color = MaterialTheme.colorScheme.surfaceVariant,
) {
    val context = LocalContext.current
    val cached = remember(resId, maxDimPx) { DrawableBitmapCache.get(resId, maxDimPx) }
    val bitmap by produceState(initialValue = cached, resId, maxDimPx) {
        if (value == null) {
            value = withContext(Dispatchers.IO) {
                runCatching { DrawableBitmapCache.decodeSampled(context.applicationContext, resId, maxDimPx) }
                    .getOrNull()
            }
        }
    }
    val ready = bitmap
    if (ready != null) {
        Image(
            bitmap = ready,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier,
        )
    } else {
        Box(modifier = modifier.background(placeholderColor))
    }
}

@Composable
fun QrPreviewDialog(
    title: String,
    code: String,
    subtitle: String,
    shareUrl: String,
    onCopyMessage: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Código QR · $title") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                QrCodeImage(
                    content = shareUrl,
                    contentDescription = "Código QR de $title",
                    size = 210.dp,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = code,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                )
                if (subtitle.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(shareUrl))
                        onCopyMessage("Enlace de trazabilidad copiado.")
                    }
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Copiar")
                }
                Button(
                    onClick = {
                        shareText(context, "Perfil Albán Café 360°", "$title ($code)\n$shareUrl")
                    }
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Compartir")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FarmDropdownField(
    farms: List<FincaProfile>,
    selectedCode: String,
    label: String = "Finca o caficultor",
    allowEmpty: Boolean = false,
    onSelected: (code: String, name: String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedProfile = farms.firstOrNull {
        it.recordCode == selectedCode || it.id == selectedCode
    }
    val displayText = when {
        selectedProfile != null -> buildString {
            append(selectedProfile.name)
            if (selectedProfile.ownerName.isNotBlank()) append(" · ${selectedProfile.ownerName}")
            append(" (${selectedProfile.recordCode.ifBlank { selectedProfile.id }})")
        }
        selectedCode.isNotBlank() -> selectedCode
        allowEmpty -> "Sin finca específica"
        else -> "Selecciona una finca o caficultor"
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
    ) {
        OutlinedTextField(
            value = displayText,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            if (allowEmpty) {
                DropdownMenuItem(
                    text = { Text("Sin finca específica") },
                    onClick = {
                        onSelected("", "")
                        expanded = false
                    },
                )
            }
            farms.forEach { profile ->
                val code = profile.recordCode.ifBlank { profile.id }
                val labelText = buildString {
                    append(profile.name)
                    if (profile.ownerName.isNotBlank()) append(" · ${profile.ownerName}")
                    append(" ($code)")
                }
                DropdownMenuItem(
                    text = { Text(labelText) },
                    onClick = {
                        onSelected(code, profile.name)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
fun LocationPickerSection(
    addressText: String,
    latitudeText: String,
    longitudeText: String,
    onLatitudeChange: (String) -> Unit,
    onLongitudeChange: (String) -> Unit,
    onGeocode: suspend (String) -> Result<NominatimResult?>,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var geoStatus by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var showMapPicker by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            geoStatus = "Obteniendo GPS..."
            scope.launch {
                val coords = fetchCurrentLocation(context)
                if (coords != null) {
                    onLatitudeChange(String.format(Locale.US, "%.6f", coords.latitude))
                    onLongitudeChange(String.format(Locale.US, "%.6f", coords.longitude))
                    geoStatus = "Ubicación GPS obtenida. Verifica las coordenadas."
                } else {
                    geoStatus = "No se pudo leer el GPS. Activa la ubicación o elige en el mapa."
                }
            }
        } else {
            geoStatus = "Permiso de ubicación denegado. Puedes buscar dirección o elegir en el mapa."
        }
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Ubicación geográfica (opcional)",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Busca la vereda, usa tu GPS o toca un punto en el mapa satelital.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = latitudeText,
                    onValueChange = onLatitudeChange,
                    label = { Text("Latitud") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = longitudeText,
                    onValueChange = onLongitudeChange,
                    label = { Text("Longitud") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        if (addressText.isBlank()) {
                            geoStatus = "Escribe primero una dirección o vereda arriba."
                            return@OutlinedButton
                        }
                        searching = true
                        geoStatus = "Buscando en Nariño..."
                        scope.launch {
                            val res = onGeocode(addressText)
                            searching = false
                            res.onSuccess { found ->
                                if (found != null) {
                                    onLatitudeChange(found.lat)
                                    onLongitudeChange(found.lon)
                                    geoStatus = "Encontrado: ${found.displayName}"
                                } else {
                                    geoStatus = "No encontramos esa vereda. Usa el mapa o GPS."
                                }
                            }.onFailure {
                                geoStatus = "Sin conexión para geocodificar. Usa el mapa o GPS."
                            }
                        }
                    },
                    enabled = !searching,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Buscar", maxLines = 1)
                }
                OutlinedButton(
                    onClick = {
                        val fine = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                        val coarse = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                        if (fine || coarse) {
                            geoStatus = "Obteniendo GPS..."
                            scope.launch {
                                val coords = fetchCurrentLocation(context)
                                if (coords != null) {
                                    onLatitudeChange(String.format(Locale.US, "%.6f", coords.latitude))
                                    onLongitudeChange(String.format(Locale.US, "%.6f", coords.longitude))
                                    geoStatus = "Ubicación GPS obtenida."
                                } else {
                                    geoStatus = "No se obtuvo señal GPS. Prueba elegir en el mapa."
                                }
                            }
                        } else {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                )
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("GPS", maxLines = 1)
                }
            }
            OutlinedButton(
                onClick = { showMapPicker = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Elegir punto tocando el mapa")
            }
            if (geoStatus.isNotBlank()) {
                Text(
                    text = geoStatus,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }

    if (showMapPicker) {
        val initLat = latitudeText.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it in -90.0..90.0 && it != 0.0 } ?: 1.473
        val initLon = longitudeText.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it in -180.0..180.0 && it != 0.0 } ?: -77.07
        MapCoordinatePickerDialog(
            initialLat = initLat,
            initialLon = initLon,
            onPicked = { lat, lon ->
                onLatitudeChange(String.format(Locale.US, "%.6f", lat))
                onLongitudeChange(String.format(Locale.US, "%.6f", lon))
                geoStatus = "Punto fijado manualmente en el mapa."
                showMapPicker = false
            },
            onDismiss = { showMapPicker = false },
        )
    }
}

@Composable
fun MapCoordinatePickerDialog(
    initialLat: Double,
    initialLon: Double,
    onPicked: (Double, Double) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    var pickedLat by remember { mutableStateOf(initialLat) }
    var pickedLon by remember { mutableStateOf(initialLon) }
    var layerMode by remember { mutableStateOf(OsmLayerMode.SATELLITE) }
    var gpsStatus by remember { mutableStateOf("") }

    val pinDrawable = remember(context) {
        createColoredMapPinDrawable(context, PinFinca.toArgb())
    }

    val mapView = remember {
        configureOsmDroidCache(context)
        MapView(context).apply {
            applyOsmTileSource(this, OsmLayerMode.SATELLITE)
            setMultiTouchControls(true)
            setBuiltInZoomControls(false)
            controller.setZoom(15.5)
            controller.setCenter(GeoPoint(initialLat, initialLon))
        }
    }

    val pickerGpsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            gpsStatus = "Obteniendo GPS..."
            scope.launch {
                val coords = fetchCurrentLocation(context)
                if (coords != null) {
                    pickedLat = coords.latitude
                    pickedLon = coords.longitude
                    mapView.controller.animateTo(GeoPoint(coords.latitude, coords.longitude), 16.5, 600L)
                    gpsStatus = "GPS capturado en el mapa."
                } else {
                    gpsStatus = "Sin señal GPS. Toca directamente el terreno."
                }
            }
        } else {
            gpsStatus = "Permiso GPS denegado. Toca el mapa para marcar."
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

    LaunchedEffect(layerMode, pickedLat, pickedLon) {
        applyOsmTileSource(mapView, layerMode)
        mapView.overlays.clear()
        val marker = Marker(mapView).apply {
            position = GeoPoint(pickedLat, pickedLon)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            icon = pinDrawable
            title = "Ubicación seleccionada"
        }
        val eventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                pickedLat = p.latitude
                pickedLon = p.longitude
                gpsStatus = "Punto marcado manualmente."
                return true
            }

            override fun longPressHelper(p: GeoPoint): Boolean = false
        })
        mapView.overlays.add(eventsOverlay)
        mapView.overlays.add(marker)
        mapView.invalidate()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Toca el mapa o usa tu GPS") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    OsmLayerMode.entries.forEach { mode ->
                        FilterChip(
                            selected = layerMode == mode,
                            onClick = { layerMode = mode },
                            label = { Text(mode.label, maxLines = 1) },
                        )
                    }
                }
                Text(
                    text = String.format(Locale.US, "Lat: %.6f · Lon: %.6f", pickedLat, pickedLon),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (gpsStatus.isNotBlank()) {
                    Text(
                        text = gpsStatus,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(RoundedCornerShape(12.dp)),
                ) {
                    AndroidView(
                        factory = { mapView },
                        modifier = Modifier.matchParentSize(),
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
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
                                    gpsStatus = "Obteniendo GPS..."
                                    scope.launch {
                                        val coords = fetchCurrentLocation(context)
                                        if (coords != null) {
                                            pickedLat = coords.latitude
                                            pickedLon = coords.longitude
                                            mapView.controller.animateTo(
                                                GeoPoint(coords.latitude, coords.longitude),
                                                16.5,
                                                600L,
                                            )
                                            gpsStatus = "GPS capturado en el mapa."
                                        } else {
                                            gpsStatus = "Sin señal GPS. Toca directamente el terreno."
                                        }
                                    }
                                } else {
                                    pickerGpsLauncher.launch(
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
                            modifier = Modifier.size(36.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.MyLocation,
                                    contentDescription = "Usar mi GPS",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                        Surface(
                            onClick = { mapView.controller.zoomIn() },
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.size(36.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Add, contentDescription = "Acercar", modifier = Modifier.size(18.dp))
                            }
                        }
                        Surface(
                            onClick = { mapView.controller.zoomOut() },
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.size(36.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Remove, contentDescription = "Alejar", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onPicked(pickedLat, pickedLon) }) {
                Text("Usar estas coordenadas")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}

@Composable
fun AiAdvisoryDialog(
    title: String,
    aiState: AiUiState,
    onDismiss: () -> Unit,
) {
    if (aiState is AiUiState.Idle) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                )
                Text(title)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                when (aiState) {
                    is AiUiState.Loading -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                            Text("Analizando información cafetera...", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    is AiUiState.Streaming -> {
                        Text(aiState.text, style = MaterialTheme.typography.bodyMedium)
                    }
                    is AiUiState.Success -> {
                        Text(aiState.text, style = MaterialTheme.typography.bodyMedium)
                    }
                    is AiUiState.Error -> {
                        Text(
                            text = aiState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    AiUiState.Idle -> Unit
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Entendido")
            }
        },
    )
}

@Composable
fun ConfirmDeleteDialog(
    itemName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("¿Eliminar registro?") },
        text = {
            Text("Se eliminará \"$itemName\". Esta acción no se puede deshacer.")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = DangerClay),
                modifier = Modifier.testTag("confirm_delete_button"),
            ) {
                Text("Sí, eliminar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}

@Composable
fun FormModalDialog(
    title: String,
    subtitle: String? = null,
    onDismiss: () -> Unit,
    confirmLabel: String = "Guardar",
    confirmEnabled: Boolean = true,
    onConfirm: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(title, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                content()
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = confirmEnabled,
                modifier = Modifier.testTag("modal_save_button"),
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}
