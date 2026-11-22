package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.LiveMarketSnapshot
import com.example.data.NominatimResult
import com.example.model.AppRole
import com.example.model.CompradorItem
import com.example.model.PrecioCafeRecord
import com.example.model.UiState
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FormModalDialog
import com.example.ui.components.NoticeBanner
import com.example.ui.components.SectionHeader
import com.example.ui.components.StateContainer
import com.example.ui.theme.DangerClay
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.WarmGold
import com.example.util.formatCop
import com.example.util.formatNumber
import com.example.util.todayIsoDate
import kotlin.math.abs
import kotlin.math.roundToLong

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PricesScreen(
    currentUserId: String,
    appRole: AppRole,
    compradoresState: UiState<List<CompradorItem>>,
    preciosCafeState: UiState<List<PrecioCafeRecord>>,
    liveMarket: LiveMarketSnapshot,
    isLoadingMarket: Boolean,
    isSaving: Boolean,
    onRefreshLiveMarket: () -> Unit,
    onSavePrecioCafe: (PrecioCafeRecord, () -> Unit) -> Unit,
    onDeletePrecioCafe: (String) -> Unit,
    onSaveComprador: (CompradorItem, () -> Unit) -> Unit,
    onDeleteComprador: (String) -> Unit,
    onGeocode: suspend (String) -> Result<NominatimResult?>,
    onAskMarketAi: () -> Unit,
    onNotify: (String) -> Unit,
) {
    val canRecordPrices = appRole.capabilities.recordCoffeePrices
    val canPublishOffers = appRole.capabilities.publishBuyerPrices
    val context = LocalContext.current
    var creatingOffer by remember { mutableStateOf(false) }
    var editingOffer by remember { mutableStateOf<CompradorItem?>(null) }
    var deleteOffer by remember { mutableStateOf<CompradorItem?>(null) }
    var buyerScope by remember(canPublishOffers) {
        mutableStateOf(if (canPublishOffers) "mios" else "todos")
    }

    var creatingPrecio by remember { mutableStateOf(false) }
    var editingPrecio by remember { mutableStateOf<PrecioCafeRecord?>(null) }
    var deletePrecio by remember { mutableStateOf<PrecioCafeRecord?>(null) }

    LaunchedEffect(Unit) {
        if (liveMarket.fetchedAt == "Sin consultar") {
            onRefreshLiveMarket()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            SectionHeader(
                kicker = "INTELIGENCIA DE MERCADO",
                title = "Precios del café y divisas",
                subtitle = "Sigue en vivo la Bolsa de Nueva York (KC=F), el dólar TRM Colombia, la publicación FNC y los precios de compradores.",
                action = {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (canRecordPrices) {
                            Button(
                                onClick = { creatingPrecio = true },
                                modifier = Modifier.testTag("add_price_history_button"),
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(17.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Registrar precio")
                            }
                        }
                        OutlinedButton(
                            onClick = onRefreshLiveMarket,
                            enabled = !isLoadingMarket,
                            modifier = Modifier.testTag("refresh_market_button"),
                        ) {
                            if (isLoadingMarket) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(17.dp))
                            }
                            Spacer(Modifier.width(6.dp))
                            Text("Actualizar bolsa y dólar")
                        }
                        if (canPublishOffers) {
                            OutlinedButton(
                                onClick = { creatingOffer = true },
                                modifier = Modifier.testTag("add_offer_button"),
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(17.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Publicar compra")
                            }
                        }
                        OutlinedButton(onClick = onAskMarketAi) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Analizar con IA")
                        }
                    }
                },
            )
        }
        if (!canRecordPrices && !canPublishOffers) {
            item {
                NoticeBanner(
                    "Modo lectura (${appRole.displayName}): puedes consultar en vivo Bolsa NY, TRM, FNC y precios de compradores sin publicar ofertas ni modificar registros."
                )
            }
        }

        // Live Market Cards: New York Futures (KC=F) + Colombia TRM (USD/COP)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "MERCADOS EN VIVO",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Text(
                        text = if (isLoadingMarket) "Consultando fuentes..." else "Actualizado: ${liveMarket.fetchedAt}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // NY Coffee C Futures Card
                LiveMarketTickerCard(
                    title = "Bolsa de Nueva York · Café C (${liveMarket.nySymbol})",
                    sourceLabel = liveMarket.nyExchange,
                    primaryValue = liveMarket.nyCoffeeCentsLb?.let { "${formatNumber(it, 2)} ¢/lb" }
                        ?: if (isLoadingMarket) "Consultando..." else "No disponible",
                    secondaryValue = liveMarket.nyUsdPerLb?.let { "Equivalente: US$ ${formatNumber(it, 2)} / libra" },
                    deltaValue = liveMarket.nyDeltaCents,
                    deltaPct = liveMarket.nyDeltaPct,
                    deltaUnitSuffix = "¢",
                    errorMessage = liveMarket.nyError,
                    icon = Icons.Default.TrendingUp,
                    testTagName = "ticker_ny_coffee",
                )

                // TRM USD/COP Card
                LiveMarketTickerCard(
                    title = "Dólar hoy en Colombia · TRM (USD/COP)",
                    sourceLabel = buildString {
                        append(liveMarket.trmSource)
                        if (!liveMarket.trmDate.isNullOrBlank()) {
                            append(" · Vigencia ${liveMarket.trmDate}")
                        }
                    },
                    primaryValue = liveMarket.trmCop?.let { "${formatCop(it)} / USD" }
                        ?: if (isLoadingMarket) "Consultando..." else "No disponible",
                    secondaryValue = liveMarket.trmPrevCop?.let { "Cierre anterior: ${formatCop(it)}" },
                    deltaValue = liveMarket.trmDeltaCop,
                    deltaPct = liveMarket.trmDeltaPct,
                    deltaUnitSuffix = " COP",
                    errorMessage = liveMarket.trmError,
                    icon = Icons.Default.AttachMoney,
                    testTagName = "ticker_usd_cop_trm",
                )

                // Theoretical Parity Reference (only when both live NY Futures and live TRM succeeded)
                val parityCarga = liveMarket.nyParityCopPerCarga125Kg
                val parityKg = liveMarket.nyParityCopPerKg
                if (parityCarga != null && parityKg != null) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "PARIDAD TEÓRICA INTERNACIONAL (BOLSA NY × TRM)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = "${formatCop(parityCarga)} / carga (125 kg verde) · ${formatCop(parityKg)} / kg",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = "Cálculo orientativo sobre café verde (125 kg = 275,58 lb) antes de prima, trilla y descuentos locales.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                }
            }
        }

        // Official FNC reference card
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
                            text = "Precio diario · FNC",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(50),
                        ) {
                            Text(
                                text = if (liveMarket.fncStatus.reachable) "Portal accesible" else "Consulta manual",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                    }
                    if (liveMarket.fncDetectedCargaCop != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column {
                                    Text(
                                        text = "Referencia detectada en FNC",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    )
                                    Text(
                                        text = "${formatCop(liveMarket.fncDetectedCargaCop)} / carga (125 kg)",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                                if (canRecordPrices) {
                                    OutlinedButton(onClick = { creatingPrecio = true }) {
                                        Text("Guardar hoy")
                                    }
                                }
                            }
                        }
                    }
                    Text(
                        text = liveMarket.fncStatus.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "Última verificación: ${liveMarket.fncStatus.checkedAt}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse("https://federaciondecafeteros.org/publicaciones/"))
                                )
                            }
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Abrir boletín FNC")
                        }
                        if (canRecordPrices) {
                            OutlinedButton(onClick = { creatingPrecio = true }) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Guardar precio del día")
                            }
                        }
                    }
                }
            }
        }

        // Price History Card (Firestore)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.testTag("price_history_section"),
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "Historial de precios del café",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        if (canRecordPrices) {
                            OutlinedButton(onClick = { creatingPrecio = true }) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Nuevo")
                            }
                        }
                    }
                    Text(
                        text = if (canRecordPrices) {
                            "Bitácora histórica privada sincronizada en la nube con cotización de Bolsa NY y TRM de cada jornada."
                        } else {
                            "Historial personal de precios disponible para cuentas Caficultor y Comprador."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    StateContainer(uiState = preciosCafeState) { list ->
                        if (list.isEmpty()) {
                            EmptyStateCard(
                                title = "Sin historial de precios aún",
                                subtitle = if (canRecordPrices) {
                                    "Registra el precio del día (COP/carga o COP/kg). Se adjuntará automáticamente el valor en vivo de Bolsa NY y TRM."
                                } else {
                                    "Tu rol (${appRole.displayName}) solo consulta precios de mercado en vivo y ofertas publicadas."
                                },
                                icon = Icons.Default.History,
                                actionLabel = if (canRecordPrices) "Registrar precio de hoy" else null,
                                onAction = if (canRecordPrices) {
                                    { creatingPrecio = true }
                                } else null,
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                list.forEachIndexed { idx, record ->
                                    if (idx > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    val prevRecord = list.getOrNull(idx + 1)
                                    val diffCarga = prevRecord?.let { record.pricePerCargaCop - it.pricePerCargaCop }
                                    PrecioCafeHistoryRow(
                                        record = record,
                                        diffCarga = diffCarga,
                                        canModify = canRecordPrices && record.userId == currentUserId,
                                        onEdit = { editingPrecio = record },
                                        onDelete = { deletePrecio = record },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            NoticeBanner(
                "El precio de referencia no es una oferta garantizada. Verifica fecha, factor de rendimiento, humedad, descuentos y transporte directamente con cada comprador."
            )
        }

        // User-submitted buyer offers comparator
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
                            text = "Precios diarios de compradores",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                        )
                        if (canPublishOffers) {
                            OutlinedButton(onClick = { creatingOffer = true }) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Publicar")
                            }
                        }
                    }
                    Text(
                        text = if (canPublishOffers) {
                            "Publica y actualiza tus precios diarios de compra por carga o kilogramo para los caficultores."
                        } else {
                            "Los compradores publican aquí sus precios diarios de compra; compara todas las plazas vigentes (solo lectura)."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    StateContainer(uiState = compradoresState) { list ->
                        val allPriced = list.filter { it.offerAmountCop > 0.0 }
                            .sortedByDescending { it.offerAmountCop }
                        val myPricedCount = allPriced.count { it.userId == currentUserId }
                        val displayedOffers = if (canPublishOffers && buyerScope == "mios") {
                            allPriced.filter { it.userId == currentUserId }
                        } else {
                            allPriced
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            FilterChip(
                                selected = buyerScope == "todos" || !canPublishOffers,
                                onClick = { buyerScope = "todos" },
                                label = { Text("Todos los compradores (${allPriced.size})") },
                                modifier = Modifier.testTag("buyers_scope_all"),
                            )
                            if (canPublishOffers) {
                                FilterChip(
                                    selected = buyerScope == "mios",
                                    onClick = { buyerScope = "mios" },
                                    label = { Text("Mis precios publicados ($myPricedCount)") },
                                    modifier = Modifier.testTag("buyers_scope_mine"),
                                )
                            }
                        }
                        if (displayedOffers.isEmpty()) {
                            EmptyStateCard(
                                title = if (canPublishOffers && buyerScope == "mios") "No has publicado precios de compra" else "Sin ofertas con precio aún",
                                subtitle = if (canPublishOffers) {
                                    "Publica tu precio diario por carga o kilogramo para que aparezca en el comparador."
                                } else {
                                    "Aún no hay precios diarios publicados por compradores."
                                },
                                icon = Icons.Default.TrendingUp,
                                actionLabel = if (canPublishOffers) "Publicar precio de compra" else null,
                                onAction = if (canPublishOffers) {
                                    { creatingOffer = true }
                                } else null,
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                displayedOffers.forEachIndexed { idx, offer ->
                                    if (idx > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    val isOwner = offer.userId == currentUserId
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text(offer.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                            Text(
                                                text = formatCop(offer.offerAmountCop),
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.primary,
                                            )
                                        }
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Surface(
                                                color = if (isOwner) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                                                shape = RoundedCornerShape(50),
                                            ) {
                                                Text(
                                                    text = if (isOwner) "Mi precio publicado" else "Comprador local",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                )
                                            }
                                            if (offer.priceDate.isNotBlank()) {
                                                Text(
                                                    text = "Vigencia: ${offer.priceDate}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                        }
                                        Text(
                                            text = buildString {
                                                append("Por ${offer.priceUnit}")
                                                if (offer.purchaseType.isNotBlank()) append(" · ${offer.purchaseType}")
                                                append(" · ${offer.location.ifBlank { "Albán, Nariño" }}")
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.secondary,
                                        )
                                        if (offer.details.isNotBlank()) {
                                            Text(
                                                text = offer.details,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        if (isOwner && canPublishOffers) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                OutlinedButton(onClick = { editingOffer = offer }) {
                                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                                                    Spacer(Modifier.width(4.dp))
                                                    Text("Editar")
                                                }
                                                OutlinedButton(
                                                    onClick = { deleteOffer = offer },
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerClay),
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(15.dp))
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

        // External sources
        item {
            val sources = listOf(
                Triple(
                    "Federación Nacional de Cafeteros",
                    "Referencia de precio interno de compra y noticias del mercado cafetero colombiano.",
                    "https://federaciondecafeteros.org/publicaciones/" to Icons.Default.Coffee,
                ),
                Triple(
                    "Bolsa de Nueva York · ICE",
                    "Información sobre el mercado de futuros del café arábica y sus contratos (Contrato C).",
                    "https://www.ice.com/products/15/Coffee-C-Futures" to Icons.Default.TrendingUp,
                ),
                Triple(
                    "Organización Internacional del Café",
                    "Estadísticas, informes y panorama internacional del sector cafetero.",
                    "https://www.ico.org/" to Icons.Default.Public,
                ),
                Triple(
                    "Banco de la República / Datos Abiertos TRM",
                    "Histórico y tasa representativa del mercado (USD/COP) vigente en Colombia.",
                    "https://www.datos.gov.co/Econom-a-y-Finanzas/Tasa-de-Cambio-Representativa-del-Mercado-TRM/32sa-8pi3" to Icons.Default.AttachMoney,
                ),
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                sources.forEach { (title, desc, urlAndIcon) ->
                    val (url, icon) = urlAndIcon
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text(title, style = MaterialTheme.typography.titleMedium)
                            }
                            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            OutlinedButton(
                                onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                            ) {
                                Text("Visitar fuente")
                                Spacer(Modifier.width(6.dp))
                                Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(15.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // Price History Create / Edit Modal
    val targetPrecio = editingPrecio ?: if (creatingPrecio) {
        PrecioCafeRecord(
            date = todayIsoDate(),
            source = "FNC",
            nyFuturesCentsLb = liveMarket.nyCoffeeCentsLb ?: 0.0,
            usdCopTrm = liveMarket.trmCop ?: 0.0,
        )
    } else null

    if (targetPrecio != null) {
        PrecioCafeFormDialog(
            initial = targetPrecio,
            isEdit = editingPrecio != null,
            isSaving = isSaving,
            liveMarket = liveMarket,
            onDismiss = {
                editingPrecio = null
                creatingPrecio = false
            },
            onSave = { updated ->
                onSavePrecioCafe(updated) {
                    editingPrecio = null
                    creatingPrecio = false
                }
            },
        )
    }

    deletePrecio?.let { item ->
        ConfirmDeleteDialog(
            itemName = "${item.source} · ${item.date}",
            onConfirm = {
                onDeletePrecioCafe(item.id)
                deletePrecio = null
            },
            onDismiss = { deletePrecio = null },
        )
    }

    // Buyer Offer Create / Edit Modal
    val targetOffer = editingOffer ?: if (creatingOffer) CompradorItem(actorType = "comprador") else null
    if (targetOffer != null) {
        CompradorFormDialog(
            initial = targetOffer,
            isEdit = editingOffer != null,
            isSaving = isSaving,
            onGeocode = onGeocode,
            onNotify = onNotify,
            onDismiss = {
                editingOffer = null
                creatingOffer = false
            },
            onSave = { updated ->
                onSaveComprador(updated) {
                    editingOffer = null
                    creatingOffer = false
                }
            },
        )
    }

    deleteOffer?.let { offer ->
        ConfirmDeleteDialog(
            itemName = offer.name,
            onConfirm = {
                onDeleteComprador(offer.id)
                deleteOffer = null
            },
            onDismiss = { deleteOffer = null },
        )
    }
}

@Composable
private fun LiveMarketTickerCard(
    title: String,
    sourceLabel: String,
    primaryValue: String,
    secondaryValue: String?,
    deltaValue: Double?,
    deltaPct: Double?,
    deltaUnitSuffix: String,
    errorMessage: String?,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    testTagName: String,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTagName),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.size(34.dp),
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(7.dp),
                        )
                    }
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = sourceLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = primaryValue,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (deltaValue != null && deltaPct != null) {
                    val isUp = deltaValue >= 0.0
                    val bgColor = if (isUp) EmeraldAccent.copy(alpha = 0.14f) else DangerClay.copy(alpha = 0.14f)
                    val fgColor = if (isUp) ForestGreen else DangerClay
                    Surface(color = bgColor, shape = RoundedCornerShape(50)) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = if (isUp) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                contentDescription = null,
                                tint = fgColor,
                                modifier = Modifier.size(14.dp),
                            )
                            val sign = if (isUp) "+" else "-"
                            Text(
                                text = "$sign${formatNumber(abs(deltaValue), 2)}$deltaUnitSuffix ($sign${formatNumber(abs(deltaPct), 2)}%)",
                                style = MaterialTheme.typography.labelSmall,
                                color = fgColor,
                            )
                        }
                    }
                }
            }

            if (!secondaryValue.isNullOrBlank()) {
                Text(
                    text = secondaryValue,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!errorMessage.isNullOrBlank()) {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = DangerClay,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PrecioCafeHistoryRow(
    record: PrecioCafeRecord,
    diffCarga: Double?,
    canModify: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(50),
                    ) {
                        Text(
                            text = record.source,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                    Text(
                        text = record.date,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = "${formatCop(record.pricePerCargaCop)} / carga (125 kg)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "${formatCop(record.pricePerKgCop)} / kg · Código ${record.recordCode}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }

            if (diffCarga != null && diffCarga != 0.0) {
                val isUp = diffCarga > 0.0
                val bgColor = if (isUp) EmeraldAccent.copy(alpha = 0.14f) else DangerClay.copy(alpha = 0.14f)
                val fgColor = if (isUp) ForestGreen else DangerClay
                Surface(color = bgColor, shape = RoundedCornerShape(50)) {
                    Text(
                        text = "${if (isUp) "+" else "-"}${formatCop(abs(diffCarga))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = fgColor,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                    )
                }
            }
        }

        if (record.nyFuturesCentsLb > 0.0 || record.usdCopTrm > 0.0) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (record.nyFuturesCentsLb > 0.0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.background,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    ) {
                        Text(
                            text = "Bolsa NY: ${formatNumber(record.nyFuturesCentsLb, 2)} ¢/lb",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
                if (record.usdCopTrm > 0.0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.background,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    ) {
                        Text(
                            text = "TRM: ${formatCop(record.usdCopTrm)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        }

        if (record.notes.isNotBlank()) {
            Text(
                text = record.notes,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (canModify) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Editar")
                }
                OutlinedButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerClay),
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Eliminar")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PrecioCafeFormDialog(
    initial: PrecioCafeRecord,
    isEdit: Boolean,
    isSaving: Boolean,
    liveMarket: LiveMarketSnapshot,
    onDismiss: () -> Unit,
    onSave: (PrecioCafeRecord) -> Unit,
) {
    var date by remember { mutableStateOf(initial.date.ifBlank { todayIsoDate() }) }
    var source by remember { mutableStateOf(initial.source.ifBlank { "FNC" }) }
    var cargaText by remember {
        mutableStateOf(
            if (initial.pricePerCargaCop > 0.0) initial.pricePerCargaCop.roundToLong().toString() else ""
        )
    }
    var kgText by remember {
        mutableStateOf(
            if (initial.pricePerKgCop > 0.0) initial.pricePerKgCop.roundToLong().toString() else ""
        )
    }
    var nyText by remember {
        mutableStateOf(
            if (initial.nyFuturesCentsLb > 0.0) String.format(java.util.Locale.US, "%.2f", initial.nyFuturesCentsLb) else ""
        )
    }
    var trmText by remember {
        mutableStateOf(
            if (initial.usdCopTrm > 0.0) String.format(java.util.Locale.US, "%.2f", initial.usdCopTrm) else ""
        )
    }
    var notes by remember { mutableStateOf(initial.notes) }

    val cargaVal = cargaText.replace(",", ".").toDoubleOrNull() ?: 0.0
    val kgVal = kgText.replace(",", ".").toDoubleOrNull() ?: 0.0
    val isValid = date.isNotBlank() && source.isNotBlank() && cargaVal > 0.0 && kgVal > 0.0

    FormModalDialog(
        title = if (isEdit) "Editar precio del café" else "Registrar precio del café",
        subtitle = "Registra el valor de la carga (125 kg) o por kilo. Se guarda junto a la cotización NY y TRM.",
        onDismiss = onDismiss,
        confirmLabel = if (isEdit) "Actualizar" else "Guardar precio",
        confirmEnabled = isValid && !isSaving,
        onConfirm = {
            onSave(
                initial.copy(
                    date = date.trim(),
                    source = source.trim(),
                    pricePerCargaCop = cargaVal,
                    pricePerKgCop = kgVal,
                    nyFuturesCentsLb = nyText.replace(",", ".").toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0,
                    usdCopTrm = trmText.replace(",", ".").toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0,
                    notes = notes.trim(),
                )
            )
        },
    ) {
        Text("Fuente de referencia", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            listOf("FNC", "Cooperativa Albán", "Comprador local", "Referencia propia").forEach { option ->
                FilterChip(
                    selected = source == option,
                    onClick = { source = option },
                    label = { Text(option) },
                )
            }
        }
        OutlinedTextField(
            value = date,
            onValueChange = { date = it },
            label = { Text("Fecha (AAAA-MM-DD)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = cargaText,
            onValueChange = { raw ->
                cargaText = raw
                val parsed = raw.replace(",", ".").toDoubleOrNull()
                if (parsed != null && parsed > 0.0) {
                    kgText = (parsed / 125.0).roundToLong().toString()
                }
            },
            label = { Text("Precio por carga (125 kg) COP *") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("precio_carga_input"),
        )
        OutlinedTextField(
            value = kgText,
            onValueChange = { raw ->
                kgText = raw
                val parsed = raw.replace(",", ".").toDoubleOrNull()
                if (parsed != null && parsed > 0.0) {
                    cargaText = (parsed * 125.0).roundToLong().toString()
                }
            },
            label = { Text("Precio por kilogramo COP *") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("precio_kg_input"),
        )

        if (liveMarket.fncDetectedCargaCop != null || liveMarket.nyParityCopPerCarga125Kg != null) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                liveMarket.fncDetectedCargaCop?.let { fncPrice ->
                    OutlinedButton(onClick = {
                        cargaText = fncPrice.roundToLong().toString()
                        kgText = (fncPrice / 125.0).roundToLong().toString()
                        source = "FNC"
                    }) {
                        Text("Usar FNC (${formatCop(fncPrice)})")
                    }
                }
                liveMarket.nyParityCopPerCarga125Kg?.let { parity ->
                    OutlinedButton(onClick = {
                        cargaText = parity.roundToLong().toString()
                        kgText = (parity / 125.0).roundToLong().toString()
                    }) {
                        Text("Usar paridad NY×TRM")
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = nyText,
                onValueChange = { nyText = it },
                label = { Text("Bolsa NY (¢/lb)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = trmText,
                onValueChange = { trmText = it },
                label = { Text("Dólar TRM (COP)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notas (factor de rendimiento, humedad, calidad)") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun SchoolScreen() {
    val context = LocalContext.current
    fun openUrl(url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            SectionHeader(
                kicker = "FORMACIÓN PARA EL CAMPO",
                title = "Escuela Cafetera",
                subtitle = "Cursos, capacitaciones y canales oficiales para consultar la agenda de la Federación Nacional de Cafeteros.",
            )
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ForestGreen),
                shape = RoundedCornerShape(22.dp),
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(color = WarmGold, shape = RoundedCornerShape(50)) {
                        Text(
                            text = "APRENDER PARA CULTIVAR MEJOR",
                            style = MaterialTheme.typography.labelSmall,
                            color = ForestGreen,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                    Text("Conocimiento que llega a tu finca", style = MaterialTheme.typography.headlineSmall, color = Color.White)
                    Text(
                        "Encuentra oportunidades de aprendizaje publicadas por la Federación y sus comités. Comprueba requisitos, cupos y fechas en cada fuente oficial.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFE2EBDF),
                    )
                }
            }
        }
        item {
            val courses = listOf(
                Triple(
                    "Educación virtual para familias cafeteras",
                    "Cursos gratuitos sobre finanzas del hogar y la finca, liderazgo y seguridad en el trabajo.",
                    "https://federaciondecafeteros.org/educacion-virtual-para-las-familias-caficultoras/" to Icons.Default.Book,
                ),
                Triple(
                    "CaféU · formación por WhatsApp",
                    "Capacitaciones sobre cosecha asistida y fertilización. Revisa requisitos con tu comité cafetero.",
                    "https://caldas.federaciondecafeteros.org/listado-noticias/destacado-informativo/" to Icons.Default.PhoneAndroid,
                ),
                Triple(
                    "Cursos cortos de café · El Agrado",
                    "Formación en filtrado, catación, tostión y barismo. Confirma sedes, precios y horarios.",
                    "https://quindio.federaciondecafeteros.org/cursos-cortos-agrado/" to Icons.Default.Coffee,
                ),
            )
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                courses.forEach { (title, desc, pair) ->
                    val (url, icon) = pair
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    ) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            OutlinedButton(onClick = { openUrl(url) }) {
                                Text("Consultar información")
                                Spacer(Modifier.width(6.dp))
                                Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(15.dp))
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
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Agenda de la Federación y Nariño", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                    }
                    Text(
                        "Consulta la programación departamental y municipal en Nariño directamente en los portales oficiales.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { openUrl("https://federaciondecafeteros.org/") }, modifier = Modifier.weight(1f)) {
                            Text("Federación")
                        }
                        OutlinedButton(onClick = { openUrl("https://narino.federaciondecafeteros.org/") }, modifier = Modifier.weight(1f)) {
                            Text("Comité Nariño")
                        }
                    }
                }
            }
        }
    }
}
