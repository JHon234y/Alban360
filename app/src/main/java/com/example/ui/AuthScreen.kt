package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.firebase.onGoogleSignInClicked
import com.example.model.AppRole
import com.example.model.PendingUserRegistration
import com.example.model.UserProfile
import com.example.ui.components.AsyncDrawableImage
import com.example.ui.components.FormModalDialog
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.SagePale
import com.example.ui.theme.WarmGold
import com.google.firebase.auth.FirebaseUser

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AuthScreen(
    credentialManager: CredentialManager? = null,
    onPrepareRegistration: (PendingUserRegistration?) -> Unit,
    onAuthSuccess: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val resolvedCredentialManager = credentialManager ?: remember(context) { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // 0 = Iniciar sesión, 1 = Crear cuenta con datos personales + Google
    var authMode by remember { mutableIntStateOf(0) }
    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("San José de Albán, Nariño") }
    var selectedRole by remember { mutableStateOf(AppRole.CAFICULTOR) }
    var farmName by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(205.dp)
                    .clip(RoundedCornerShape(24.dp)),
            ) {
                AsyncDrawableImage(
                    resId = R.drawable.img_coffee_hero,
                    contentDescription = "Paisaje cafetero de San José de Albán, Nariño",
                    contentScale = ContentScale.Crop,
                    maxDimPx = 960,
                    placeholderColor = ForestGreen,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    ForestGreen.copy(alpha = 0.25f),
                                    ForestGreen.copy(alpha = 0.92f),
                                )
                            )
                        )
                        .padding(20.dp),
                ) {
                    Column(
                        modifier = Modifier.align(Alignment.BottomStart),
                    ) {
                        Surface(
                            color = WarmGold,
                            shape = RoundedCornerShape(50),
                        ) {
                            Text(
                                text = "SAN JOSÉ DE ALBÁN · NARIÑO",
                                style = MaterialTheme.typography.labelSmall,
                                color = ForestGreen,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "ALBÁN CAFÉ 360°",
                            style = MaterialTheme.typography.displaySmall,
                            color = Color.White,
                        )
                        Text(
                            text = "CAFÉ CONECTA · Del cultivo a una mejor decisión.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFE7F0E5),
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(22.dp),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = authMode == 0,
                            onClick = {
                                authMode = 0
                                errorMessage = null
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            modifier = Modifier.testTag("auth_mode_login_tab"),
                        ) {
                            Text("Iniciar sesión", maxLines = 1)
                        }
                        SegmentedButton(
                            selected = authMode == 1,
                            onClick = {
                                authMode = 1
                                errorMessage = null
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            modifier = Modifier.testTag("auth_mode_register_tab"),
                        ) {
                            Text("Crear usuario", maxLines = 1)
                        }
                    }

                    if (authMode == 0) {
                        Text(
                            text = "Ingresa con tu cuenta de Google",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = "Sincroniza tus fincas, bitácora, cosechas, historial de precios del café y costos. Si es tu primer ingreso podrás completar tu nombre, contacto celular y lugar.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        listOf(
                            Icons.Default.QrCode2 to "Perfiles trazables con código QR para fincas y caficultores",
                            Icons.Default.Landscape to "Mapa satelital, bitácora de cultivo y control de cosechas",
                            Icons.Default.TrendingUp to "Bolsa NY, dólar TRM, historial de precios y Asesor IA",
                        ).forEach { (icon, text) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .padding(7.dp)
                                            .size(18.dp),
                                    )
                                }
                                Text(
                                    text = text,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    } else {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(50),
                        ) {
                            Text(
                                text = "PASO 1: TUS DATOS · PASO 2: VINCULAR GOOGLE",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                        Text(
                            text = "Crear usuario en la plataforma",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = "Registra el nombre de la persona, contacto celular y lugar, e inicia sesión con tu cuenta de Google para activar tu panel.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        OutlinedTextField(
                            value = fullName,
                            onValueChange = {
                                fullName = it
                                if (errorMessage != null) errorMessage = null
                            },
                            label = { Text("Nombre de la persona *") },
                            placeholder = { Text("Ej. Carlos Andrés Gómez") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_name_input"),
                        )

                        OutlinedTextField(
                            value = phone,
                            onValueChange = {
                                phone = it
                                if (errorMessage != null) errorMessage = null
                            },
                            label = { Text("Contacto celular / WhatsApp *") },
                            placeholder = { Text("Ej. 312 456 7890") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_phone_input"),
                        )

                        OutlinedTextField(
                            value = location,
                            onValueChange = {
                                location = it
                                if (errorMessage != null) errorMessage = null
                            },
                            label = { Text("Lugar (vereda o municipio) *") },
                            placeholder = { Text("Ej. Vereda El Carmen, San José de Albán") },
                            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_location_input"),
                        )

                        Text(
                            text = "Elige tu rol permanente en la plataforma *",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                        RoleSelectionList(
                            selectedRole = selectedRole,
                            onSelectRole = { selectedRole = it },
                            tagPrefix = "register",
                        )

                        val orgLabel = when (selectedRole) {
                            AppRole.CAFICULTOR -> "Finca principal (opcional)"
                            AppRole.PROVEEDOR -> "Almacén o empresa (opcional)"
                            AppRole.COMPRADOR -> "Agencia o punto de compra (opcional)"
                            AppRole.AGRONOMO -> "Entidad o zona de atención (opcional)"
                        }
                        OutlinedTextField(
                            value = farmName,
                            onValueChange = { farmName = it },
                            label = { Text(orgLabel) },
                            leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    if (errorMessage != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Text(
                                text = errorMessage.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(12.dp),
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    Button(
                        onClick = {
                            if (authMode == 1) {
                                if (fullName.trim().length < 2) {
                                    errorMessage = "Escribe el nombre de la persona para continuar."
                                    return@Button
                                }
                                if (phone.trim().length < 5) {
                                    errorMessage = "Ingresa un número de contacto celular válido."
                                    return@Button
                                }
                                if (location.trim().length < 2) {
                                    errorMessage = "Ingresa el lugar (vereda o municipio) para continuar."
                                    return@Button
                                }
                                onPrepareRegistration(
                                    PendingUserRegistration(
                                        fullName = fullName.trim(),
                                        phone = phone.trim(),
                                        location = location.trim(),
                                        roleId = selectedRole.roleId,
                                        role = selectedRole.displayName,
                                        farmName = farmName.trim(),
                                    )
                                )
                            } else {
                                onPrepareRegistration(null)
                            }
                            isLoading = true
                            errorMessage = null
                            onGoogleSignInClicked(
                                context = context,
                                credentialManager = resolvedCredentialManager,
                                onAuthSuccess = {
                                    isLoading = false
                                    onAuthSuccess()
                                },
                                onAuthError = { msg ->
                                    isLoading = false
                                    errorMessage = msg
                                },
                                scope = scope,
                                onAuthCancelled = {
                                    isLoading = false
                                },
                            )
                        },
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("google_sign_in_button"),
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.5.dp,
                            )
                            Spacer(Modifier.width(10.dp))
                            Text("Conectando...")
                        } else {
                            Icon(Icons.Default.AccountCircle, contentDescription = null)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = if (authMode == 1) "Crear usuario con Google" else "Iniciar sesión con Google",
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    if (authMode == 0) {
                        TextButton(
                            onClick = {
                                authMode = 1
                                errorMessage = null
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("switch_to_register_button"),
                        ) {
                            Text("¿Aún no tienes usuario? Créalo con tus datos y Google")
                        }
                    }

                    Text(
                        text = "Acceso seguro con Google Credential Manager y datos privados protegidos en Firestore.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileOnboardingScreen(
    googleUser: FirebaseUser,
    isSaving: Boolean,
    onSaveProfile: (UserProfile) -> Unit,
    onSignOut: () -> Unit,
) {
    var fullName by remember(googleUser.uid) { mutableStateOf(googleUser.displayName.orEmpty()) }
    var phone by remember(googleUser.uid) { mutableStateOf(googleUser.phoneNumber.orEmpty()) }
    var location by remember(googleUser.uid) { mutableStateOf("San José de Albán, Nariño") }
    var selectedRole by remember(googleUser.uid) { mutableStateOf(AppRole.CAFICULTOR) }
    var farmName by remember(googleUser.uid) { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(22.dp),
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(50),
                    ) {
                        Text(
                            text = "COMPLETA TU PERFIL",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                    Text(
                        text = "Personaliza tu cuenta",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "Cuenta vinculada: ${googleUser.email.orEmpty()}. Ingresa el nombre de la persona, contacto celular y lugar para saludarte en el panel y gestionar tu cuenta.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    OutlinedTextField(
                        value = fullName,
                        onValueChange = {
                            fullName = it
                            if (validationError != null) validationError = null
                        },
                        label = { Text("Nombre de la persona *") },
                        placeholder = { Text("Ej. Carlos Andrés Gómez") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("onboarding_name_input"),
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = {
                            phone = it
                            if (validationError != null) validationError = null
                        },
                        label = { Text("Contacto celular / WhatsApp *") },
                        placeholder = { Text("Ej. 312 456 7890") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("onboarding_phone_input"),
                    )

                    OutlinedTextField(
                        value = location,
                        onValueChange = {
                            location = it
                            if (validationError != null) validationError = null
                        },
                        label = { Text("Lugar (vereda o municipio) *") },
                        placeholder = { Text("Ej. Vereda El Carmen, San José de Albán") },
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("onboarding_location_input"),
                    )

                    Text(
                        text = "Rol permanente en la plataforma *",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    RoleSelectionList(
                        selectedRole = selectedRole,
                        onSelectRole = { selectedRole = it },
                        tagPrefix = "onboarding",
                    )

                    val orgLabel = when (selectedRole) {
                        AppRole.CAFICULTOR -> "Finca principal (opcional)"
                        AppRole.PROVEEDOR -> "Almacén o empresa (opcional)"
                        AppRole.COMPRADOR -> "Agencia o punto de compra (opcional)"
                        AppRole.AGRONOMO -> "Entidad o zona de atención (opcional)"
                    }
                    OutlinedTextField(
                        value = farmName,
                        onValueChange = { farmName = it },
                        label = { Text(orgLabel) },
                        leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    if (validationError != null) {
                        Text(
                            text = validationError.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }

                    Button(
                        onClick = {
                            if (fullName.trim().length < 2) {
                                validationError = "Ingresa el nombre de la persona (mínimo 2 caracteres)."
                                return@Button
                            }
                            if (phone.trim().length < 5) {
                                validationError = "Ingresa un número de contacto celular válido."
                                return@Button
                            }
                            if (location.trim().length < 2) {
                                validationError = "Ingresa tu lugar (vereda o municipio)."
                                return@Button
                            }
                            validationError = null
                            onSaveProfile(
                                UserProfile(
                                    userId = googleUser.uid,
                                    fullName = fullName.trim(),
                                    email = googleUser.email.orEmpty(),
                                    phone = phone.trim(),
                                    location = location.trim(),
                                    roleId = selectedRole.roleId,
                                    role = selectedRole.displayName,
                                    roleLocked = true,
                                    permissions = selectedRole.capabilities,
                                    farmName = farmName.trim(),
                                )
                            )
                        },
                        enabled = !isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("complete_profile_button"),
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Guardando...")
                        } else {
                            Text("Guardar y entrar", fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = onSignOut,
                        enabled = !isSaving,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Cambiar cuenta de Google")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UserProfileFormDialog(
    initial: UserProfile,
    googleEmail: String,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (UserProfile) -> Unit,
) {
    var fullName by remember(initial) { mutableStateOf(initial.fullName) }
    var phone by remember(initial) { mutableStateOf(initial.phone) }
    var location by remember(initial) { mutableStateOf(initial.location.ifBlank { "San José de Albán, Nariño" }) }
    var farmName by remember(initial) { mutableStateOf(initial.farmName) }
    val lockedRole = initial.resolvedAppRole

    val isValid = fullName.trim().length >= 2 && phone.trim().length >= 5 && location.trim().length >= 2

    FormModalDialog(
        title = "Mis datos de usuario",
        subtitle = "Cuenta Google: $googleEmail",
        onDismiss = onDismiss,
        confirmLabel = "Guardar cambios",
        confirmEnabled = isValid && !isSaving,
        onConfirm = {
            onSave(
                initial.copy(
                    fullName = fullName.trim(),
                    email = googleEmail,
                    phone = phone.trim(),
                    location = location.trim(),
                    farmName = farmName.trim(),
                )
            )
        },
    ) {
        OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = { Text("Nombre de la persona *") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("edit_profile_name_input"),
        )
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Contacto celular / WhatsApp *") },
            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("edit_profile_phone_input"),
        )
        OutlinedTextField(
            value = location,
            onValueChange = { location = it },
            label = { Text("Lugar (vereda o municipio) *") },
            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("edit_profile_location_input"),
        )
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = SagePale.copy(alpha = 0.72f),
            border = BorderStroke(1.dp, ForestGreen.copy(alpha = 0.25f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("edit_profile_locked_role"),
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Rol permanente bloqueado",
                    tint = ForestGreen,
                    modifier = Modifier.size(18.dp),
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Rol permanente: ${lockedRole.displayName}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = ForestGreen,
                    )
                    Text(
                        text = "El rol asignado en tu registro enfoca la aplicación y no puede cambiarse.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ForestGreen.copy(alpha = 0.85f),
                    )
                }
            }
        }
        val orgLabel = when (lockedRole) {
            AppRole.CAFICULTOR -> "Finca principal (opcional)"
            AppRole.PROVEEDOR -> "Almacén o empresa (opcional)"
            AppRole.COMPRADOR -> "Agencia o punto de compra (opcional)"
            AppRole.AGRONOMO -> "Entidad o zona de atención (opcional)"
        }
        OutlinedTextField(
            value = farmName,
            onValueChange = { farmName = it },
            label = { Text(orgLabel) },
            leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun RoleSelectionList(
    selectedRole: AppRole,
    onSelectRole: (AppRole) -> Unit,
    tagPrefix: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AppRole.entries.forEach { roleOption ->
            val isSelected = selectedRole == roleOption
            val titleText = if (roleOption == AppRole.CAFICULTOR) {
                "Caficultor / Agricultor"
            } else {
                roleOption.displayName
            }
            Surface(
                onClick = { onSelectRole(roleOption) },
                shape = RoundedCornerShape(14.dp),
                color = if (isSelected) SagePale else MaterialTheme.colorScheme.background,
                border = BorderStroke(
                    width = if (isSelected) 1.5.dp else 1.dp,
                    color = if (isSelected) ForestGreen else MaterialTheme.colorScheme.outline,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("${tagPrefix}_role_${roleOption.roleId}"),
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = titleText,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) ForestGreen else MaterialTheme.colorScheme.primary,
                        )
                        if (isSelected) {
                            Surface(color = ForestGreen, shape = RoundedCornerShape(50)) {
                                Text(
                                    text = "Seleccionado",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                )
                            }
                        }
                    }
                    Text(
                        text = roleOption.summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) ForestGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (isSelected) {
                        Spacer(Modifier.height(2.dp))
                        roleOption.ownFocusBullets.take(3).forEach { bullet ->
                            Text(
                                text = "• $bullet",
                                style = MaterialTheme.typography.labelSmall,
                                color = ForestGreen,
                            )
                        }
                    }
                }
            }
        }
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = "Al registrarte, la app se enfocará en este rol y no podrá cambiarse después ni editar datos de otros roles.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
