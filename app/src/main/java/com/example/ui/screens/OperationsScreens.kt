package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.AppRole
import com.example.model.CosechaRecord
import com.example.model.CostoCalculation
import com.example.model.CostoLine
import com.example.model.CultivoActivity
import com.example.model.FincaProfile
import com.example.model.InquietudTecnica
import com.example.model.RecolectorPostulacion
import com.example.model.UiState
import com.example.model.UserProfile
import com.example.model.VisitaAgronomica
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FarmDropdownField
import com.example.ui.components.FormModalDialog
import com.example.ui.components.NoticeBanner
import com.example.ui.components.SectionHeader
import com.example.ui.components.StateContainer
import com.example.ui.theme.DangerClay
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.SagePale
import com.example.ui.theme.WarmGold
import com.example.util.formatCop
import com.example.util.formatNumber
import com.example.util.todayIsoDate
import com.example.viewmodel.CalculatorDraft

private val costCategories = listOf("Insumos", "Maquinaria", "Mano de obra", "Transporte", "Beneficio", "Otros")

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CostsCalculatorScreen(
    currentUserId: String,
    appRole: AppRole,
    draft: CalculatorDraft,
    fincasState: UiState<List<FincaProfile>>,
    isSaving: Boolean,
    onUpdateMeta: (
        name: String?,
        farmCode: String?,
        farmName: String?,
        estimatedKg: String?,
        offerPricePerKg: String?,
    ) -> Unit,
    onAddLine: () -> Unit,
    onUpdateLine: (Int, CostoLine) -> Unit,
    onRemoveLine: (Int) -> Unit,
    onSave: () -> Unit,
    onReset: () -> Unit,
    onOpenHistory: () -> Unit,
    onAskCostsAi: () -> Unit,
) {
    val canCalculate = appRole.capabilities.calculateOwnCosts
    val farms = (fincasState as? UiState.Success)?.data.orEmpty()
        .filter { it.userId == currentUserId }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            SectionHeader(
                kicker = "FINANZAS DE LA FINCA",
                title = "Calculadora de costos",
                subtitle = "Desglosa tus gastos por partida, estima tu costo real por kilogramo y evalúa si una oferta cubre tu esfuerzo.",
                action = {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = onOpenHistory,
                            modifier = Modifier.testTag("open_cost_history_button"),
                        ) {
                            Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Ver historial")
                        }
                        OutlinedButton(
                            onClick = onAskCostsAi,
                            modifier = Modifier.testTag("ask_costs_ai_button"),
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Analizar con IA")
                        }
                    }
                },
            )
        }

        if (!canCalculate) {
            item {
                NoticeBanner(
                    "Módulo exclusivo de caficultor: tu rol permanente (${appRole.displayName}) puede simular cálculos temporalmente o consultar el análisis IA, pero no guarda registros de costos en fincas."
                )
            }
        }

        // Live KPI Summary Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ForestGreen),
                shape = RoundedCornerShape(20.dp),
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (draft.editingId.isBlank()) "Resumen en vivo" else "Editando cálculo guardado",
                            style = MaterialTheme.typography.labelMedium,
                            color = WarmGold,
                        )
                        if (draft.editingId.isNotBlank()) {
                            Surface(
                                color = Color.White.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(50),
                            ) {
                                Text(
                                    text = draft.recordCode,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Costo total", style = MaterialTheme.typography.bodySmall, color = Color(0xFFDDE7DA))
                            Text(
                                text = formatCop(draft.totalAmount),
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Costo por kg", style = MaterialTheme.typography.bodySmall, color = Color(0xFFDDE7DA))
                            Text(
                                text = if (draft.kgValue > 0.0) formatCop(draft.costPerKg) else "—",
                                style = MaterialTheme.typography.headlineSmall,
                                color = WarmGold,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    // Desglose por rubro (como en plataforma inicial)
                    HorizontalDivider(color = Color.White.copy(alpha = 0.16f))
                    costCategories.forEach { cat ->
                        val catTotal = draft.lines.filter { it.category == cat }.sumOf { it.subtotal }
                        if (catTotal > 0.0 || cat in listOf("Insumos", "Maquinaria", "Mano de obra", "Otros")) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(cat, style = MaterialTheme.typography.bodySmall, color = Color(0xFFDDE7DA))
                                Text(
                                    text = formatCop(catTotal),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }

                    val profit = draft.estimatedProfit
                    HorizontalDivider(color = Color.White.copy(alpha = 0.18f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (profit != null && profit < 0.0) "Margen negativo" else "Ganancia estimada",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFDDE7DA),
                        )
                        Text(
                            text = if (profit != null && draft.kgValue > 0.0) formatCop(profit) else "Ingresa una oferta",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                profit == null || draft.kgValue <= 0.0 -> Color(0xFFE9E5D5)
                                profit >= 0.0 -> Color(0xFFA7F3D0)
                                else -> Color(0xFFFCA5A5)
                            },
                        )
                    }
                }
            }
        }

        // General Info Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Datos generales del lote o cálculo",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    OutlinedTextField(
                        value = draft.name,
                        onValueChange = { onUpdateMeta(it, null, null, null, null) },
                        label = { Text("Nombre del cálculo *") },
                        placeholder = { Text("Ej. Cosecha principal lote El Mirador") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("cost_name_input"),
                    )
                    FarmDropdownField(
                        farms = farms,
                        selectedCode = draft.farmCode,
                        label = "Finca asociada (opcional)",
                        allowEmpty = true,
                        onSelected = { code, name ->
                            onUpdateMeta(null, code, name, null, null)
                        },
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = draft.estimatedKg,
                            onValueChange = { onUpdateMeta(null, null, null, it, null) },
                            label = { Text("Kg estimados") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("cost_kg_input"),
                        )
                        OutlinedTextField(
                            value = draft.offerPricePerKg,
                            onValueChange = { onUpdateMeta(null, null, null, null, it) },
                            label = { Text("Oferta $/kg (opc.)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("cost_offer_input"),
                        )
                    }
                }
            }
        }

        // Expense Lines Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Partidas de gasto (${draft.lines.size})",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                OutlinedButton(
                    onClick = onAddLine,
                    modifier = Modifier.testTag("add_cost_line_button"),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Añadir partida")
                }
            }
        }

        items(draft.lines.size, key = { idx -> "line_$idx" }) { index ->
            val line = draft.lines[index]
            CostLineEditorCard(
                index = index,
                line = line,
                canDelete = draft.lines.size > 1,
                onChange = { onUpdateLine(index, it) },
                onRemove = { onRemoveLine(index) },
            )
        }

        // Action Buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onReset,
                    enabled = !isSaving,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("reset_cost_button"),
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Limpiar")
                }
                if (canCalculate) {
                    Button(
                        onClick = onSave,
                        enabled = !isSaving,
                        modifier = Modifier
                            .weight(1.6f)
                            .height(50.dp)
                            .testTag("save_cost_button"),
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (draft.editingId.isBlank()) "Guardar cálculo" else "Actualizar cálculo")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CostLineEditorCard(
    index: Int,
    line: CostoLine,
    canDelete: Boolean,
    onChange: (CostoLine) -> Unit,
    onRemove: () -> Unit,
) {
    var categoryMenuOpen by remember { mutableStateOf(false) }
    var qtyText by remember(index, line.qty) {
        mutableStateOf(if (line.qty == 0.0) "" else if (line.qty % 1.0 == 0.0) line.qty.toInt().toString() else line.qty.toString())
    }
    var unitText by remember(index, line.unit) {
        mutableStateOf(if (line.unit == 0.0) "" else if (line.unit % 1.0 == 0.0) line.unit.toInt().toString() else line.unit.toString())
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Partida #${index + 1} · Subtotal: ${formatCop(line.subtotal)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (canDelete) {
                    IconButton(onClick = onRemove) {
                        Icon(Icons.Default.Delete, contentDescription = "Quitar partida", tint = DangerClay)
                    }
                }
            }
            ExposedDropdownMenuBox(
                expanded = categoryMenuOpen,
                onExpandedChange = { categoryMenuOpen = !categoryMenuOpen },
            ) {
                OutlinedTextField(
                    value = line.category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Categoría") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryMenuOpen) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                )
                ExposedDropdownMenu(
                    expanded = categoryMenuOpen,
                    onDismissRequest = { categoryMenuOpen = false },
                ) {
                    costCategories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = {
                                onChange(line.copy(category = cat))
                                categoryMenuOpen = false
                            },
                        )
                    }
                }
            }
            OutlinedTextField(
                value = line.item,
                onValueChange = { onChange(line.copy(item = it)) },
                label = { Text("Concepto o actividad *") },
                placeholder = { Text("Ej. Bulto fertilizante, jornales") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = qtyText,
                    onValueChange = { raw ->
                        qtyText = raw
                        val parsed = raw.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
                        onChange(line.copy(qty = parsed))
                    },
                    label = { Text("Cantidad") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = unitText,
                    onValueChange = { raw ->
                        unitText = raw
                        val parsed = raw.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
                        onChange(line.copy(unit = parsed))
                    },
                    label = { Text("Valor unitario COP") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1.4f),
                )
            }
        }
    }
}

@Composable
fun CostsHistoryScreen(
    appRole: AppRole,
    costosState: UiState<List<CostoCalculation>>,
    onReopenInCalculator: (CostoCalculation) -> Unit,
    onDelete: (String) -> Unit,
    onNewCalculation: () -> Unit,
) {
    val canCalculate = appRole.capabilities.calculateOwnCosts
    var search by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<CostoCalculation?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            SectionHeader(
                kicker = "FINANZAS DE LA FINCA",
                title = "Historial de costos",
                subtitle = "Consulta tus cálculos guardados, reábrelos en la calculadora para actualizarlos o compara lotes.",
                action = if (canCalculate) {
                    {
                        Button(
                            onClick = onNewCalculation,
                            modifier = Modifier.testTag("history_new_calc_button"),
                        ) {
                            Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Ir a calculadora")
                        }
                    }
                } else null,
            )
        }
        if (!canCalculate) {
            item {
                NoticeBanner("El historial privado de costos de producción corresponde exclusivamente al rol Caficultor.")
            }
        }
        item {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text("Buscar por nombre, finca o código...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            StateContainer(uiState = costosState) { list ->
                val filtered = list.filter { item ->
                    val q = search.trim().lowercase()
                    q.isEmpty() || item.name.lowercase().contains(q) ||
                        item.farmName.lowercase().contains(q) ||
                        item.recordCode.lowercase().contains(q)
                }
                if (filtered.isEmpty()) {
                    EmptyStateCard(
                        title = "Aún no hay cálculos guardados",
                        subtitle = if (canCalculate) {
                            "Crea un desglose en la calculadora de costos y guárdalo para tener tu historial."
                        } else {
                            "Este espacio almacena cálculos privados de costos del rol Caficultor."
                        },
                        icon = Icons.Default.History,
                        actionLabel = if (canCalculate) "Crear cálculo" else null,
                        onAction = if (canCalculate) onNewCalculation else null,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        filtered.forEach { calc ->
                            val costPerKg = if (calc.estimatedKg > 0.0) calc.totalAmount / calc.estimatedKg else 0.0
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = RoundedCornerShape(50),
                                        ) {
                                            Text(
                                                text = calc.recordCode,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            )
                                        }
                                        Text(
                                            text = calc.date,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Text(
                                        text = calc.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    if (calc.farmName.isNotBlank() || calc.farmCode.isNotBlank()) {
                                        Text(
                                            text = "Finca: ${calc.farmName.ifBlank { calc.farmCode }}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Column {
                                            Text("Total", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(formatCop(calc.totalAmount), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = if (calc.estimatedKg > 0.0) "${formatNumber(calc.estimatedKg)} kg est." else "Sin kg",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                            Text(
                                                text = if (costPerKg > 0.0) "${formatCop(costPerKg)}/kg" else "—",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.secondary,
                                            )
                                        }
                                    }
                                    if (calc.lines.isNotEmpty()) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                        calc.lines.take(4).forEach { ln ->
                                            Text(
                                                text = "• ${ln.category}: ${ln.item} (${formatCop(ln.subtotal)})",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        if (calc.lines.size > 4) {
                                            Text(
                                                text = "+ ${calc.lines.size - 4} partidas adicionales",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.secondary,
                                            )
                                        }
                                    }
                                    if (canCalculate) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedButton(
                                                onClick = { onReopenInCalculator(calc) },
                                                modifier = Modifier.weight(1f),
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(6.dp))
                                                Text("Reabrir")
                                            }
                                            OutlinedButton(
                                                onClick = { deleteTarget = calc },
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerClay),
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Eliminar cálculo", modifier = Modifier.size(16.dp))
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

@Composable
fun HarvestScreen(
    currentUserId: String,
    appRole: AppRole,
    cosechasState: UiState<List<CosechaRecord>>,
    fincasState: UiState<List<FincaProfile>>,
    isSaving: Boolean,
    onSave: (CosechaRecord, () -> Unit) -> Unit,
    onDelete: (String) -> Unit,
) {
    val canRecordHarvest = appRole.capabilities.recordOwnHarvests
    val farms = (fincasState as? UiState.Success)?.data.orEmpty()
        .filter { it.userId == currentUserId }
    val records = (cosechasState as? UiState.Success)?.data.orEmpty()
    val totalKg = records.sumOf { it.quantityKg }

    var search by remember { mutableStateOf("") }
    var creating by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<CosechaRecord?>(null) }
    var deleteTarget by remember { mutableStateOf<CosechaRecord?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            SectionHeader(
                kicker = "OPERACIÓN EN CAMPO",
                title = "Registro de cosecha",
                subtitle = "Controla los pesajes diarios por trabajador y finca para consolidar tu producción.",
                action = if (canRecordHarvest) {
                    {
                        Button(
                            onClick = { creating = true },
                            modifier = Modifier.testTag("add_harvest_button"),
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Registrar pesaje")
                        }
                    }
                } else null,
            )
        }
        if (!canRecordHarvest) {
            item {
                NoticeBanner("El registro de pesajes de cosecha corresponde exclusivamente al rol Caficultor.")
            }
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ForestGreen),
                shape = RoundedCornerShape(18.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("Total acumulado", style = MaterialTheme.typography.bodySmall, color = Color(0xFFDDE7DA))
                        Text(
                            text = "${formatNumber(totalKg)} kg",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }
                    Surface(color = WarmGold, shape = RoundedCornerShape(12.dp)) {
                        Text(
                            text = "${records.size} pesajes",
                            style = MaterialTheme.typography.labelLarge,
                            color = ForestGreen,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }
        item {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text("Buscar por trabajador, finca o fecha...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            StateContainer(uiState = cosechasState) { list ->
                val filtered = list.filter { item ->
                    val q = search.trim().lowercase()
                    q.isEmpty() || item.workerName.lowercase().contains(q) ||
                        item.farmName.lowercase().contains(q) ||
                        item.farmCode.lowercase().contains(q) ||
                        item.date.contains(q)
                }
                if (filtered.isEmpty()) {
                    EmptyStateCard(
                        title = "Sin pesajes de cosecha",
                        subtitle = if (canRecordHarvest) {
                            "Registra el primer pesaje indicando trabajador, finca y kilogramos recolectados."
                        } else {
                            "Los pesajes privados de cosecha pertenecen al rol Caficultor."
                        },
                        icon = Icons.Default.Scale,
                        actionLabel = if (canRecordHarvest) "Registrar pesaje" else null,
                        onAction = if (canRecordHarvest) ({ creating = true }) else null,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        filtered.forEach { rec ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = rec.workerName,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                        Text(
                                            text = "${formatNumber(rec.quantityKg)} kg",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondary,
                                        )
                                    }
                                    Text(
                                        text = "${rec.farmName.ifBlank { rec.farmCode }} · ${rec.date} · ${rec.recordCode}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    if (rec.details.isNotBlank()) {
                                        Text(rec.details, style = MaterialTheme.typography.bodySmall)
                                    }
                                    if (canRecordHarvest) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedButton(onClick = { editing = rec }, modifier = Modifier.weight(1f)) {
                                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(6.dp))
                                                Text("Editar")
                                            }
                                            OutlinedButton(
                                                onClick = { deleteTarget = rec },
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerClay),
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Eliminar pesaje", modifier = Modifier.size(16.dp))
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

    if (creating || editing != null) {
        val base = editing ?: CosechaRecord(date = todayIsoDate())
        var workerName by remember(base.id) { mutableStateOf(base.workerName) }
        var farmCode by remember(base.id) { mutableStateOf(base.farmCode) }
        var farmName by remember(base.id) { mutableStateOf(base.farmName) }
        var date by remember(base.id) { mutableStateOf(base.date.ifBlank { todayIsoDate() }) }
        var qtyText by remember(base.id) { mutableStateOf(if (base.quantityKg > 0.0) base.quantityKg.toString() else "") }
        var details by remember(base.id) { mutableStateOf(base.details) }
        var formError by remember(base.id) { mutableStateOf<String?>(null) }

        FormModalDialog(
            title = if (editing == null) "Nuevo pesaje de cosecha" else "Editar pesaje",
            onDismiss = {
                creating = false
                editing = null
            },
            confirmEnabled = !isSaving,
            onConfirm = {
                val qty = qtyText.toDoubleOrNull() ?: 0.0
                when {
                    workerName.isBlank() -> formError = "Ingresa el nombre del recolector o trabajador."
                    farmCode.isBlank() -> formError = "Selecciona una finca o caficultor."
                    qty <= 0.0 -> formError = "Ingresa una cantidad en kg mayor que cero."
                    else -> {
                        formError = null
                        onSave(
                            base.copy(
                                workerName = workerName.trim(),
                                farmCode = farmCode.trim(),
                                farmName = farmName.trim(),
                                date = date.trim().ifBlank { todayIsoDate() },
                                quantityKg = qty,
                                details = details.trim(),
                            )
                        ) {
                            creating = false
                            editing = null
                        }
                    }
                }
            },
        ) {
            if (farms.isEmpty()) {
                NoticeBanner("Consejo: registra primero tus fincas en la sección Fincas para seleccionarlas aquí fácilmente.")
            }
            FarmDropdownField(
                farms = farms,
                selectedCode = farmCode,
                label = "Finca o caficultor *",
                onSelected = { c, n ->
                    farmCode = c
                    farmName = n
                },
            )
            OutlinedTextField(
                value = workerName,
                onValueChange = { workerName = it },
                label = { Text("Trabajador o recolector *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = qtyText,
                    onValueChange = { qtyText = it },
                    label = { Text("Cantidad (kg) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Fecha (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
            OutlinedTextField(
                value = details,
                onValueChange = { details = it },
                label = { Text("Observaciones (lote, calidad)") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
            if (formError != null) {
                Text(formError.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }

    deleteTarget?.let { target ->
        ConfirmDeleteDialog(
            itemName = "${target.workerName} (${formatNumber(target.quantityKg)} kg)",
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
fun CropLogScreen(
    currentUserId: String,
    appRole: AppRole,
    cultivosState: UiState<List<CultivoActivity>>,
    fincasState: UiState<List<FincaProfile>>,
    isSaving: Boolean,
    onSave: (CultivoActivity, () -> Unit) -> Unit,
    onDelete: (String) -> Unit,
) {
    val canLogActivities = appRole.capabilities.logOwnCropActivities
    val allFarms = (fincasState as? UiState.Success)?.data.orEmpty()
    val myFarms = remember(allFarms, currentUserId) { allFarms.filter { it.userId == currentUserId } }
    var scopeTab by remember(appRole) { mutableStateOf(if (canLogActivities) "mia" else "comunidad") }
    var filterType by remember { mutableStateOf("Todos") }
    var search by remember { mutableStateOf("") }
    var creating by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<CultivoActivity?>(null) }
    var deleteTarget by remember { mutableStateOf<CultivoActivity?>(null) }

    val activityTypes = listOf("Siembra", "Abono", "Poda", "Control fitosanitario", "Cosecha")

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            SectionHeader(
                kicker = "TRAZABILIDAD EN FINCA",
                title = "Bitácora de cultivo",
                subtitle = if (canLogActivities) {
                    "Registra y edita las labores de tu finca o visualiza cómo manejan sus cafetales otros productores."
                } else {
                    "Consulta en modo lectura el manejo técnico, abonados y labores de cultivo de las fincas cafeteras."
                },
                action = if (canLogActivities) {
                    {
                        Button(
                            onClick = { creating = true },
                            modifier = Modifier.testTag("add_crop_activity_button"),
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Nueva labor")
                        }
                    }
                } else null,
            )
        }
        if (!canLogActivities) {
            item {
                NoticeBanner(
                    "Modo consulta comunitaria (${appRole.displayName}): las bitácoras de cultivo son editadas exclusivamente por el caficultor dueño de cada finca."
                )
            }
        }
        item {
            val allList = (cultivosState as? UiState.Success)?.data.orEmpty()
            val mineCount = allList.count { it.userId == currentUserId }
            val otherCount = if (canLogActivities) allList.count { it.userId != currentUserId } else allList.size
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (canLogActivities) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        FilterChip(
                            selected = scopeTab == "mia",
                            onClick = { scopeTab = "mia" },
                            label = { Text("Mi bitácora (Editar) · $mineCount") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ForestGreen,
                                selectedLabelColor = Color.White,
                            ),
                            modifier = Modifier.testTag("cultivo_tab_mia"),
                        )
                        FilterChip(
                            selected = scopeTab == "comunidad",
                            onClick = { scopeTab = "comunidad" },
                            label = { Text("Otras fincas (Visualizar) · $otherCount") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ForestGreen,
                                selectedLabelColor = Color.White,
                            ),
                            modifier = Modifier.testTag("cultivo_tab_comunidad"),
                        )
                    }
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    (listOf("Todos") + activityTypes).forEach { chip ->
                        FilterChip(
                            selected = filterType == chip,
                            onClick = { filterType = chip },
                            label = { Text(chip) },
                        )
                    }
                }
            }
        }
        item {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text("Buscar por labor, finca, caficultor o nota...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            StateContainer(uiState = cultivosState) { list ->
                val scopedList = when {
                    !canLogActivities -> list
                    scopeTab == "mia" -> list.filter { it.userId == currentUserId }
                    else -> list.filter { it.userId != currentUserId }
                }
                val filtered = scopedList.filter { act ->
                    val matchesType = filterType == "Todos" || act.activityType.equals(filterType, ignoreCase = true)
                    val q = search.trim().lowercase()
                    val matchesQuery = q.isEmpty() || act.name.lowercase().contains(q) ||
                        act.farmName.lowercase().contains(q) ||
                        act.authorName.lowercase().contains(q) ||
                        act.details.lowercase().contains(q)
                    matchesType && matchesQuery
                }
                if (filtered.isEmpty()) {
                    val isOwnTab = canLogActivities && scopeTab == "mia"
                    EmptyStateCard(
                        title = if (isOwnTab) "Aún no tienes labores en tu bitácora" else "Sin labores comunitarias aún",
                        subtitle = if (isOwnTab) {
                            "Documenta siembras, abonados, podas, control fitosanitario y cosechas de tu finca."
                        } else {
                            "Cuando los caficultores registren actividades en sus fincas podrás consultarlas aquí."
                        },
                        icon = Icons.Default.CalendarMonth,
                        actionLabel = if (isOwnTab) "Registrar actividad" else null,
                        onAction = if (isOwnTab) ({ creating = true }) else null,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        filtered.forEach { act ->
                            val isOwner = canLogActivities && act.userId == currentUserId
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = RoundedCornerShape(50),
                                        ) {
                                            Text(
                                                text = act.activityType,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            )
                                        }
                                        Text(
                                            text = act.date,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Text(act.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                    Text(
                                        text = buildString {
                                            append(act.farmName.ifBlank { act.farmCode })
                                            if (act.authorName.isNotBlank()) append(" · ${act.authorName}")
                                            append(" · ${act.recordCode}")
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    if (act.startTime.isNotBlank() || act.endTime.isNotBlank() || act.quantity > 0.0) {
                                        val timeLabel = listOf(act.startTime, act.endTime).filter { it.isNotBlank() }.joinToString(" - ")
                                        val qtyLabel = if (act.quantity > 0.0) "Cantidad: ${formatNumber(act.quantity)}" else ""
                                        Text(
                                            text = listOf(timeLabel, qtyLabel).filter { it.isNotBlank() }.joinToString(" · "),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.secondary,
                                        )
                                    }
                                    if (act.details.isNotBlank()) {
                                        Text(act.details, style = MaterialTheme.typography.bodySmall)
                                    }
                                    if (isOwner) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedButton(onClick = { editing = act }, modifier = Modifier.weight(1f)) {
                                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(6.dp))
                                                Text("Editar")
                                            }
                                            OutlinedButton(
                                                onClick = { deleteTarget = act },
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerClay),
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Eliminar actividad", modifier = Modifier.size(16.dp))
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

    if (creating || editing != null) {
        val base = editing ?: CultivoActivity(date = todayIsoDate())
        var activityType by remember(base.id) { mutableStateOf(base.activityType.ifBlank { "Siembra" }) }
        var name by remember(base.id) { mutableStateOf(base.name) }
        var farmId by remember(base.id) { mutableStateOf(base.farmId) }
        var farmCode by remember(base.id) { mutableStateOf(base.farmCode) }
        var farmName by remember(base.id) { mutableStateOf(base.farmName) }
        var date by remember(base.id) { mutableStateOf(base.date.ifBlank { todayIsoDate() }) }
        var startTime by remember(base.id) { mutableStateOf(base.startTime) }
        var endTime by remember(base.id) { mutableStateOf(base.endTime) }
        var qtyText by remember(base.id) { mutableStateOf(if (base.quantity > 0.0) base.quantity.toString() else "") }
        var details by remember(base.id) { mutableStateOf(base.details) }
        var formError by remember(base.id) { mutableStateOf<String?>(null) }

        FormModalDialog(
            title = if (editing == null) "Nueva actividad de cultivo" else "Editar actividad",
            onDismiss = {
                creating = false
                editing = null
            },
            confirmEnabled = !isSaving,
            onConfirm = {
                val timeConflict = startTime.isNotBlank() && endTime.isNotBlank() && endTime.trim() <= startTime.trim()
                when {
                    name.isBlank() -> formError = "Escribe el nombre de la labor o insumo."
                    farmCode.isBlank() -> formError = "Selecciona una de tus fincas."
                    timeConflict -> formError = "La hora de fin debe ser posterior a la hora de inicio (ej. 07:00 y 15:00)."
                    else -> {
                        formError = null
                        val chosen = myFarms.firstOrNull { it.recordCode == farmCode || it.id == farmCode }
                        onSave(
                            base.copy(
                                activityType = activityType,
                                name = name.trim(),
                                farmId = chosen?.id ?: farmId,
                                farmCode = farmCode.trim(),
                                farmName = chosen?.name ?: farmName.trim(),
                                date = date.trim().ifBlank { todayIsoDate() },
                                startTime = startTime.trim(),
                                endTime = endTime.trim(),
                                quantity = qtyText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0,
                                details = details.trim(),
                            )
                        ) {
                            creating = false
                            editing = null
                        }
                    }
                }
            },
        ) {
            if (myFarms.isEmpty()) {
                NoticeBanner("Registra primero tu propia finca en el módulo Fincas para asociar labores de bitácora.")
            }
            Text("Tipo de labor", style = MaterialTheme.typography.labelMedium)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                activityTypes.forEach { t ->
                    FilterChip(
                        selected = activityType == t,
                        onClick = { activityType = t },
                        label = { Text(t) },
                    )
                }
            }
            FarmDropdownField(
                farms = myFarms,
                selectedCode = farmCode,
                label = "Tu finca *",
                onSelected = { c, n ->
                    farmCode = c
                    farmName = n
                    farmId = myFarms.firstOrNull { it.recordCode == c || it.id == c }?.id.orEmpty()
                },
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Descripción corta de la labor *") },
                placeholder = { Text("Ej. Abonado lote superior con NPK") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Fecha (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = qtyText,
                    onValueChange = { qtyText = it },
                    label = { Text("Cantidad (opc.)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = startTime,
                    onValueChange = { startTime = it },
                    label = { Text("Inicio (HH:MM)") },
                    placeholder = { Text("07:00") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = endTime,
                    onValueChange = { endTime = it },
                    label = { Text("Fin (HH:MM)") },
                    placeholder = { Text("15:00") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
            OutlinedTextField(
                value = details,
                onValueChange = { details = it },
                label = { Text("Notas adicionales") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
            if (formError != null) {
                Text(formError.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }

    deleteTarget?.let { target ->
        ConfirmDeleteDialog(
            itemName = "${target.activityType} · ${target.name}",
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
fun AgronomyScreen(
    currentUserId: String,
    appRole: AppRole,
    userProfile: UserProfile?,
    visitasState: UiState<List<VisitaAgronomica>>,
    inquietudesState: UiState<List<InquietudTecnica>>,
    fincasState: UiState<List<FincaProfile>>,
    isSaving: Boolean,
    preselectedFarmCode: String? = null,
    onConsumePreselectedFarm: () -> Unit = {},
    onSave: (VisitaAgronomica, () -> Unit) -> Unit,
    onDelete: (String) -> Unit,
    onSaveInquietud: (InquietudTecnica, () -> Unit) -> Unit,
    onReplyInquietud: (String, String, () -> Unit) -> Unit,
    onDeleteInquietud: (String) -> Unit,
    onAskAgronomyAi: (question: String, farmContext: String) -> Unit,
) {
    val canRegisterVisit = appRole.capabilities.registerAgronomyVisits
    val canReplyInquiries = appRole.capabilities.replyInquiries
    val canCreateInquiries = appRole.capabilities.createInquiries

    val farms = (fincasState as? UiState.Success)?.data.orEmpty()
    val farmOptions = remember(farms) { farms.filter { it.recordType == "finca" }.ifEmpty { farms } }

    var activeSectionTab by remember { mutableStateOf("visitas") } // "visitas" | "inquietudes"
    var visitScopeFilter by remember { mutableStateOf("todas") } // "todas" | "mias" | "mis_fincas"
    var inquietudFilter by remember { mutableStateOf("todas") } // "todas" | "abierta" | "respondida"

    var aiQuestion by remember { mutableStateOf("") }
    var aiFarmCode by remember { mutableStateOf("") }
    var aiFarmName by remember { mutableStateOf("") }

    var creatingVisit by remember { mutableStateOf(false) }
    var prefillVisit by remember { mutableStateOf<VisitaAgronomica?>(null) }
    var editingVisit by remember { mutableStateOf<VisitaAgronomica?>(null) }
    var deleteVisitTarget by remember { mutableStateOf<VisitaAgronomica?>(null) }

    var creatingInquietud by remember { mutableStateOf(false) }
    var editingInquietud by remember { mutableStateOf<InquietudTecnica?>(null) }
    var replyingInquietud by remember { mutableStateOf<InquietudTecnica?>(null) }
    var deleteInquietudTarget by remember { mutableStateOf<InquietudTecnica?>(null) }

    LaunchedEffect(preselectedFarmCode, farmOptions, canRegisterVisit) {
        if (!preselectedFarmCode.isNullOrBlank()) {
            if (canRegisterVisit) {
                activeSectionTab = "visitas"
                val targetFarm = farmOptions.firstOrNull {
                    it.recordCode == preselectedFarmCode || it.id == preselectedFarmCode
                }
                prefillVisit = VisitaAgronomica(
                    date = todayIsoDate(),
                    engineerName = userProfile?.fullName.orEmpty(),
                    farmId = targetFarm?.id.orEmpty(),
                    farmCode = targetFarm?.recordCode?.ifBlank { targetFarm.id } ?: preselectedFarmCode,
                    farmName = targetFarm?.name.orEmpty(),
                    farmOwnerId = targetFarm?.userId.orEmpty(),
                    farmOwnerName = targetFarm?.ownerName.orEmpty(),
                    location = targetFarm?.location.orEmpty(),
                )
                creatingVisit = true
            }
            onConsumePreselectedFarm()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            SectionHeader(
                kicker = "ACOMPAÑAMIENTO TÉCNICO",
                title = "Panel agronómico",
                subtitle = when (appRole) {
                    AppRole.AGRONOMO -> "Registra visitas a cualquier finca, publica observaciones y responde inquietudes de los caficultores."
                    AppRole.CAFICULTOR -> "Consulta las visitas técnicas hechas a tus fincas y envía inquietudes a los ingenieros agrónomos."
                    else -> "Consulta las visitas agronómicas e inquietudes técnicas compartidas en la comunidad."
                },
                action = if (canRegisterVisit || canCreateInquiries) {
                    {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (canRegisterVisit) {
                                Button(
                                    onClick = {
                                        prefillVisit = VisitaAgronomica(
                                            date = todayIsoDate(),
                                            engineerName = userProfile?.fullName.orEmpty(),
                                        )
                                        creatingVisit = true
                                    },
                                    modifier = Modifier.testTag("add_agronomy_visit_button"),
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(17.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Registrar visita")
                                }
                            }
                            if (canCreateInquiries) {
                                OutlinedButton(
                                    onClick = {
                                        activeSectionTab = "inquietudes"
                                        creatingInquietud = true
                                    },
                                    modifier = Modifier.testTag("add_inquietud_button"),
                                ) {
                                    Icon(Icons.Default.QuestionAnswer, contentDescription = null, modifier = Modifier.size(17.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Nueva inquietud")
                                }
                            }
                        }
                    }
                } else null,
            )
        }

        // Section mode tabs: Visitas vs Inquietudes
        item {
            val allVisitas = (visitasState as? UiState.Success)?.data.orEmpty()
            val allInquietudes = (inquietudesState as? UiState.Success)?.data.orEmpty()
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                FilterChip(
                    selected = activeSectionTab == "visitas",
                    onClick = { activeSectionTab = "visitas" },
                    label = { Text("Visitas a fincas (${allVisitas.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ForestGreen,
                        selectedLabelColor = Color.White,
                    ),
                    modifier = Modifier.testTag("agronomy_tab_visitas"),
                )
                FilterChip(
                    selected = activeSectionTab == "inquietudes",
                    onClick = { activeSectionTab = "inquietudes" },
                    label = { Text("Inquietudes técnicas (${allInquietudes.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ForestGreen,
                        selectedLabelColor = Color.White,
                    ),
                    modifier = Modifier.testTag("agronomy_tab_inquietudes"),
                )
            }
        }

        // Gemini AI Agronomist Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ForestGreen),
                shape = RoundedCornerShape(20.dp),
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = WarmGold)
                        Text(
                            text = "Consultar Asesor Agronómico IA",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                        )
                    }
                    Text(
                        text = "Describe síntomas en hoja, fruto, sombra o nutrición para recibir orientación técnica adaptada al café de altura de Nariño.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE0EBDD),
                    )
                    OutlinedTextField(
                        value = aiQuestion,
                        onValueChange = { aiQuestion = it },
                        placeholder = { Text("Ej. Manchas anaranjadas en el envés de la hoja en lote de Castillo...") },
                        minLines = 2,
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedTextColor = ForestGreen,
                            unfocusedTextColor = ForestGreen,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("agronomy_ai_input"),
                    )
                    if (farmOptions.isNotEmpty()) {
                        FarmDropdownField(
                            farms = farmOptions,
                            selectedCode = aiFarmCode,
                            label = "Contexto de finca (opcional)",
                            allowEmpty = true,
                            onSelected = { code, name ->
                                aiFarmCode = code
                                aiFarmName = name
                            },
                        )
                    }
                    Button(
                        onClick = {
                            val q = aiQuestion.trim().ifBlank {
                                "Recomendaciones preventivas de roya, broca y fertilización en Albán, Nariño para esta temporada."
                            }
                            val ctx = if (aiFarmName.isNotBlank()) "$aiFarmName ($aiFarmCode)" else "San José de Albán, Nariño"
                            onAskAgronomyAi(q, ctx)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WarmGold, contentColor = ForestGreen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("agronomy_ai_submit"),
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Consultar orientación técnica")
                    }
                }
            }
        }

        if (activeSectionTab == "visitas") {
            item {
                val scopeOptions = buildList {
                    add("todas" to "Todas las fincas")
                    if (canRegisterVisit) add("mias" to "Mis visitas registradas")
                    if (appRole == AppRole.CAFICULTOR) add("mis_fincas" to "Visitas a mis fincas")
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    scopeOptions.forEach { (key, label) ->
                        FilterChip(
                            selected = visitScopeFilter == key,
                            onClick = { visitScopeFilter = key },
                            label = { Text(label) },
                        )
                    }
                }
            }
            item {
                StateContainer(uiState = visitasState) { list ->
                    val myFarmIds = farms.filter { it.userId == currentUserId }.map { it.id }.toSet()
                    val filtered = when (visitScopeFilter) {
                        "mias" -> list.filter { it.userId == currentUserId }
                        "mis_fincas" -> list.filter {
                            it.farmOwnerId == currentUserId || (it.farmId.isNotBlank() && it.farmId in myFarmIds)
                        }
                        else -> list
                    }
                    if (filtered.isEmpty()) {
                        EmptyStateCard(
                            title = "Sin visitas agronómicas registradas",
                            subtitle = if (canRegisterVisit) {
                                "Selecciona una finca para registrar observaciones, qué se realizó y recomendaciones."
                            } else {
                                "Cuando los ingenieros agrónomos registren visitas a las fincas podrás consultarlas aquí."
                            },
                            icon = Icons.Default.Eco,
                            actionLabel = if (canRegisterVisit) "Registrar visita" else null,
                            onAction = if (canRegisterVisit) ({
                                prefillVisit = VisitaAgronomica(
                                    date = todayIsoDate(),
                                    engineerName = userProfile?.fullName.orEmpty(),
                                )
                                creatingVisit = true
                            }) else null,
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            filtered.forEach { visit ->
                                val isAuthor = canRegisterVisit && visit.userId == currentUserId
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                shape = RoundedCornerShape(50),
                                            ) {
                                                Text(
                                                    text = visit.recordCode,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                )
                                            }
                                            Text(
                                                text = visit.date,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        Text(visit.diagnosis, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                        Text(
                                            text = buildString {
                                                append("Ing. ${visit.engineerName}")
                                                val farmLabel = visit.farmName.ifBlank { visit.farmCode }
                                                if (farmLabel.isNotBlank()) append(" · Finca: $farmLabel")
                                                if (visit.farmOwnerName.isNotBlank()) append(" (${visit.farmOwnerName})")
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.secondary,
                                        )
                                        if (visit.activitiesPerformed.isNotBlank()) {
                                            Text(
                                                text = "Qué se realizó: ${visit.activitiesPerformed}",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium,
                                            )
                                        }
                                        if (visit.recommendations.isNotBlank()) {
                                            Text(
                                                text = "Comentarios y recomendaciones: ${visit.recommendations}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        if (isAuthor) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                OutlinedButton(onClick = { editingVisit = visit }, modifier = Modifier.weight(1f)) {
                                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(Modifier.width(6.dp))
                                                    Text("Editar")
                                                }
                                                OutlinedButton(
                                                    onClick = { deleteVisitTarget = visit },
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerClay),
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Eliminar visita", modifier = Modifier.size(16.dp))
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
        } else {
            // Inquietudes técnicas tab
            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    listOf(
                        "todas" to "Todas",
                        "abierta" to "Abiertas",
                        "respondida" to "Respondidas",
                    ).forEach { (key, label) ->
                        FilterChip(
                            selected = inquietudFilter == key,
                            onClick = { inquietudFilter = key },
                            label = { Text(label) },
                        )
                    }
                }
            }
            item {
                StateContainer(uiState = inquietudesState) { list ->
                    val filtered = list.filter { inq ->
                        inquietudFilter == "todas" || inq.status.equals(inquietudFilter, ignoreCase = true)
                    }
                    if (filtered.isEmpty()) {
                        EmptyStateCard(
                            title = "Sin inquietudes técnicas aún",
                            subtitle = "Los caficultores pueden publicar preguntas sobre su cultivo y los ingenieros responderlas aquí.",
                            icon = Icons.Default.QuestionAnswer,
                            actionLabel = if (canCreateInquiries) "Publicar inquietud" else null,
                            onAction = if (canCreateInquiries) ({ creatingInquietud = true }) else null,
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            filtered.forEach { inq ->
                                val isAuthor = canCreateInquiries && inq.userId == currentUserId
                                val isAnswered = inq.status == "respondida"
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Surface(
                                                color = if (isAnswered) SagePale else MaterialTheme.colorScheme.secondaryContainer,
                                                shape = RoundedCornerShape(50),
                                            ) {
                                                Text(
                                                    text = if (isAnswered) "Respondida" else "Abierta",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (isAnswered) ForestGreen else MaterialTheme.colorScheme.onSecondaryContainer,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                )
                                            }
                                            Text(
                                                text = "${inq.date} · ${inq.recordCode}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        Text(inq.subject, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                        Text(
                                            text = buildString {
                                                append("Por ${inq.authorName} (${inq.authorRole})")
                                                if (inq.farmName.isNotBlank()) append(" · Finca: ${inq.farmName}")
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.secondary,
                                        )
                                        Text(inq.question, style = MaterialTheme.typography.bodyMedium)

                                        if (inq.replyText.isNotBlank()) {
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = SagePale.copy(alpha = 0.65f),
                                                border = BorderStroke(1.dp, ForestGreen.copy(alpha = 0.25f)),
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(10.dp),
                                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                                ) {
                                                    Text(
                                                        text = "Respuesta de ${inq.replyByName.ifBlank { "Ingeniero agrónomo" }} · ${inq.replyDate}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = ForestGreen,
                                                        fontWeight = FontWeight.Bold,
                                                    )
                                                    Text(inq.replyText, style = MaterialTheme.typography.bodySmall, color = ForestGreen)
                                                }
                                            }
                                        }

                                        if (canReplyInquiries || isAuthor) {
                                            FlowRow(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                            ) {
                                                if (canReplyInquiries) {
                                                    Button(onClick = { replyingInquietud = inq }) {
                                                        Icon(Icons.Default.Reply, contentDescription = null, modifier = Modifier.size(16.dp))
                                                        Spacer(Modifier.width(4.dp))
                                                        Text(if (isAnswered) "Actualizar respuesta" else "Responder")
                                                    }
                                                }
                                                if (isAuthor && !isAnswered) {
                                                    OutlinedButton(onClick = { editingInquietud = inq }) {
                                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                                        Spacer(Modifier.width(4.dp))
                                                        Text("Editar")
                                                    }
                                                }
                                                if (isAuthor) {
                                                    OutlinedButton(
                                                        onClick = { deleteInquietudTarget = inq },
                                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerClay),
                                                    ) {
                                                        Icon(Icons.Default.Delete, contentDescription = "Eliminar inquietud", modifier = Modifier.size(16.dp))
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
        }
    }

    // Create / Edit Visit Modal
    if (creatingVisit || editingVisit != null) {
        val base = editingVisit ?: prefillVisit ?: VisitaAgronomica(
            date = todayIsoDate(),
            engineerName = userProfile?.fullName.orEmpty(),
        )
        var engineerName by remember(base.id, base.farmCode) { mutableStateOf(base.engineerName) }
        var farmId by remember(base.id, base.farmCode) { mutableStateOf(base.farmId) }
        var farmCode by remember(base.id, base.farmCode) { mutableStateOf(base.farmCode) }
        var farmName by remember(base.id, base.farmCode) { mutableStateOf(base.farmName) }
        var location by remember(base.id, base.farmCode) { mutableStateOf(base.location) }
        var date by remember(base.id, base.farmCode) { mutableStateOf(base.date.ifBlank { todayIsoDate() }) }
        var diagnosis by remember(base.id, base.farmCode) { mutableStateOf(base.diagnosis) }
        var activitiesPerformed by remember(base.id, base.farmCode) { mutableStateOf(base.activitiesPerformed) }
        var recommendations by remember(base.id, base.farmCode) { mutableStateOf(base.recommendations) }
        var formError by remember(base.id, base.farmCode) { mutableStateOf<String?>(null) }

        FormModalDialog(
            title = if (editingVisit == null) "Registrar visita agronómica" else "Editar visita agronómica",
            subtitle = "Todos los registros quedan visibles en la ficha de la finca visitada.",
            onDismiss = {
                creatingVisit = false
                editingVisit = null
                prefillVisit = null
            },
            confirmEnabled = !isSaving,
            onConfirm = {
                when {
                    engineerName.isBlank() -> formError = "Ingresa el nombre del ingeniero o técnico."
                    diagnosis.isBlank() -> formError = "Ingresa qué observaste o el diagnóstico de la visita."
                    else -> {
                        formError = null
                        val chosen = farmOptions.firstOrNull { it.recordCode == farmCode || it.id == farmCode || it.id == farmId }
                        onSave(
                            base.copy(
                                engineerName = engineerName.trim(),
                                farmId = chosen?.id ?: farmId,
                                farmCode = farmCode.trim(),
                                farmName = chosen?.name ?: farmName.trim(),
                                farmOwnerId = chosen?.userId ?: base.farmOwnerId,
                                farmOwnerName = chosen?.ownerName?.ifBlank { chosen.name } ?: base.farmOwnerName,
                                location = location.trim().ifBlank { chosen?.location.orEmpty() },
                                date = date.trim().ifBlank { todayIsoDate() },
                                diagnosis = diagnosis.trim(),
                                activitiesPerformed = activitiesPerformed.trim(),
                                recommendations = recommendations.trim(),
                            )
                        ) {
                            creatingVisit = false
                            editingVisit = null
                            prefillVisit = null
                        }
                    }
                }
            },
        ) {
            OutlinedTextField(
                value = engineerName,
                onValueChange = { engineerName = it },
                label = { Text("Ingeniero / técnico responsable *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            FarmDropdownField(
                farms = farmOptions,
                selectedCode = farmCode,
                label = "Finca visitada (cualquier finca)",
                allowEmpty = true,
                onSelected = { c, n ->
                    farmCode = c
                    farmName = n
                    val picked = farmOptions.firstOrNull { it.recordCode == c || it.id == c }
                    farmId = picked?.id.orEmpty()
                    if (location.isBlank() && picked != null) location = picked.location
                },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Vereda / sector") },
                    singleLine = true,
                    modifier = Modifier.weight(1.2f),
                )
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Fecha") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
            OutlinedTextField(
                value = diagnosis,
                onValueChange = { diagnosis = it },
                label = { Text("Qué se observó / diagnóstico *") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = activitiesPerformed,
                onValueChange = { activitiesPerformed = it },
                label = { Text("Qué se hizo en la visita (actividades)") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = recommendations,
                onValueChange = { recommendations = it },
                label = { Text("Comentarios y recomendaciones") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
            if (formError != null) {
                Text(formError.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }

    deleteVisitTarget?.let { target ->
        ConfirmDeleteDialog(
            itemName = target.diagnosis,
            onConfirm = {
                onDelete(target.id)
                deleteVisitTarget = null
            },
            onDismiss = { deleteVisitTarget = null },
        )
    }

    // Create / Edit Inquietud Modal
    if (creatingInquietud || editingInquietud != null) {
        val base = editingInquietud ?: InquietudTecnica(date = todayIsoDate())
        var subject by remember(base.id) { mutableStateOf(base.subject) }
        var question by remember(base.id) { mutableStateOf(base.question) }
        var farmCode by remember(base.id) { mutableStateOf(base.farmCode) }
        var farmName by remember(base.id) { mutableStateOf(base.farmName) }
        var farmId by remember(base.id) { mutableStateOf(base.farmId) }
        var formError by remember(base.id) { mutableStateOf<String?>(null) }

        FormModalDialog(
            title = if (editingInquietud == null) "Nueva inquietud técnica" else "Editar inquietud",
            subtitle = "Los ingenieros agrónomos verán tu consulta y podrán responderte aquí.",
            onDismiss = {
                creatingInquietud = false
                editingInquietud = null
            },
            confirmEnabled = !isSaving,
            onConfirm = {
                when {
                    subject.trim().length < 2 -> formError = "Escribe un asunto breve."
                    question.trim().length < 3 -> formError = "Describe tu pregunta o síntoma en campo."
                    else -> {
                        formError = null
                        onSaveInquietud(
                            base.copy(
                                subject = subject.trim(),
                                question = question.trim(),
                                farmId = farmId,
                                farmCode = farmCode.trim(),
                                farmName = farmName.trim(),
                            )
                        ) {
                            creatingInquietud = false
                            editingInquietud = null
                        }
                    }
                }
            },
        ) {
            FarmDropdownField(
                farms = farmOptions,
                selectedCode = farmCode,
                label = "Finca relacionada (opcional)",
                allowEmpty = true,
                onSelected = { c, n ->
                    farmCode = c
                    farmName = n
                    farmId = farmOptions.firstOrNull { it.recordCode == c || it.id == c }?.id.orEmpty()
                },
            )
            OutlinedTextField(
                value = subject,
                onValueChange = { subject = it },
                label = { Text("Asunto o tema *") },
                placeholder = { Text("Ej. Presencia de broca en lote El Mirador") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = question,
                onValueChange = { question = it },
                label = { Text("Detalle de tu inquietud *") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
            if (formError != null) {
                Text(formError.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }

    // Engineer Reply Modal
    replyingInquietud?.let { target ->
        var replyText by remember(target.id) { mutableStateOf(target.replyText) }
        var replyError by remember(target.id) { mutableStateOf<String?>(null) }

        FormModalDialog(
            title = "Responder inquietud técnica",
            subtitle = "${target.authorName}: ${target.subject}",
            confirmLabel = "Publicar respuesta",
            confirmEnabled = !isSaving,
            onDismiss = { replyingInquietud = null },
            onConfirm = {
                if (replyText.trim().length < 2) {
                    replyError = "Escribe una recomendación clara antes de enviar."
                } else {
                    replyError = null
                    onReplyInquietud(target.id, replyText.trim()) {
                        replyingInquietud = null
                    }
                }
            },
        ) {
            Text(target.question, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                value = replyText,
                onValueChange = { replyText = it },
                label = { Text("Respuesta y recomendación técnica *") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
            if (replyError != null) {
                Text(replyError.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }

    deleteInquietudTarget?.let { target ->
        ConfirmDeleteDialog(
            itemName = target.subject,
            onConfirm = {
                onDeleteInquietud(target.id)
                deleteInquietudTarget = null
            },
            onDismiss = { deleteInquietudTarget = null },
        )
    }
}

@Composable
fun CollectorsScreen(
    appRole: AppRole,
    recolectoresState: UiState<List<RecolectorPostulacion>>,
    isSaving: Boolean,
    onSave: (RecolectorPostulacion, () -> Unit) -> Unit,
    onDelete: (String) -> Unit,
) {
    val canManageCollectors = appRole.capabilities.manageCollectors
    var contactName by remember { mutableStateOf("") }
    var contactEmail by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var experience by remember { mutableStateOf("Con experiencia") }
    var availability by remember { mutableStateOf("") }
    var formError by remember { mutableStateOf<String?>(null) }
    var deleteTarget by remember { mutableStateOf<RecolectorPostulacion?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            SectionHeader(
                kicker = "TALENTO DE COSECHA",
                title = "Trabajo / Recolectores",
                subtitle = "Registra datos de disponibilidad para jornadas de recolección. Tus datos personales se almacenan en tu bóveda privada.",
            )
        }
        item {
            NoticeBanner(
                text = if (canManageCollectors) {
                    "Privacidad protegida: por seguridad, las postulaciones y teléfonos quedan guardados exclusivamente en tu cuenta privada y no se exponen a terceros."
                } else {
                    "Módulo privado exclusivo del rol Caficultor: tu cuenta (${appRole.displayName}) no gestiona recolectores de cosecha."
                }
            )
        }
        if (canManageCollectors) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Text("Nueva inscripción de recolector", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        }
                    OutlinedTextField(
                        value = contactName,
                        onValueChange = { contactName = it },
                        label = { Text("Nombre completo *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = contactPhone,
                            onValueChange = { contactPhone = it },
                            label = { Text("Teléfono / WhatsApp *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = location,
                            onValueChange = { location = it },
                            label = { Text("Vereda / municipio *") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    OutlinedTextField(
                        value = contactEmail,
                        onValueChange = { contactEmail = it },
                        label = { Text("Correo electrónico *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text("Experiencia en recolección", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Con experiencia", "Sin experiencia", "Cuadrilla").forEach { exp ->
                            FilterChip(
                                selected = experience == exp,
                                onClick = { experience = exp },
                                label = { Text(exp) },
                            )
                        }
                    }
                    OutlinedTextField(
                        value = availability,
                        onValueChange = { availability = it },
                        label = { Text("Disponibilidad (días o temporada) *") },
                        placeholder = { Text("Ej. Lunes a sábado, cosecha mayo - julio") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (formError != null) {
                        Text(formError.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                    Button(
                        onClick = {
                            when {
                                contactName.isBlank() -> formError = "Ingresa el nombre completo."
                                contactPhone.trim().length < 5 -> formError = "Ingresa un número de teléfono válido (mínimo 5 dígitos)."
                                !contactEmail.contains("@") || !contactEmail.contains(".") ->
                                    formError = "Ingresa un correo electrónico válido."
                                location.isBlank() -> formError = "Ingresa la vereda o municipio."
                                availability.isBlank() -> formError = "Indica los días o temporada disponible."
                                else -> {
                                    formError = null
                                    onSave(
                                        RecolectorPostulacion(
                                            contactName = contactName.trim(),
                                            contactEmail = contactEmail.trim(),
                                            contactPhone = contactPhone.trim(),
                                            location = location.trim(),
                                            experience = experience,
                                            availability = availability.trim(),
                                        )
                                    ) {
                                        contactName = ""
                                        contactEmail = ""
                                        contactPhone = ""
                                        location = ""
                                        availability = ""
                                    }
                                }
                            }
                        },
                        enabled = !isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("submit_collector_button"),
                    ) {
                        Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Guardar inscripción privada")
                    }
                }
            }
        }
        }
        item {
            Text(
                text = "Mis postulaciones registradas",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        item {
            StateContainer(uiState = recolectoresState) { list ->
                if (list.isEmpty()) {
                    EmptyStateCard(
                        title = "Sin postulaciones guardadas",
                        subtitle = "Las fichas de recolectores que registres aparecerán aquí para consultarlas o gestionarlas.",
                        icon = Icons.Default.Badge,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        list.forEach { rec ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(rec.contactName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                        Text(
                                            text = "${rec.experience} · ${rec.location} · ${rec.recordCode}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Text(
                                            text = "Tel: ${rec.contactPhone} · ${rec.availability}",
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                    if (canManageCollectors) {
                                        IconButton(onClick = { deleteTarget = rec }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Eliminar postulación", tint = DangerClay)
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

    deleteTarget?.let { target ->
        ConfirmDeleteDialog(
            itemName = target.contactName,
            onConfirm = {
                onDelete(target.id)
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null },
        )
    }
}
