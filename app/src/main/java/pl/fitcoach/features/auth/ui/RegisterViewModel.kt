package pl.fitcoach.features.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.fitcoach.features.auth.domain.model.UserRole
import pl.fitcoach.features.auth.domain.usecase.RegisterUseCase
import javax.inject.Inject

data class RegisterUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val selectedRole: UserRole = UserRole.TRAINER,
    val inviteCode: String = "",
    val isLoading: Boolean = false,
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val error: String? = null,
    val registeredRole: UserRole? = null
)

sealed class RegisterEvent {
    data class EmailChanged(val email: String) : RegisterEvent()
    data class PasswordChanged(val password: String) : RegisterEvent()
    data class ConfirmPasswordChanged(val confirmPassword: String) : RegisterEvent()
    data class RoleSelected(val role: UserRole) : RegisterEvent()
    data class InviteCodeChanged(val code: String) : RegisterEvent()
    data object TogglePasswordVisibility : RegisterEvent()
    data object ToggleConfirmPasswordVisibility : RegisterEvent()
    data object RegisterClicked : RegisterEvent()
    data object ErrorDismissed : RegisterEvent()
}

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val registerUseCase: RegisterUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onEvent(event: RegisterEvent) {
        when (event) {
            is RegisterEvent.EmailChanged ->
                _uiState.update { it.copy(email = event.email, error = null) }

            is RegisterEvent.PasswordChanged ->
                _uiState.update { it.copy(password = event.password, error = null) }

            is RegisterEvent.ConfirmPasswordChanged ->
                _uiState.update { it.copy(confirmPassword = event.confirmPassword, error = null) }

            is RegisterEvent.RoleSelected ->
                _uiState.update { it.copy(selectedRole = event.role, inviteCode = "", error = null) }

            is RegisterEvent.InviteCodeChanged ->
                _uiState.update { it.copy(inviteCode = event.code, error = null) }

            is RegisterEvent.TogglePasswordVisibility ->
                _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }

            is RegisterEvent.ToggleConfirmPasswordVisibility ->
                _uiState.update { it.copy(isConfirmPasswordVisible = !it.isConfirmPasswordVisible) }

            is RegisterEvent.RegisterClicked -> register()

            is RegisterEvent.ErrorDismissed ->
                _uiState.update { it.copy(error = null) }
        }
    }

    private fun register() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            registerUseCase(
                email = state.email,
                password = state.password,
                confirmPassword = state.confirmPassword,
                role = state.selectedRole.name.lowercase()
            )
                .onSuccess { user ->
                    _uiState.update { it.copy(isLoading = false, registeredRole = user.role) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = mapError(error)
                        )
                    }
                }
        }
    }

    private fun mapError(error: Throwable): String = when {
        error.message?.contains("already registered") == true ||
            error.message?.contains("already been registered") == true ->
            "Podany adres email jest już zajęty"
        error.message?.contains("Hasła nie są zgodne") == true -> error.message!!
        error.message?.contains("Email") == true -> error.message!!
        error.message?.contains("Hasło") == true -> error.message!!
        error.message?.contains("network") == true ->
            "Brak połączenia z internetem"
        else -> "Wystąpił błąd. Spróbuj ponownie"
    }
}
