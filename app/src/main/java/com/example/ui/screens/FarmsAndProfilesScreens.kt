package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.TrendingUp
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.NominatimResult
import com.example.model.AppRole
import com.example.model.CompradorItem
import com.example.model.CultivoActivity
import com.example.model.FincaProfile
import com.example.model.InquietudTecnica
import com.example.model.ProveedorItem
import com.example.model.UiState
import com.example.model.VisitaAgronomica
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FormModalDialog
import com.example.ui.components.LocationPickerSection
import com.example.ui.components.NoticeBanner
import com.example.ui.components.QrCodeImage
import com.example.ui.components.QrPreviewDialog
import com.example.ui.components.SectionHeader
import com.example.ui.components.StateContainer
import com.example.ui.theme.DangerClay
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.ForestGreenLight
import com.example.ui.theme.SagePale
import com.example.ui.theme.WarmGold
import com.example.util.formatCop
import com.example.util.formatNumber
import com.example.util.parseCoordinateOrNull
import com.example.util.profileShareUrl
import com.example.util.shareText
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FincasScreen(
    currentUserId: String,
    appRole: AppRole,
    fincasState: UiState<List<FincaProfile>>,
    isSaving: Boolean,
    onSave: (FincaProfile, () -> Unit) -> Unit,
    onDelete: (String) -> Unit,
    onOpenProfile: (String) -> Unit,
    onOpenInMap: (Double, Double) -> Unit,
    onRegisterVisitForFarm: (String) -> Unit,
    onGeocode: suspend (String) -> Result<NominatimResult?>,
    onNotify: (String) -> Unit,
) {
    val canManageOwnFarms = appRole.capabilities.manageOwnFarms
    val canRegisterVisit = appRole.capabilities.registerAgronomyVisits

    var search by remember { mutableStateOf("") }
    var scopeTab by remember(appRole) {
        mutableStateOf(if (canManageOwnFarms) "mias" else "otras")
    }
    var filter by remember { mutableStateOf("todos") }
    var editing by remember { mutableStateOf<FincaProfile?>(null) }
    var creatingType by remember { mutableStateOf<String?>(null) }
    var qrTarget by remember { mutableStateOf<FincaProfile?>(null) }
    var deleteTarget by remember { mutableStateOf<FincaProfile?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            SectionHeader(
                kicker = "GESTIÓN CAFETERA",
                title = "Fincas y caficultores",
                subtitle = if (canManageOwnFarms) {
                    "Administra tus fincas con código QR o explora los perfiles trazables de la comunidad."
                } else {
                    "Consulta fichas, manejo y códigos QR de las fincas cafeteras en modo comunidad."
                },
                action = if (canManageOwnFarms) {
                    {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { creatingType = "finca" },
                                modifier = Modifier.testTag("add_finca_button"),
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Nueva finca")
                            }
                            OutlinedButton(
                                onClick = { creatingType = "caficultor" },
                                modifier = Modifier.testTag("add_caficultor_button"),
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Caficultor")
                            }
                        }
                    }
                } else null,
            )
        }

        if (!canManageOwnFarms) {
            item {
                NoticeBanner(
                    if (canRegisterVisit) {
                        "Enfoque Ingeniero Agrónomo: consulta cualquier finca en modo lectura y pulsa 'Registrar visita' para dejar observaciones técnicas."
                    } else {
                        "Enfoque ${appRole.displayName}: visualiza las fincas, su ubicación y trazabilidad QR en modo solo lectura."
                    }
                )
            }
        }

        item {
            val allList = (fincasState as? UiState.Success)?.data.orEmpty()
            val mineCount = allList.count { it.userId == currentUserId }
            val communityCount = if (canManageOwnFarms) allList.count { it.userId != currentUserId } else allList.size
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (canManageOwnFarms) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        FilterChip(
                            selected = scopeTab == "mias",
                            onClick = { scopeTab = "mias" },
                            label = { Text("Mis fincas (Editar) · $mineCount") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ForestGreen,
                                selectedLabelColor = Color.White,
                            ),
                            modifier = Modifier.testTag("fincas_tab_mias"),
                        )
                        FilterChip(
                            selected = scopeTab == "otras",
                            onClick = { scopeTab = "otras" },
                            label = { Text("Otras fincas (Comunidad) · $communityCount") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ForestGreen,
                                selectedLabelColor = Color.White,
                            ),
                            modifier = Modifier.testTag("fincas_tab_otras"),
                        )
                    }
                }
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    label = { Text("Buscar por nombre, propietario, ubicación o código") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("todos" to "Todos", "finca" to "Fincas", "caficultor" to "Caficultores").forEach { (key, label) ->
                        FilterChip(
                            selected = filter == key,
                            onClick = { filter = key },
                            label = { Text(label) },
                        )
                    }
                }
            }
        }

        item {
            StateContainer(uiState = fincasState) { list ->
                val scopedList = when {
                    !canManageOwnFarms -> list
                    scopeTab == "mias" -> list.filter { it.userId == currentUserId }
                    else -> list.filter { it.userId != currentUserId }
                }
                val filtered = scopedList.filter { item ->
                    (filter == "todos" || item.recordType == filter) &&
                        (search.isBlank() || listOf(
                            item.name,
                            item.ownerName,
                            item.subtitle,
                            item.location,
                            item.recordCode,
                            item.relatedCode,
                            item.details,
                        ).any { it.contains(search, ignoreCase = true) })
                }

                if (filtered.isEmpty()) {
                    val isOwnTab = canManageOwnFarms && scopeTab == "mias"
                    EmptyStateCard(
                        title = if (isOwnTab) "Aún no tienes fincas propias registradas" else "Sin fincas registradas en la comunidad aún",
                        subtitle = if (isOwnTab) {
                            "Registra tu finca o perfil para editar su manejo, bitácora y código QR."
                        } else {
                            "Cuando los caficultores registren sus fincas aparecerán aquí para consulta comunitaria."
                        },
                        icon = Icons.Default.Landscape,
                        actionLabel = if (isOwnTab) "Registrar mi finca" else null,
                        onAction = if (isOwnTab) ({ creatingType = "finca" }) else null,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        filtered.forEach { profile ->
                            val isOwner = canManageOwnFarms && profile.userId == currentUserId
                            val code = profile.recordCode.ifBlank { profile.id }
                            FincaProfileCard(
                                profile = profile,
                                isOwner = isOwner,
                                onOpenProfile = { onOpenProfile(code) },
                                onOpenInMap = if (profile.recordType == "finca" && profile.hasValidCoordinates) {
                                    { onOpenInMap(profile.latitude!!, profile.longitude!!) }
                                } else {
                                    null
                                },
                                onRegisterVisit = if (canRegisterVisit && profile.recordType == "finca") {
                                    { onRegisterVisitForFarm(code) }
                                } else {
                                    null
                                },
                                onShowQr = { qrTarget = profile },
                                onEdit = { editing = profile },
                                onDelete = { deleteTarget = profile },
                            )
                        }
                    }
                }
            }
        }
    }

    val formProfile = editing ?: creatingType?.let { FincaProfile(recordType = it) }
    if (formProfile != null) {
        FincaFormDialog(
            initial = formProfile,
            isEdit = editing != null,
            isSaving = isSaving,
            onGeocode = onGeocode,
            onNotify = onNotify,
            onDismiss = {
                editing = null
                creatingType = null
            },
            onSave = { updated ->
                onSave(updated) {
                    editing = null
                    creatingType = null
                }
            },
        )
    }

    qrTarget?.let { target ->
        val code = target.recordCode.ifBlank { target.id }
        QrPreviewDialog(
            title = target.name,
            code = code,
            subtitle = target.location,
            shareUrl = profileShareUrl(code),
            onCopyMessage = onNotify,
            onDismiss = { qrTarget = null },
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
fun FincaProfileCard(
    profile: FincaProfile,
    isOwner: Boolean,
    onOpenProfile: () -> Unit,
    onOpenInMap: (() -> Unit)? = null,
    onRegisterVisit: (() -> Unit)? = null,
    onShowQr: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val code = profile.recordCode.ifBlank { profile.id }
    val shareUrl = profileShareUrl(code)
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (profile.recordType == "finca") Icons.Default.Landscape else Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Column {
                        Text(profile.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = buildString {
                                append(if (profile.recordType == "finca") "Finca · $code" else "Caficultor · $code")
                                if (profile.ownerName.isNotBlank()) append(" · ${profile.ownerName}")
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isOwner) SagePale else MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Text(
                        text = if (isOwner) "Propia" else "Comunidad",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isOwner) ForestGreen else MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }

            if (profile.subtitle.isNotBlank()) {
                Text(profile.subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(profile.location, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (profile.relatedCode.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Vinculado: ${profile.relatedCode}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (profile.recordType == "finca" && profile.areaHectares > 0.0) {
                    Text(
                        text = "Área cultivada: ${formatNumber(profile.areaHectares)} ha",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                if (profile.recordType == "finca" && profile.hasValidCoordinates) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(
                            Icons.Default.MyLocation,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = ForestGreen,
                        )
                        Text(
                            text = String.format(
                                Locale.US,
                                "Pin en mapa: %.5f, %.5f",
                                profile.latitude!!,
                                profile.longitude!!,
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = ForestGreen,
                        )
                    }
                }
            }

            if (profile.recordType == "finca") {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.background,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        QrCodeImage(
                            content = shareUrl,
                            contentDescription = "QR para abrir el perfil de ${profile.name}",
                            size = 76.dp,
                        )
                        Column {
                            Text("Escanea y conoce esta finca", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            Text("Abre su ficha pública, bitácora y visitas de agrónomos.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(onClick = onOpenProfile) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Ver ficha y manejo")
                }
                if (onRegisterVisit != null) {
                    OutlinedButton(onClick = onRegisterVisit) {
                        Icon(Icons.Default.Eco, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Registrar visita")
                    }
                }
                if (onOpenInMap != null) {
                    OutlinedButton(onClick = onOpenInMap) {
                        Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Mapa")
                    }
                }
                OutlinedButton(onClick = onShowQr) {
                    Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("QR")
                }
                if (isOwner) {
                    OutlinedButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Editar")
                    }
                    OutlinedButton(
                        onClick = onDelete,
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

@Composable
fun FincaFormDialog(
    initial: FincaProfile,
    isEdit: Boolean,
    isSaving: Boolean,
    onGeocode: suspend (String) -> Result<NominatimResult?>,
    onNotify: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: (FincaProfile) -> Unit,
) {
    val isFinca = initial.recordType == "finca"
    var name by remember(initial.id, initial.recordType) { mutableStateOf(initial.name) }
    var subtitle by remember(initial.id, initial.recordType) { mutableStateOf(initial.subtitle) }
    var location by remember(initial.id, initial.recordType) { mutableStateOf(initial.location) }
    var relatedCode by remember(initial.id, initial.recordType) { mutableStateOf(initial.relatedCode) }
    var areaText by remember(initial.id, initial.recordType) {
        mutableStateOf(if (initial.areaHectares > 0.0) initial.areaHectares.toString() else "")
    }
    var details by remember(initial.id, initial.recordType) { mutableStateOf(initial.details) }
    var latText by remember(initial.id, initial.latitude) {
        mutableStateOf(initial.latitude?.let { String.format(Locale.US, "%.6f", it) }.orEmpty())
    }
    var lonText by remember(initial.id, initial.longitude) {
        mutableStateOf(initial.longitude?.let { String.format(Locale.US, "%.6f", it) }.orEmpty())
    }

    FormModalDialog(
        title = "${if (isEdit) "Editar" else "Registrar"} ${if (isFinca) "finca" else "caficultor"}",
        subtitle = "Completa la información para mantener tu trazabilidad al día.",
        confirmLabel = if (isEdit) "Guardar cambios" else "Crear registro",
        confirmEnabled = !isSaving,
        onDismiss = onDismiss,
        onConfirm = {
            if (name.isBlank() || location.isBlank()) {
                onNotify("Nombre y ubicación son obligatorios.")
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
                    subtitle = subtitle.trim(),
                    location = location.trim(),
                    relatedCode = relatedCode.trim(),
                    areaHectares = areaText.trim().replace(',', '.').toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0,
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
            label = { Text(if (isFinca) "Nombre de la finca *" else "Nombre del caficultor *") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = subtitle,
            onValueChange = { subtitle = it },
            label = { Text(if (isFinca) "Variedad o descripción" else "Especialidad o descripción") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = location,
            onValueChange = { location = it },
            label = { Text(if (isFinca) "Dirección o vereda *" else "Municipio / departamento *") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = relatedCode,
            onValueChange = { relatedCode = it },
            label = { Text(if (isFinca) "Código del caficultor vinculado" else "Código de finca vinculada") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        if (isFinca) {
            OutlinedTextField(
                value = areaText,
                onValueChange = { areaText = it },
                label = { Text("Área cultivada (hectáreas)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        OutlinedTextField(
            value = details,
            onValueChange = { details = it },
            label = { Text(if (isFinca) "Notas de la finca" else "Notas del perfil") },
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileDetailScreen(
    codeOrId: String,
    currentUserId: String,
    appRole: AppRole,
    fincasState: UiState<List<FincaProfile>>,
    cultivosState: UiState<List<CultivoActivity>>,
    visitasState: UiState<List<VisitaAgronomica>>,
    inquietudesState: UiState<List<InquietudTecnica>>,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    onRegisterVisit: (String) -> Unit,
    onNotify: (String) -> Unit,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val fincas = (fincasState as? UiState.Success)?.data.orEmpty()
    val cultivos = (cultivosState as? UiState.Success)?.data.orEmpty()
    val visitas = (visitasState as? UiState.Success)?.data.orEmpty()
    val inquietudes = (inquietudesState as? UiState.Success)?.data.orEmpty()

    val profile = fincas.firstOrNull {
        it.recordCode == codeOrId || it.id == codeOrId
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (profile == null) {
            item {
                EmptyStateCard(
                    title = "Perfil no encontrado",
                    subtitle = "No encontramos el perfil $codeOrId. Consulta las fincas registradas.",
                    actionLabel = "Volver a fincas",
                    onAction = onBack,
                )
            }
            return@LazyColumn
        }

        val code = profile.recordCode.ifBlank { profile.id }
        val isOwner = appRole.capabilities.manageOwnFarms && profile.userId == currentUserId
        val canManageCropLog = appRole.capabilities.logOwnCropActivities && profile.userId == currentUserId
        val canRegisterVisit = appRole.capabilities.registerAgronomyVisits
        val shareUrl = profileShareUrl(code)
        val linkedActivities = cultivos.filter {
            (it.farmId.isNotBlank() && it.farmId == profile.id) ||
                it.farmCode == code ||
                it.farmCode == profile.id
        }
        val linkedVisitas = visitas.filter {
            (it.farmId.isNotBlank() && it.farmId == profile.id) ||
                it.farmCode == code ||
                it.farmCode == profile.id
        }
        val linkedInquietudes = inquietudes.filter {
            (it.farmId.isNotBlank() && it.farmId == profile.id) ||
                it.farmCode == code ||
                it.farmCode == profile.id
        }

        item {
            SectionHeader(
                kicker = "TRAZABILIDAD CAFETERA",
                title = profile.name,
                subtitle = if (isOwner) "Tu finca (Editable) · $code" else "Finca comunitaria (Solo lectura) · $code",
                action = {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Volver a fincas")
                        }
                        if (canRegisterVisit && profile.recordType == "finca") {
                            Button(onClick = { onRegisterVisit(code) }) {
                                Icon(Icons.Default.Eco, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Registrar visita")
                            }
                        }
                    }
                },
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (profile.recordType == "finca") "Datos de la finca" else "Datos del caficultor",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if (profile.ownerName.isNotBlank()) DetailLine("Propietario / caficultor", profile.ownerName)
                    DetailLine("Ubicación", profile.location)
                    DetailLine("Descripción", profile.subtitle.ifBlank { "Sin descripción" })
                    DetailLine("Código", code)
                    if (profile.relatedCode.isNotBlank()) DetailLine("Perfil vinculado", profile.relatedCode)
                    if (profile.recordType == "finca" && profile.areaHectares > 0.0) {
                        DetailLine("Área cultivada", "${formatNumber(profile.areaHectares)} hectáreas")
                    }
                    DetailLine("Información adicional", profile.details.ifBlank { "Aún no se han añadido notas." })
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Bitácora y manejo del cultivo", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                    if (linkedActivities.isEmpty()) {
                        Text("Aún no hay actividades de cultivo asociadas a esta finca.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        linkedActivities.forEach { act ->
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text("${act.activityType} · ${act.name}", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                                    Text(act.date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (act.authorName.isNotBlank()) {
                                    Text("Registrado por: ${act.authorName}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                }
                                if (act.details.isNotBlank()) {
                                    Text(act.details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onNavigate("cultivo") }) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (canManageCropLog) "Gestionar bitácora" else "Ver bitácoras")
                        }
                        OutlinedButton(onClick = { onNavigate("precios") }) {
                            Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Precios")
                        }
                    }
                }
            }
        }

        // Visitas y comentarios de ingenieros agrónomos
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Visitas y observaciones agronómicas",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "${linkedVisitas.size}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                    if (linkedVisitas.isEmpty()) {
                        Text(
                            text = "Aún no hay visitas de ingenieros agrónomos registradas para esta finca.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        linkedVisitas.forEach { v ->
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text("Ing. ${v.engineerName}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                                    Text(v.date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text("Observación: ${v.diagnosis}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                if (v.activitiesPerformed.isNotBlank()) {
                                    Text("Qué se hizo: ${v.activitiesPerformed}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                }
                                if (v.recommendations.isNotBlank()) {
                                    Text("Comentarios / recomendaciones: ${v.recommendations}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    if (canRegisterVisit && profile.recordType == "finca") {
                        OutlinedButton(onClick = { onRegisterVisit(code) }) {
                            Icon(Icons.Default.Eco, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Registrar visita a esta finca")
                        }
                    }
                }
            }
        }

        // Inquietudes técnicas asociadas
        if (linkedInquietudes.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Inquietudes técnicas de la finca", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                        linkedInquietudes.forEach { inq ->
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(inq.subject, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                                    Text(
                                        text = if (inq.status == "respondida") "Respondida" else "Abierta",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (inq.status == "respondida") ForestGreen else MaterialTheme.colorScheme.secondary,
                                    )
                                }
                                Text(inq.question, style = MaterialTheme.typography.bodySmall)
                                if (inq.replyText.isNotBlank()) {
                                    Text(
                                        text = "Respuesta (${inq.replyByName}): ${inq.replyText}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ForestGreen,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("Comparte este perfil", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                    QrCodeImage(
                        content = shareUrl,
                        contentDescription = "Código QR del perfil de ${profile.name}",
                        size = 200.dp,
                    )
                    Text(
                        text = "Escanea el QR para abrir directamente esta ficha de origen.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(shareUrl))
                                onNotify("Enlace copiado.")
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Copiar enlace")
                        }
                        Button(
                            onClick = { shareText(context, "Perfil ${profile.name}", "${profile.name} ($code)\n$shareUrl") }
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Compartir")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RoleProfileScreen(
    role: String, // "perfil-caficultor" | "perfil-ingeniero" | "perfil-proveedor" | "perfil-comprador"
    activeAppRole: AppRole,
    fincasState: UiState<List<FincaProfile>>,
    visitasState: UiState<List<VisitaAgronomica>>,
    proveedoresState: UiState<List<ProveedorItem>>,
    compradoresState: UiState<List<CompradorItem>>,
    onNavigate: (String) -> Unit,
    onOpenProfile: (String) -> Unit,
) {
    val targetAppRole = when (role) {
        "perfil-ingeniero" -> AppRole.AGRONOMO
        "perfil-proveedor" -> AppRole.PROVEEDOR
        "perfil-comprador" -> AppRole.COMPRADOR
        else -> AppRole.CAFICULTOR
    }
    val config = when (role) {
        "perfil-ingeniero" -> RoleConfig(
            "Ingeniero agrónomo",
            "Acompañamiento técnico en campo",
            "Selecciona cualquier finca, registra observaciones y labores de visita visibles para todos, y responde inquietudes técnicas.",
            Icons.Default.Eco,
            listOf("agronomia" to "Panel agronómico", "fincas" to "Explorar fincas", "cultivo" to "Ver bitácoras", "proveedores" to "Proveedores"),
        )
        "perfil-proveedor" -> RoleConfig(
            "Proveedor",
            "Insumos, herramientas y asesoría comercial",
            "Muestra e ingresa tus productos, categorías, precios de referencia y ubicación para que caficultores e ingenieros te contacten.",
            Icons.Default.Inventory2,
            listOf("proveedores" to "Proveedores", "mapa" to "Mapa satelital", "fincas" to "Ver fincas"),
        )
        "perfil-comprador" -> RoleConfig(
            "Comprador",
            "Precios de compra y origen del café",
            "Publica precios diarios de compra por kilo o carga, explora fincas trazables y consulta bitácoras y visitas agronómicas.",
            Icons.Default.ShoppingBag,
            listOf("precios" to "Precios de compra", "fincas" to "Explorar fincas", "mapa" to "Mapa satelital", "proveedores" to "Proveedores"),
        )
        else -> RoleConfig(
            "Caficultor",
            "Del cultivo a una mejor decisión",
            "Edita tus fincas y tu bitácora de cultivo propia, controla cosechas y costos, y visualiza el manejo de otras fincas.",
            Icons.Default.Person,
            listOf("fincas" to "Fincas", "cultivo" to "Bitácoras", "precios" to "Precios", "mapa" to "Mapa satelital"),
        )
    }

    val fincas = (fincasState as? UiState.Success)?.data.orEmpty().filter { it.recordType == "finca" }
    val visitas = (visitasState as? UiState.Success)?.data.orEmpty()
    val proveedores = (proveedoresState as? UiState.Success)?.data.orEmpty()
    val compradores = (compradoresState as? UiState.Success)?.data.orEmpty()
    val isCurrentlyActive = activeAppRole == targetAppRole

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            SectionHeader(
                kicker = "PERFILES · ALBÁN CAFÉ 360°",
                title = "Perfil ${config.name.lowercase()}",
                subtitle = if (isCurrentlyActive) {
                    "Este es tu rol permanente activo en la plataforma."
                } else {
                    "Vista comunitaria de solo lectura. Tu rol permanente es ${activeAppRole.displayName}."
                },
            )
        }
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = ForestGreen),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(listOf(ForestGreen, ForestGreenLight)))
                        .padding(20.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Surface(color = WarmGold, shape = RoundedCornerShape(50)) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Icon(config.icon, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(14.dp))
                                    Text(config.name.uppercase(), style = MaterialTheme.typography.labelSmall, color = ForestGreen)
                                }
                            }
                            Surface(
                                color = Color.White.copy(alpha = 0.20f),
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.45f)),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                                    Text(
                                        text = if (isCurrentlyActive) "Tu rol permanente" else "Solo consulta",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                    )
                                }
                            }
                        }
                        Text(config.headline, style = MaterialTheme.typography.headlineMedium, color = Color.White)
                        Text(config.desc, style = MaterialTheme.typography.bodyMedium, color = Color(0xFFE6EEE4))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            config.shortcuts.forEach { (route, label) ->
                                Button(
                                    onClick = { onNavigate(route) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = ForestGreen),
                                ) {
                                    Text(label)
                                    Spacer(Modifier.width(4.dp))
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(15.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        when (role) {
            "perfil-comprador" -> {
                item {
                    Text("Precios y puntos de compra recientes", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                }
                if (compradores.isEmpty()) {
                    item {
                        EmptyStateCard(
                            title = "Aún no hay precios de compra publicados",
                            subtitle = "Registra precios diarios en Precios del café o explora las fincas disponibles.",
                            actionLabel = "Ir a Precios",
                            onAction = { onNavigate("precios") },
                        )
                    }
                } else {
                    items(compradores.take(6), key = { it.id }) { offer ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(offer.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                    if (offer.offerAmountCop > 0.0) {
                                        Text(formatCop(offer.offerAmountCop), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                Text(
                                    text = "${offer.priceUnit} · ${offer.location.ifBlank { "Albán, Nariño" }}${if (offer.priceDate.isNotBlank()) " · ${offer.priceDate}" else ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                item {
                    Text("Fincas para descubrir", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                }
                items(fincas, key = { it.id }) { finca ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(finca.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "${finca.location}${if (finca.ownerName.isNotBlank()) " · ${finca.ownerName}" else ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            OutlinedButton(onClick = { onOpenProfile(finca.recordCode.ifBlank { finca.id }) }) {
                                Text("Ver ficha")
                            }
                        }
                    }
                }
            }
            "perfil-proveedor" -> {
                item {
                    Text("Productos e insumos publicados", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                }
                if (proveedores.isEmpty()) {
                    item {
                        EmptyStateCard(
                            title = "Aún no hay productos publicados",
                            subtitle = "Publica fertilizantes, semillas, herramientas o servicios para la comunidad cafetera.",
                            actionLabel = "Ir a Proveedores",
                            onAction = { onNavigate("proveedores") },
                        )
                    }
                } else {
                    items(proveedores, key = { it.id }) { prov ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(prov.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                if (prov.category.isNotBlank()) {
                                    Text(prov.category, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                }
                                Text(prov.productsServices, style = MaterialTheme.typography.bodySmall)
                                if (prov.priceReference.isNotBlank() || prov.commercialContact.isNotBlank()) {
                                    Text(
                                        text = listOf(prov.priceReference, prov.commercialContact).filter { it.isNotBlank() }.joinToString(" · "),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = ForestGreen,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            "perfil-ingeniero" -> {
                item {
                    Text("Visitas y asistencia técnica", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                }
                if (visitas.isEmpty()) {
                    item {
                        EmptyStateCard(
                            title = "Sin visitas registradas",
                            subtitle = "Registra una visita desde el panel agronómico seleccionando una finca.",
                            actionLabel = "Abrir panel agronómico",
                            onAction = { onNavigate("agronomia") },
                        )
                    }
                } else {
                    items(visitas, key = { it.id }) { visita ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Ing. ${visita.engineerName} · ${visita.farmName.ifBlank { "Visita técnica" }}", style = MaterialTheme.typography.titleMedium)
                                Text("${visita.diagnosis} · ${visita.date}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                if (visita.activitiesPerformed.isNotBlank()) {
                                    Text("Actividades: ${visita.activitiesPerformed}", style = MaterialTheme.typography.bodySmall)
                                }
                                if (visita.recommendations.isNotBlank()) {
                                    Text("Recomendaciones: ${visita.recommendations}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
            else -> {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    ) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Tu ruta cafetera", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                            Text(
                                "Administra tu finca y bitácora propia, consulta las observaciones del ingeniero agrónomo, explora el manejo de otras fincas, calcula costos y compara precios diarios antes de vender.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class RoleConfig(
    val name: String,
    val headline: String,
    val desc: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val shortcuts: List<Pair<String, String>>,
)
