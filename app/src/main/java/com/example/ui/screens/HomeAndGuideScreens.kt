package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.example.R
import com.example.data.LiveMarketSnapshot
import com.example.model.AppRole
import com.example.model.CompradorItem
import com.example.model.CosechaRecord
import com.example.model.CostoCalculation
import com.example.model.CultivoActivity
import com.example.model.FincaProfile
import com.example.model.InquietudTecnica
import com.example.model.PrecioCafeRecord
import com.example.model.ProveedorItem
import com.example.model.UiState
import com.example.model.UserProfile
import com.example.model.VisitaAgronomica
import com.example.ui.components.AsyncDrawableImage
import com.example.ui.components.SectionHeader
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.ForestGreenLight
import com.example.ui.theme.NoticeBorder
import com.example.ui.theme.NoticeSand
import com.example.ui.theme.NoticeText
import com.example.ui.theme.RoastedBronze
import com.example.ui.theme.SagePale
import com.example.ui.theme.Terracotta
import com.example.ui.theme.WarmBeigeIcon
import com.example.ui.theme.WarmGold
import com.example.util.formatCop
import com.example.util.formatNumber
import com.example.util.todayIsoDate
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.ShoppingBag

private data class RecentActivityItem(
    val title: String,
    val typeLabel: String,
    val date: String,
    val sortStamp: Long,
    val route: String,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    currentUserId: String,
    appRole: AppRole,
    userDisplayName: String?,
    userProfile: UserProfile?,
    fincasState: UiState<List<FincaProfile>>,
    cosechasState: UiState<List<CosechaRecord>>,
    cultivosState: UiState<List<CultivoActivity>>,
    costosState: UiState<List<CostoCalculation>>,
    visitasState: UiState<List<VisitaAgronomica>>,
    inquietudesState: UiState<List<InquietudTecnica>>,
    proveedoresState: UiState<List<ProveedorItem>>,
    compradoresState: UiState<List<CompradorItem>>,
    preciosCafeState: UiState<List<PrecioCafeRecord>>,
    liveMarket: LiveMarketSnapshot,
    onNavigate: (String) -> Unit,
    onEditProfile: () -> Unit,
    onAskMarketAi: () -> Unit,
) {
    val context = LocalContext.current
    val fincasList = (fincasState as? UiState.Success)?.data.orEmpty()
    val cosechasList = (cosechasState as? UiState.Success)?.data.orEmpty()
    val cultivosList = (cultivosState as? UiState.Success)?.data.orEmpty()
    val costosList = (costosState as? UiState.Success)?.data.orEmpty()
    val visitasList = (visitasState as? UiState.Success)?.data.orEmpty()
    val inquietudesList = (inquietudesState as? UiState.Success)?.data.orEmpty()
    val proveedoresList = (proveedoresState as? UiState.Success)?.data.orEmpty()
    val compradoresList = (compradoresState as? UiState.Success)?.data.orEmpty()
    val preciosList = (preciosCafeState as? UiState.Success)?.data.orEmpty()

    val fincaItems = fincasList.filter { it.recordType == "finca" }
    val fincaCount = fincaItems.size
    val caficultorCount = fincasList.count { it.recordType == "caficultor" }
    val totalHectares = fincaItems.sumOf { it.areaHectares }
    val totalKilos = cosechasList.sumOf { it.quantityKg }
    val today = todayIsoDate()
    val harvestAlertsCount = cosechasList.count { it.date >= today }
    val pricedOffersCount = compradoresList.count { it.offerAmountCop > 0.0 }

    val myFincasCount = fincaItems.count { it.userId == currentUserId }
    val otherFincasCount = fincaItems.count { it.userId != currentUserId }
    val myCultivosCount = cultivosList.count { it.userId == currentUserId }
    val otherCultivosCount = cultivosList.count { it.userId != currentUserId }
    val myVisitasCount = visitasList.count { it.userId == currentUserId }
    val openInquietudesCount = inquietudesList.count { it.status != "respondida" }
    val myProveedoresCount = proveedoresList.count { it.userId == currentUserId }
    val myCompradoresCount = compradoresList.count { it.userId == currentUserId }

    val displayFullName = userProfile?.fullName?.trim()?.takeIf { it.isNotBlank() }
        ?: userDisplayName?.trim()?.takeIf { it.isNotBlank() }
        ?: appRole.displayName
    val userRole = appRole.displayName
    val userPhone = userProfile?.phone?.takeIf { it.isNotBlank() }
    val userLocation = userProfile?.location?.takeIf { it.isNotBlank() } ?: "San José de Albán, Nariño"

    val siembraCount = cultivosList.count { it.activityType.equals("Siembra", ignoreCase = true) }
    val abonoCount = cultivosList.count { it.activityType.equals("Abono", ignoreCase = true) }
    val cosechaLogCount = cultivosList.count { it.activityType.equals("Cosecha", ignoreCase = true) }

    // Recent multi-module activity (excludes private recolectores, matching initial platform)
    val recentActivities = buildList {
        fincasList.forEach { f ->
            add(
                RecentActivityItem(
                    title = f.name,
                    typeLabel = if (f.recordType == "finca") "Finca" else "Caficultor",
                    date = today,
                    sortStamp = f.updatedAt?.toDate()?.time ?: f.createdAt?.toDate()?.time ?: 0L,
                    route = "fincas",
                )
            )
        }
        cosechasList.forEach { c ->
            add(
                RecentActivityItem(
                    title = "${c.workerName} (${formatNumber(c.quantityKg)} kg)",
                    typeLabel = "Cosecha",
                    date = c.date,
                    sortStamp = c.createdAt?.toDate()?.time ?: 0L,
                    route = "cosecha",
                )
            )
        }
        cultivosList.forEach { a ->
            add(
                RecentActivityItem(
                    title = "${a.activityType} · ${a.name}",
                    typeLabel = "Actividad de cultivo",
                    date = a.date,
                    sortStamp = a.createdAt?.toDate()?.time ?: 0L,
                    route = "cultivo",
                )
            )
        }
        costosList.forEach { k ->
            add(
                RecentActivityItem(
                    title = "${k.name} (${formatCop(k.totalAmount)})",
                    typeLabel = "Costo de producción",
                    date = k.date,
                    sortStamp = k.createdAt?.toDate()?.time ?: 0L,
                    route = "historial",
                )
            )
        }
        visitasList.forEach { v ->
            add(
                RecentActivityItem(
                    title = "${v.diagnosis} · Ing. ${v.engineerName}",
                    typeLabel = "Visita agronómica",
                    date = v.date,
                    sortStamp = v.createdAt?.toDate()?.time ?: 0L,
                    route = "agronomia",
                )
            )
        }
        inquietudesList.forEach { q ->
            add(
                RecentActivityItem(
                    title = "${q.subject} · ${q.authorName}",
                    typeLabel = if (q.status == "respondida") "Inquietud respondida" else "Inquietud abierta",
                    date = q.date,
                    sortStamp = q.updatedAt?.toDate()?.time ?: q.createdAt?.toDate()?.time ?: 0L,
                    route = "agronomia",
                )
            )
        }
        preciosList.forEach { p ->
            add(
                RecentActivityItem(
                    title = "${p.source} (${formatCop(p.pricePerCargaCop)} / carga)",
                    typeLabel = "Precio del café",
                    date = p.date,
                    sortStamp = p.createdAt?.toDate()?.time ?: 0L,
                    route = "precios",
                )
            )
        }
        proveedoresList.forEach { p ->
            add(
                RecentActivityItem(
                    title = p.name,
                    typeLabel = "Proveedor",
                    date = p.location,
                    sortStamp = p.createdAt?.toDate()?.time ?: 0L,
                    route = "proveedores",
                )
            )
        }
    }.sortedByDescending { it.sortStamp }.take(6)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // 1. Original Hero + Personalized Farmer Greeting
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp)),
            ) {
                AsyncDrawableImage(
                    resId = R.drawable.img_coffee_hero,
                    contentDescription = "Cafetales de San José de Albán, Nariño",
                    contentScale = ContentScale.Crop,
                    maxDimPx = 960,
                    placeholderColor = ForestGreen,
                    modifier = Modifier.matchParentSize(),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    ForestGreen.copy(alpha = 0.95f),
                                    ForestGreen.copy(alpha = 0.80f),
                                )
                            )
                        )
                        .padding(22.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Surface(color = WarmGold, shape = RoundedCornerShape(50)) {
                                Text(
                                    text = "SAN JOSÉ DE ALBÁN · NARIÑO",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ForestGreen,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                )
                            }
                            Surface(
                                onClick = onEditProfile,
                                color = Color.White.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                                modifier = Modifier.testTag("home_edit_profile_button"),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Editar perfil",
                                        tint = Color.White,
                                        modifier = Modifier.size(13.dp),
                                    )
                                    Text(
                                        text = "Mi perfil",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = "¡Bienvenido, $displayFullName!",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = WarmGold,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = "El valor de tu café empieza con una mejor decisión.",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                )
                                Text(
                                    text = "ALBÁN CAFÉ 360° reúne tus registros de producción y herramientas para consultar referencias, calcular costos y prepararte para comparar alternativas de venta.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFF0EADD),
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = Color(0xFFFFFDF7),
                                border = BorderStroke(2.dp, WarmGold),
                                modifier = Modifier.size(78.dp),
                            ) {
                                AsyncDrawableImage(
                                    resId = R.drawable.ic_launcher_art,
                                    contentDescription = "Logo principal de ALBÁN CAFÉ 360°",
                                    contentScale = ContentScale.Crop,
                                    maxDimPx = 256,
                                    placeholderColor = Color(0xFFFFFDF7),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(4.dp)
                                        .clip(RoundedCornerShape(14.dp)),
                                )
                            }
                        }

                        // Compact User Contact & Location Strip
                        Surface(
                            color = Color.White.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            FlowRow(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = WarmGold, modifier = Modifier.size(14.dp))
                                    Text(userRole, style = MaterialTheme.typography.labelSmall, color = Color.White)
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                ) {
                                    Icon(Icons.Default.Phone, contentDescription = null, tint = WarmGold, modifier = Modifier.size(14.dp))
                                    Text(
                                        text = userPhone ?: "Agrega tu celular",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                ) {
                                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = WarmGold, modifier = Modifier.size(14.dp))
                                    Text(
                                        text = buildString {
                                            append(userLocation)
                                            val fn = userProfile?.farmName?.trim().orEmpty()
                                            if (fn.isNotBlank()) append(" · $fn")
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFF0EADD),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Button(
                                onClick = {
                                    when (appRole) {
                                        AppRole.AGRONOMO -> onNavigate("agronomia")
                                        AppRole.PROVEEDOR -> onNavigate("proveedores")
                                        AppRole.COMPRADOR -> onNavigate("precios")
                                        AppRole.CAFICULTOR -> onNavigate("cultivo")
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = WarmGold,
                                    contentColor = ForestGreen,
                                ),
                                modifier = Modifier.testTag("home_role_primary_button"),
                            ) {
                                Icon(Icons.Default.Book, contentDescription = null, modifier = Modifier.size(17.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    when (appRole) {
                                        AppRole.AGRONOMO -> "Registrar visita"
                                        AppRole.PROVEEDOR -> "Mis productos"
                                        AppRole.COMPRADOR -> "Precios de compra"
                                        AppRole.CAFICULTOR -> "Mi bitácora de finca"
                                    }
                                )
                            }
                            OutlinedButton(
                                onClick = { onNavigate("guia") },
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.55f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                modifier = Modifier.testTag("home_guide_button"),
                            ) {
                                Icon(Icons.Default.Book, contentDescription = null, modifier = Modifier.size(17.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Guía de uso")
                            }
                        }
                    }
                }
            }
        }

        // 1B. Locked Role-Focused Workspace + Community Read-Only Exploration
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(20.dp),
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ENFOQUE PERMANENTE DE TU CUENTA",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                            Text(
                                text = "Mi espacio · ${appRole.displayName}",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Surface(
                            color = SagePale,
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, ForestGreen.copy(alpha = 0.25f)),
                            modifier = Modifier.testTag("home_locked_role_badge"),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Rol permanente",
                                    tint = ForestGreen,
                                    modifier = Modifier.size(13.dp),
                                )
                                Text(
                                    text = "Rol fijo",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ForestGreen,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }

                    Text(
                        text = appRole.summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = SagePale.copy(alpha = 0.7f),
                        border = BorderStroke(1.dp, ForestGreen.copy(alpha = 0.22f)),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            when (appRole) {
                                AppRole.AGRONOMO -> {
                                    Text(
                                        text = "TU GESTIÓN COMO INGENIERO AGRÓNOMO",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ForestGreen,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        text = "Selecciona cualquier finca de la comunidad para registrar visitas técnicas ($myVisitasCount tuyas), dejar observaciones visibles y responder inquietudes abiertas ($openInquietudesCount).",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ForestGreen,
                                    )
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Button(onClick = { onNavigate("agronomia") }) {
                                            Icon(Icons.Default.Eco, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Visitas ($myVisitasCount) e inquietudes ($openInquietudesCount)")
                                        }
                                        OutlinedButton(onClick = { onNavigate("fincas") }) {
                                            Icon(Icons.Default.Landscape, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Elegir finca ($fincaCount)")
                                        }
                                    }
                                }
                                AppRole.PROVEEDOR -> {
                                    Text(
                                        text = "TU GESTIÓN COMO PROVEEDOR",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ForestGreen,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        text = "Publica y actualiza tus productos, categorías, precios de referencia y ubicación en el mapa ($myProveedoresCount publicados por ti).",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ForestGreen,
                                    )
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Button(onClick = { onNavigate("proveedores") }) {
                                            Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Mis productos ($myProveedoresCount)")
                                        }
                                        OutlinedButton(onClick = { onNavigate("mapa") }) {
                                            Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Ubicar tienda en mapa")
                                        }
                                    }
                                }
                                AppRole.COMPRADOR -> {
                                    Text(
                                        text = "TU GESTIÓN COMO COMPRADOR",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ForestGreen,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        text = "Ingresa precios diarios de compra por kilogramo o carga, registra tu historial de precios y marca tu punto de compra en el mapa ($myCompradoresCount ofertas tuyas).",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ForestGreen,
                                    )
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Button(onClick = { onNavigate("precios") }) {
                                            Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Mis precios de compra ($myCompradoresCount)")
                                        }
                                        OutlinedButton(onClick = { onNavigate("mapa") }) {
                                            Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Punto en el mapa")
                                        }
                                    }
                                }
                                AppRole.CAFICULTOR -> {
                                    Text(
                                        text = "TU GESTIÓN PROPIA COMO CAFICULTOR",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ForestGreen,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        text = "Edita tus fincas ($myFincasCount), lleva tu bitácora de cultivo ($myCultivosCount labores), controla cosechas, calcula costos y envía inquietudes a los agrónomos.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ForestGreen,
                                    )
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Button(onClick = { onNavigate("fincas") }) {
                                            Icon(Icons.Default.Landscape, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Mis fincas ($myFincasCount)")
                                        }
                                        OutlinedButton(onClick = { onNavigate("cultivo") }) {
                                            Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Mi bitácora ($myCultivosCount)")
                                        }
                                        OutlinedButton(onClick = { onNavigate("cosecha") }) {
                                            Icon(Icons.Default.Scale, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Cosechas")
                                        }
                                        OutlinedButton(onClick = { onNavigate("costos") }) {
                                            Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Costos")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(Icons.Default.Public, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                            Text(
                                text = "EXPLORAR COMUNIDAD (SOLO LECTURA)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Text(
                            text = appRole.communityFocusText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        val communityItems = listOf(
                            Triple("fincas", Icons.Default.Landscape, "Otras fincas ($otherFincasCount)"),
                            Triple("cultivo", Icons.Default.CalendarMonth, "Bitácoras comunitarias ($otherCultivosCount)"),
                            Triple("agronomia", Icons.Default.QuestionAnswer, "Visitas e inquietudes (${visitasList.size + inquietudesList.size})"),
                            Triple("proveedores", Icons.Default.Inventory2, "Catálogo proveedores (${proveedoresList.size})"),
                            Triple("precios", Icons.Default.TrendingUp, "Precios compradores ($pricedOffersCount)"),
                            Triple("mapa", Icons.Default.Map, "Ver todo en el mapa"),
                        )
                        communityItems.chunked(2).forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                pair.forEach { (route, icon, label) ->
                                    Surface(
                                        onClick = { onNavigate(route) },
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.background,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        ) {
                                            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Pulso cafetero · hoy (Original Market Pulse + Live NY Coffee + TRM)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(20.dp),
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "MERCADO EN VIVO",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                            Text(
                                text = "Pulso cafetero · hoy",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        OutlinedButton(onClick = { onNavigate("precios") }) {
                            Text("Ver fuentes")
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(15.dp))
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        MiniPulseTile(
                            label = "Bolsa NY / FNC",
                            value = liveMarket.nyCoffeeCentsLb?.let { "${formatNumber(it, 2)} ¢/lb" }
                                ?: liveMarket.fncDetectedCargaCop?.let { formatCop(it) }
                                ?: "Sin verificar",
                            modifier = Modifier.weight(1f),
                        )
                        MiniPulseTile(
                            label = "Dólar hoy · TRM",
                            value = liveMarket.trmCop?.let { formatCop(it) } ?: "No disponible",
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        MiniPulseTile(
                            label = "Proveedores aportados",
                            value = "${proveedoresList.size}",
                            modifier = Modifier.weight(1f),
                        )
                        MiniPulseTile(
                            label = "Alertas de cosecha",
                            value = "$harvestAlertsCount",
                            modifier = Modifier.weight(1f),
                        )
                    }

                    Text(
                        text = "Los datos de proveedores y cosechas provienen de registros de usuarios; el precio oficial requiere boletín verificable. Último precio guardado: ${preciosList.firstOrNull()?.let { formatCop(it.pricePerCargaCop) } ?: "Ninguno"} ($pricedOffersCount ofertas locales).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(
                        onClick = {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://federaciondecafeteros.org/publicaciones/"))
                            )
                        },
                        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = "Noticias y boletines FNC",
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.tertiary)
                    }
                }
            }
        }

        // 3. Ruta para una venta informada (Original 3 Steps: 01 Consulta, 02 Calcula, 03 Compara)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "RUTA PARA UNA VENTA INFORMADA",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
                StepBannerCard(
                    number = "01 · Consulta",
                    description = "Revisa fuente, fecha y unidad del precio de referencia.",
                    icon = Icons.Default.TrendingUp,
                    onClick = { onNavigate("precios") },
                )
                StepBannerCard(
                    number = "02 · Calcula",
                    description = "Desglosa tus gastos y estima el costo por kilogramo.",
                    icon = Icons.Default.Calculate,
                    onClick = { onNavigate("costos") },
                )
                StepBannerCard(
                    number = "03 · Compara",
                    description = "Contrasta ofertas de la misma calidad y unidad, incluidos descuentos y transporte.",
                    icon = Icons.Default.Scale,
                    onClick = { onNavigate("precios") },
                )
            }
        }

        // 4. Original 4 Core KPI Stat Cards (including highlighted Caficultores card)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    StatKpiCard(
                        icon = Icons.Default.Landscape,
                        label = "Fincas registradas",
                        value = "$fincaCount",
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigate("fincas") },
                    )
                    StatKpiCard(
                        icon = Icons.Default.Person,
                        label = "Caficultores",
                        value = "$caficultorCount",
                        highlighted = true,
                        caption = "PERSONAS QUE CULTIVAN FUTURO",
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigate("fincas") },
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    StatKpiCard(
                        icon = Icons.Default.Inventory2,
                        label = "Proveedores",
                        value = "${proveedoresList.size}",
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigate("proveedores") },
                    )
                    StatKpiCard(
                        icon = Icons.Default.Scale,
                        label = "Kilogramos registrados",
                        value = "${formatNumber(totalKilos)} kg",
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigate("cosecha") },
                    )
                }
            }
        }

        // 5. Original Escuela Cafetera Feature Banner
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
                        Surface(color = WarmGold, shape = RoundedCornerShape(50)) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                            ) {
                                Icon(Icons.Default.School, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(14.dp))
                                Text("APRENDE Y CRECE", style = MaterialTheme.typography.labelSmall, color = ForestGreen)
                            }
                        }
                        Text(
                            text = "Escuela Cafetera",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                        Text(
                            text = "Explora cursos, capacitaciones y canales oficiales para consultar la agenda de la Federación Nacional de Cafeteros.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFE2EBDF),
                        )
                        Button(
                            onClick = { onNavigate("escuela") },
                            colors = ButtonDefaults.buttonColors(containerColor = WarmGold, contentColor = ForestGreen),
                        ) {
                            Text("Explorar escuela")
                            Spacer(Modifier.width(6.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // 6. Role-Tailored Summary Cards
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column {
                    Text(
                        text = "RESUMEN DE OPERACIÓN · ${appRole.shortUpperLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Text(
                        text = "Tus módulos principales",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                when (appRole) {
                    AppRole.AGRONOMO -> {
                        val latestVisit = visitasList.firstOrNull()
                        DashboardModuleSummaryCard(
                            moduleTitle = "Visitas técnicas",
                            kicker = "TU GESTIÓN EN CAMPO",
                            icon = Icons.Default.Eco,
                            primaryMetric = "${myVisitasCount} ${if (myVisitasCount == 1) "visita" else "visitas"}",
                            secondaryMetric = "${visitasList.size} en la red · $openInquietudesCount inquietudes abiertas",
                            detailText = latestVisit?.let {
                                "Reciente: ${it.farmName.ifBlank { it.farmCode }} · ${it.diagnosis}"
                            } ?: "Selecciona una finca para registrar observaciones y recomendaciones.",
                            actionLabel = "Ir a Agronomía",
                            tag = "dashboard_card_agronomia",
                            accentContainerColor = SagePale,
                            accentContentColor = ForestGreen,
                            onClick = { onNavigate("agronomia") },
                        )
                        val latestFinca = fincaItems.firstOrNull()
                        DashboardModuleSummaryCard(
                            moduleTitle = "Fincas de la comunidad",
                            kicker = "CONSULTA Y SEGUIMIENTO",
                            icon = Icons.Default.Landscape,
                            primaryMetric = "$fincaCount ${if (fincaCount == 1) "finca" else "fincas"}",
                            secondaryMetric = "${formatNumber(totalHectares)} ha · ${cultivosList.size} labores en bitácoras",
                            detailText = latestFinca?.let { "Última finca: ${it.name} (${it.location})" }
                                ?: "Explora las fincas cafeteras para planear visitas agronómicas.",
                            actionLabel = "Ver Fincas",
                            tag = "dashboard_card_fincas",
                            accentContainerColor = WarmBeigeIcon,
                            accentContentColor = RoastedBronze,
                            onClick = { onNavigate("fincas") },
                        )
                    }
                    AppRole.PROVEEDOR -> {
                        val latestProv = proveedoresList.firstOrNull { it.userId == currentUserId }
                            ?: proveedoresList.firstOrNull()
                        DashboardModuleSummaryCard(
                            moduleTitle = "Mis productos",
                            kicker = "CATÁLOGO COMERCIAL",
                            icon = Icons.Default.Inventory2,
                            primaryMetric = "$myProveedoresCount ${if (myProveedoresCount == 1) "producto" else "productos"}",
                            secondaryMetric = "${proveedoresList.size} publicaciones activas en la comunidad",
                            detailText = latestProv?.let { "${it.name} · ${it.productsServices}" }
                                ?: "Publica fertilizantes, semillas, herramientas y tus precios de referencia.",
                            actionLabel = "Gestionar catálogo",
                            tag = "dashboard_card_proveedores",
                            accentContainerColor = SagePale,
                            accentContentColor = ForestGreen,
                            onClick = { onNavigate("proveedores") },
                        )
                        DashboardModuleSummaryCard(
                            moduleTitle = "Mapa y cobertura",
                            kicker = "TERRITORIO NARIÑO",
                            icon = Icons.Default.Map,
                            primaryMetric = "$fincaCount fincas",
                            secondaryMetric = "Ubica tu almacén o punto de despacho en el mapa satelital",
                            detailText = "Conecta tus insumos agrícolas con caficultores cercanos en San José de Albán.",
                            actionLabel = "Abrir Mapa",
                            tag = "dashboard_card_mapa",
                            accentContainerColor = NoticeSand,
                            accentContentColor = NoticeText,
                            onClick = { onNavigate("mapa") },
                        )
                    }
                    AppRole.COMPRADOR -> {
                        val latestOffer = compradoresList.firstOrNull { it.userId == currentUserId }
                            ?: compradoresList.firstOrNull()
                        DashboardModuleSummaryCard(
                            moduleTitle = "Precios y compras",
                            kicker = "MERCADO DIARIO DE CAFÉ",
                            icon = Icons.Default.ShoppingBag,
                            primaryMetric = "$myCompradoresCount ${if (myCompradoresCount == 1) "oferta" else "ofertas"}",
                            secondaryMetric = "$pricedOffersCount precios publicados en Albán · ${preciosList.size} registros",
                            detailText = latestOffer?.let {
                                "${it.name}: ${if (it.offerAmountCop > 0) formatCop(it.offerAmountCop) else "Punto activo"} / ${it.priceUnit}"
                            } ?: "Publica el precio de compra del día por carga o kilogramo.",
                            actionLabel = "Publicar precio",
                            tag = "dashboard_card_precios",
                            accentContainerColor = SagePale,
                            accentContentColor = ForestGreen,
                            onClick = { onNavigate("precios") },
                        )
                        DashboardModuleSummaryCard(
                            moduleTitle = "Fincas de origen",
                            kicker = "TRAZABILIDAD QR",
                            icon = Icons.Default.Landscape,
                            primaryMetric = "$fincaCount fincas",
                            secondaryMetric = "${formatNumber(totalHectares)} ha registradas · ${visitasList.size} visitas técnicas",
                            detailText = "Consulta fincas, bitácoras de manejo y fichas QR de los caficultores.",
                            actionLabel = "Explorar Fincas",
                            tag = "dashboard_card_fincas",
                            accentContainerColor = WarmBeigeIcon,
                            accentContentColor = RoastedBronze,
                            onClick = { onNavigate("fincas") },
                        )
                    }
                    AppRole.CAFICULTOR -> {
                        val latestFinca = fincaItems.firstOrNull { it.userId == currentUserId } ?: fincaItems.firstOrNull()
                        DashboardModuleSummaryCard(
                            moduleTitle = "Mis fincas",
                            kicker = "PERFILES Y TRAZABILIDAD QR",
                            icon = Icons.Default.Landscape,
                            primaryMetric = when (fincasState) {
                                is UiState.Loading -> "Cargando..."
                                is UiState.Error -> "Sin conexión"
                                is UiState.Success -> "$myFincasCount ${if (myFincasCount == 1) "propia" else "propias"}"
                            },
                            secondaryMetric = when {
                                fincasState is UiState.Success && totalHectares > 0.0 ->
                                    "${formatNumber(totalHectares)} ha comunidad · $otherFincasCount de otros productores"
                                fincasState is UiState.Success ->
                                    "$fincaCount fincas en la red · $caficultorCount caficultores"
                                else -> "Sincronizando con tu cuenta"
                            },
                            detailText = when {
                                fincasState is UiState.Error -> fincasState.message
                                latestFinca != null -> "Finca: ${latestFinca.name} (${latestFinca.location})"
                                else -> "Registra tus fincas con ubicación satelital y código QR."
                            },
                            actionLabel = "Abrir Fincas",
                            tag = "dashboard_card_fincas",
                            accentContainerColor = SagePale,
                            accentContentColor = ForestGreen,
                            onClick = { onNavigate("fincas") },
                        )

                        val latestHarvest = cosechasList.firstOrNull()
                        DashboardModuleSummaryCard(
                            moduleTitle = "Cosechas",
                            kicker = "PESAJES Y PRODUCCIÓN",
                            icon = Icons.Default.Scale,
                            primaryMetric = when (cosechasState) {
                                is UiState.Loading -> "Cargando..."
                                is UiState.Error -> "Sin conexión"
                                is UiState.Success -> "${formatNumber(totalKilos)} kg"
                            },
                            secondaryMetric = when (cosechasState) {
                                is UiState.Success -> "${cosechasList.size} ${if (cosechasList.size == 1) "pesaje registrado" else "pesajes registrados"}"
                                else -> "Calculando kilos recolectados"
                            },
                            detailText = when {
                                cosechasState is UiState.Error -> cosechasState.message
                                latestHarvest != null ->
                                    "Último pesaje: ${latestHarvest.workerName} · ${formatNumber(latestHarvest.quantityKg)} kg (${latestHarvest.date})"
                                else -> "Lleva el control diario de recolección por trabajador y finca."
                            },
                            actionLabel = "Ir a Cosechas",
                            tag = "dashboard_card_cosechas",
                            accentContainerColor = WarmBeigeIcon,
                            accentContentColor = RoastedBronze,
                            onClick = { onNavigate("cosecha") },
                        )

                        val latestActivity = cultivosList.firstOrNull { it.userId == currentUserId } ?: cultivosList.firstOrNull()
                        DashboardModuleSummaryCard(
                            moduleTitle = "Bitácora de cultivo",
                            kicker = "LABORES DE TU FINCA",
                            icon = Icons.Default.CalendarMonth,
                            primaryMetric = when (cultivosState) {
                                is UiState.Loading -> "Cargando..."
                                is UiState.Error -> "Sin conexión"
                                is UiState.Success -> "$myCultivosCount ${if (myCultivosCount == 1) "labor" else "labores"}"
                            },
                            secondaryMetric = when (cultivosState) {
                                is UiState.Success -> "Siembra: $siembraCount · Abono: $abonoCount · Comunidad: $otherCultivosCount"
                                else -> "Consultando labores recientes"
                            },
                            detailText = when {
                                cultivosState is UiState.Error -> cultivosState.message
                                latestActivity != null ->
                                    "Reciente: ${latestActivity.activityType} · ${latestActivity.name} (${latestActivity.date})"
                                else -> "Documenta siembras, abonados y cosechas asociadas a tus lotes."
                            },
                            actionLabel = "Abrir Bitácora",
                            tag = "dashboard_card_bitacora",
                            accentContainerColor = NoticeSand,
                            accentContentColor = NoticeText,
                            onClick = { onNavigate("cultivo") },
                        )
                    }
                }
            }
        }

        // 7. Quick Access Grid (Role-focused shortcuts)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(20.dp),
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Accesos rápidos",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = "Enfocados para ${appRole.displayName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    val actions = when (appRole) {
                        AppRole.CAFICULTOR -> listOf(
                            Triple("fincas", Icons.Default.Landscape, "Mis fincas"),
                            Triple("cultivo", Icons.Default.CalendarMonth, "Mi bitácora"),
                            Triple("cosecha", Icons.Default.Scale, "Registrar cosecha"),
                            Triple("costos", Icons.Default.Calculate, "Calcular costos"),
                            Triple("precios", Icons.Default.TrendingUp, "Precios del café"),
                            Triple("mapa", Icons.Default.Map, "Mapa satelital"),
                        )
                        AppRole.AGRONOMO -> listOf(
                            Triple("agronomia", Icons.Default.Eco, "Visitas e inquietudes"),
                            Triple("fincas", Icons.Default.Landscape, "Explorar fincas"),
                            Triple("cultivo", Icons.Default.CalendarMonth, "Ver bitácoras"),
                            Triple("mapa", Icons.Default.Map, "Mapa de fincas"),
                            Triple("proveedores", Icons.Default.Inventory2, "Proveedores"),
                            Triple("precios", Icons.Default.TrendingUp, "Precios del café"),
                        )
                        AppRole.PROVEEDOR -> listOf(
                            Triple("proveedores", Icons.Default.Inventory2, "Mis productos"),
                            Triple("mapa", Icons.Default.Map, "Ubicar punto comercial"),
                            Triple("fincas", Icons.Default.Landscape, "Ver fincas"),
                            Triple("precios", Icons.Default.TrendingUp, "Mercado del café"),
                            Triple("agronomia", Icons.Default.Eco, "Consultas técnicas"),
                            Triple("escuela", Icons.Default.School, "Escuela Cafetera"),
                        )
                        AppRole.COMPRADOR -> listOf(
                            Triple("precios", Icons.Default.TrendingUp, "Publicar compra"),
                            Triple("mapa", Icons.Default.Map, "Mapa de compra"),
                            Triple("fincas", Icons.Default.Landscape, "Fincas de origen"),
                            Triple("cultivo", Icons.Default.CalendarMonth, "Bitácoras de lotes"),
                            Triple("agronomia", Icons.Default.Eco, "Visitas técnicas"),
                            Triple("proveedores", Icons.Default.Inventory2, "Proveedores"),
                        )
                    }
                    actions.chunked(2).forEach { pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            pair.forEach { (route, icon, label) ->
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onNavigate(route) },
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.background,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 8. Asesor Cafetero IA
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)),
                shape = RoundedCornerShape(20.dp),
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.size(34.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "ASISTENTE INTELIGENTE",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                            Text(
                                text = "Asesor Cafetero IA",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    Text(
                        text = "Analiza tu punto de equilibrio por kg y recibe recomendaciones prácticas basadas en tus costos, Bolsa NY, TRM y las ofertas del mercado.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = onAskMarketAi,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Analizar costos")
                        }
                        OutlinedButton(
                            onClick = { onNavigate("agronomia") },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.Eco, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Agronomía IA")
                        }
                    }
                }
            }
        }

        // 9. Recent Multi-Module Activity
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(20.dp),
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Actividad reciente",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if (recentActivities.isEmpty()) {
                        Text(
                            text = "Tus registros aparecerán aquí cuando agregues información.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        recentActivities.forEachIndexed { idx, act ->
                            if (idx > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onNavigate(act.route) }
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = act.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = act.typeLabel,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Text(
                                    text = act.date,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardModuleSummaryCard(
    moduleTitle: String,
    kicker: String,
    icon: ImageVector,
    primaryMetric: String,
    secondaryMetric: String,
    detailText: String,
    actionLabel: String,
    tag: String,
    accentContainerColor: Color,
    accentContentColor: Color,
    onClick: () -> Unit,
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = accentContainerColor,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = moduleTitle,
                                tint = accentContentColor,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                    Column {
                        Text(
                            text = kicker,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                        Text(
                            text = moduleTitle,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Text(
                    text = primaryMetric,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.background,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Text(
                    text = secondaryMetric,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = detailText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onClick) {
                    Text(actionLabel)
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniPulseTile(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun StepBannerCard(
    number: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier.padding(15.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SagePale),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = ForestGreen)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(number, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun StatKpiCard(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    caption: String? = null,
) {
    val bg = if (highlighted) {
        Brush.linearGradient(listOf(ForestGreen, ForestGreenLight))
    } else null
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (highlighted) ForestGreen else MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(1.dp, if (highlighted) ForestGreenLight else MaterialTheme.colorScheme.outline),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (bg != null) Modifier.background(bg) else Modifier),
        ) {
            if (!highlighted) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Terracotta),
                )
            }
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(11.dp),
                    color = if (highlighted) WarmGold else WarmBeigeIcon,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (highlighted) ForestGreen else RoastedBronze,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(18.dp),
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (highlighted) Color(0xFFE9E5D5) else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (highlighted) Color.White else MaterialTheme.colorScheme.primary,
                )
                if (caption != null) {
                    Text(
                        text = caption,
                        style = MaterialTheme.typography.labelSmall,
                        color = WarmGold,
                    )
                }
            }
        }
    }
}

@Composable
fun GuideScreen(
    onGoHome: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            SectionHeader(
                kicker = "FOLLETO DE ORIENTACIÓN",
                title = "Plataforma para caficultores",
                subtitle = "Una guía breve para entender el propósito de la plataforma y comenzar a explorarla.",
            )
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ForestGreen),
                shape = RoundedCornerShape(24.dp),
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    AsyncDrawableImage(
                        resId = R.drawable.img_guide_illustration,
                        contentDescription = "Ilustración finca cafetera de San José de Albán",
                        contentScale = ContentScale.Crop,
                        maxDimPx = 960,
                        placeholderColor = ForestGreen,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                            .clip(RoundedCornerShape(18.dp)),
                    )
                    Text(
                        text = "UNA GUÍA PARA EMPEZAR",
                        style = MaterialTheme.typography.labelSmall,
                        color = WarmGold,
                    )
                    Text(
                        text = "Tu punto de partida en el mundo del café.",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                    )
                    Text(
                        text = "La Plataforma para caficultores es un espacio digital dirigido a quienes se dedican al cultivo de café. Este folleto te orienta para recorrer la plataforma y reconocer la información disponible, sin reemplazar las indicaciones de cada sección.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFE1EADC),
                    )
                    Button(
                        onClick = onGoHome,
                        colors = ButtonDefaults.buttonColors(containerColor = WarmGold, contentColor = ForestGreen),
                    ) {
                        Text("Explorar la plataforma")
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(17.dp))
                    }
                }
            }
        }
        item {
            Text(
                text = "¿PARA QUÉ SIRVE?",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
            )
            Text(
                text = "Una orientación para caficultores",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        item {
            val cards = listOf(
                Triple(
                    Icons.Default.CompassCalibration,
                    "Conocer el espacio",
                    "Identifica las secciones que aparecen en el menú y descubre qué información presenta cada una. Empieza por el resumen para tener una visión general."
                ),
                Triple(
                    Icons.Default.Book,
                    "Consultar con criterio",
                    "Lee las descripciones y las indicaciones de cada pantalla antes de utilizar sus opciones. Si encuentras datos de referencia, comprueba su fecha, fuente y unidad."
                ),
                Triple(
                    Icons.Default.TouchApp,
                    "¿Cómo funciona?",
                    "1. Abre el menú de navegación.\n2. Elige la sección que quieres consultar.\n3. Lee las instrucciones visibles en esa sección y utiliza solo las opciones que necesites.\n4. Vuelve al resumen para seguir explorando."
                ),
                Triple(
                    Icons.Default.HelpOutline,
                    "Antes de continuar",
                    "La disponibilidad de información y herramientas puede variar. Verifica lo que se muestra en pantalla y no des por hecho que una consulta equivale a una oferta, asesoría o trámite confirmado."
                ),
            )
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                cards.forEach { (icon, title, body) ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    ) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SagePale,
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = ForestGreen,
                                    modifier = Modifier
                                        .padding(9.dp)
                                        .size(22.dp),
                                )
                            }
                            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        // Restored original guide-footer summary banner
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = SagePale,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "En pocas palabras: esta guía te ayuda a orientarte dentro de la Plataforma para caficultores. Recorre el menú a tu ritmo y consulta cada sección para conocer su contenido.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ForestGreen,
                    )
                    Button(
                        onClick = onGoHome,
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreen, contentColor = Color.White),
                    ) {
                        Text("Ir al resumen")
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
