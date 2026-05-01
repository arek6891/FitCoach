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
import pl.fitcoach.features.auth.domain.usecase.LoginUseCase
import javax.inject.Inject

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val isPasswordVisible: Boolean = false,
    val error: String? = null,
    val loggedInRole: UserRole? = null
)

sealed class LoginEvent {
    data class EmailChanged(val email: String) : LoginEvent()
    data class PasswordChanged(val password: String) : LoginEvent()
    data object TogglePasswordVisibility : LoginEvent()
    data object LoginClicked : LoginEvent()
    data object ErrorDismissed : LoginEvent()
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEvent(event: LoginEvent) {
        when (event) {
            is LoginEvent.EmailChanged ->
                _uiState.update { it.copy(email = event.email, error = null) }

            is LoginEvent.PasswordChanged ->
                _uiState.update { it.copy(password = event.password, error = null) }

            is LoginEvent.TogglePasswordVisibility ->
                _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }

            is LoginEvent.LoginClicked -> login()

            is LoginEvent.ErrorDismissed ->
                _uiState.update { it.copy(error = null) }
        }
    }

    private fun login() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            loginUseCase(_uiState.value.email, _uiState.value.password)
                .onSuccess { user ->
                    _uiState.update { it.copy(isLoading = false, loggedInRole = user.role) }
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
        error.message?.contains("Invalid login credentials") == true ->
            "Nieprawidłowy email lub hasło"
        error.message?.contains("Email nie jest") == true -> error.message!!
        error.message?.contains("Hasło") == true -> error.message!!
        error.message?.contains("network") == true ->
            "Brak połączenia z internetem"
        else -> "Wystąpił błąd. Spróbuj ponownie"
    }
}
