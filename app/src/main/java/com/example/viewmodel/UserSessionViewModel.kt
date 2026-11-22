package com.example.viewmodel

import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CafeRepository
import com.example.firebase.authStateFlow
import com.example.model.AppRole
import com.example.model.PendingUserRegistration
import com.example.model.RoleCapabilities
import com.example.model.RoleDefinition
import com.example.model.UiState
import com.example.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface UserSessionUiState {
    data object Unauthenticated : UserSessionUiState

    data class Loading(
        val user: FirebaseUser,
        val message: String = "Cargando tu cuenta...",
    ) : UserSessionUiState

    data class NeedsOnboarding(
        val user: FirebaseUser,
        val availableRoles: List<RoleDefinition> = AppRole.entries.map { it.toDefaultDefinition() },
    ) : UserSessionUiState

    data class Authenticated(
        val user: FirebaseUser,
        val profile: UserProfile,
        val appRole: AppRole,
        val roleDefinition: RoleDefinition,
        val capabilities: RoleCapabilities,
        val displayName: String,
    ) : UserSessionUiState {
        val isCaficultor: Boolean get() = appRole == AppRole.CAFICULTOR
        val isAgronomo: Boolean get() = appRole == AppRole.AGRONOMO
        val isProveedor: Boolean get() = appRole == AppRole.PROVEEDOR
        val isComprador: Boolean get() = appRole == AppRole.COMPRADOR

        val roleDisplayLabel: String
            get() = roleDefinition.displayName.ifBlank { appRole.displayName }

        fun hasCapability(check: (RoleCapabilities) -> Boolean): Boolean = check(capabilities)
    }

    data class Error(
        val user: FirebaseUser,
        val message: String,
    ) : UserSessionUiState
}

/**
 * ViewModel dedicado a gestionar la sesión de Google Auth y el perfil/rol permanente
 * almacenado en Firestore (`/users/{uid}` y `/roles`), garantizando que la interfaz
 * se renderice dinámicamente según el rol verificado en la nube.
 */
class UserSessionViewModel(
    private val repository: CafeRepository,
    private val auth: FirebaseAuth = Firebase.auth,
) : ViewModel() {

    private val _pendingRegistration = MutableStateFlow<PendingUserRegistration?>(null)
    private val _retryTrigger = MutableStateFlow(0)

    private val _isSavingProfile = MutableStateFlow(false)
    val isSavingProfile: StateFlow<Boolean> = _isSavingProfile.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private var handledPendingUid: String? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    val sessionState: StateFlow<UserSessionUiState> = combine(
        auth.authStateFlow().distinctUntilChangedBy { it?.uid },
        _retryTrigger,
    ) { firebaseUser, _ -> firebaseUser }
        .flatMapLatest { firebaseUser ->
            if (firebaseUser == null) {
                handledPendingUid = null
                flowOf(UserSessionUiState.Unauthenticated)
            } else {
                observeAuthenticatedSession(firebaseUser)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = auth.currentUser?.let { UserSessionUiState.Loading(it) }
                ?: UserSessionUiState.Unauthenticated,
        )

    val activeAppRole: StateFlow<AppRole?> = sessionState
        .map { state -> (state as? UserSessionUiState.Authenticated)?.appRole }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = null,
        )

    private fun observeAuthenticatedSession(firebaseUser: FirebaseUser): Flow<UserSessionUiState> {
        val profileStream: Flow<UiState<UserProfile?>> =
            repository.observeUserProfile(firebaseUser.uid)
                .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val rootError = firestoreError ?: error
                    Log.w(TAG, "Error observing user profile for ${firebaseUser.uid}", rootError)
                    emit(
                        UiState.Error(
                            rootError.localizedMessage
                                ?: "No pudimos cargar tu perfil y rol desde Firestore."
                        )
                    )
                }

        return combine(
            profileStream,
            repository.observeRoleDefinitions(),
            _pendingRegistration,
        ) { profileState, roleDefinitions, pending ->
            if (pending != null) {
                maybePersistPendingRegistration(firebaseUser, pending)
                return@combine UserSessionUiState.Loading(
                    user = firebaseUser,
                    message = "Creando tu usuario cafetero...",
                )
            }

            when (profileState) {
                is UiState.Loading -> UserSessionUiState.Loading(
                    user = firebaseUser,
                    message = "Cargando tu cuenta...",
                )
                is UiState.Error -> UserSessionUiState.Error(
                    user = firebaseUser,
                    message = profileState.message,
                )
                is UiState.Success -> {
                    val profile = profileState.data
                    if (profile == null) {
                        UserSessionUiState.NeedsOnboarding(
                            user = firebaseUser,
                            availableRoles = roleDefinitions,
                        )
                    } else {
                        val appRole = profile.resolvedAppRole
                        val roleDefinition = roleDefinitions.firstOrNull { it.roleId == appRole.roleId }
                            ?: appRole.toDefaultDefinition()
                        val displayName = profile.fullName.takeIf { it.isNotBlank() }
                            ?: firebaseUser.displayName?.takeIf { it.isNotBlank() }
                            ?: appRole.displayName
                        UserSessionUiState.Authenticated(
                            user = firebaseUser,
                            profile = profile,
                            appRole = appRole,
                            roleDefinition = roleDefinition,
                            capabilities = appRole.capabilities,
                            displayName = displayName,
                        )
                    }
                }
            }
        }
    }

    fun prepareRegistration(registration: PendingUserRegistration?) {
        handledPendingUid = null
        _pendingRegistration.value = registration
    }

    fun clearPendingRegistration() {
        _pendingRegistration.value = null
    }

    fun retryProfileObservation() {
        _retryTrigger.value += 1
    }

    private fun maybePersistPendingRegistration(
        firebaseUser: FirebaseUser,
        pending: PendingUserRegistration,
    ) {
        if (handledPendingUid == firebaseUser.uid) return
        handledPendingUid = firebaseUser.uid

        val resolvedRole = AppRole.fromRoleOrId(pending.roleId, pending.role)
        val profile = UserProfile(
            userId = firebaseUser.uid,
            fullName = pending.fullName,
            email = firebaseUser.email.orEmpty(),
            phone = pending.phone,
            location = pending.location,
            roleId = resolvedRole.roleId,
            role = resolvedRole.displayName,
            roleLocked = true,
            permissions = resolvedRole.capabilities,
            farmName = pending.farmName,
        )
        saveUserProfile(
            profile = profile,
            onError = {
                handledPendingUid = null
                _pendingRegistration.value = null
            },
            onDone = {
                _pendingRegistration.value = null
            },
        )
    }

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

        val resolvedRole = profile.resolvedAppRole
        val cleanProfile = profile.copy(
            fullName = profile.fullName.trim(),
            phone = profile.phone.trim(),
            location = profile.location.trim(),
            farmName = profile.farmName.trim(),
            roleId = resolvedRole.roleId,
            role = resolvedRole.displayName,
            roleLocked = true,
            permissions = resolvedRole.capabilities,
        )

        viewModelScope.launch {
            _isSavingProfile.value = true
            try {
                repository.saveUserProfile(cleanProfile)
                    .onSuccess {
                        notify("Datos personales guardados.")
                        onDone()
                    }
                    .onFailure { error ->
                        notify(error.localizedMessage ?: "No se pudieron guardar tus datos de perfil.")
                        onError()
                    }
            } catch (e: CancellationException) {
                throw e
            } finally {
                _isSavingProfile.value = false
            }
        }
    }

    fun signOut(
        credentialManager: CredentialManager,
        onComplete: () -> Unit = {},
    ) {
        viewModelScope.launch {
            withContext(NonCancellable) {
                try {
                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to clear credential state: ${e.message}")
                } finally {
                    _pendingRegistration.value = null
                    handledPendingUid = null
                    auth.signOut()
                    onComplete()
                }
            }
        }
    }

    private fun notify(message: String) {
        _messages.tryEmit(message)
    }

    companion object {
        private const val TAG = "UserSessionVM"
    }
}
