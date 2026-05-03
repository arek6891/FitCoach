package pl.fitcoach.features.dashboard.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.fitcoach.features.auth.domain.repository.AuthRepository
import pl.fitcoach.features.clients.domain.model.Client
import pl.fitcoach.features.clients.domain.usecase.GetClientsUseCase
import pl.fitcoach.features.clients.domain.usecase.GetTrainerProfileUseCase
import javax.inject.Inject

data class TrainerDashboardUiState(
    val trainerName: String = "",
    val clients: List<Client> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val isLoggedOut: Boolean = false
) {
    val activeClientsCount: Int get() = clients.count { it.isActive }
}

sealed class TrainerDashboardEvent {
    data object Refresh : TrainerDashboardEvent()
    data object LogoutClicked : TrainerDashboardEvent()
    data object ErrorDismissed : TrainerDashboardEvent()
    data object LogoutHandled : TrainerDashboardEvent()
}

@HiltViewModel
class TrainerDashboardViewModel @Inject constructor(
    private val getClientsUseCase: GetClientsUseCase,
    private val getTrainerProfileUseCase: GetTrainerProfileUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrainerDashboardUiState())
    val uiState: StateFlow<TrainerDashboardUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun onEvent(event: TrainerDashboardEvent) {
        when (event) {
            is TrainerDashboardEvent.Refresh -> loadData()
            is TrainerDashboardEvent.LogoutClicked -> logout()
            is TrainerDashboardEvent.ErrorDismissed ->
                _uiState.update { it.copy(error = null) }
            is TrainerDashboardEvent.LogoutHandled ->
                _uiState.update { it.copy(isLoggedOut = false) }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val clientsDeferred = async { getClientsUseCase() }
            val profileDeferred = async { getTrainerProfileUseCase() }

            val clientsResult = clientsDeferred.await()
            val profileResult = profileDeferred.await()

            val error = clientsResult.exceptionOrNull() ?: profileResult.exceptionOrNull()

            _uiState.update {
                it.copy(
                    isLoading = false,
                    clients = clientsResult.getOrElse { emptyList() },
                    trainerName = profileResult.getOrNull()?.firstName ?: "",
                    error = error?.let { mapError(it) }
                )
            }
        }
    }

    private fun logout() {
        viewModelScope.launch {
            runCatching { authRepository.logout() }
            _uiState.update { it.copy(isLoggedOut = true) }
        }
    }

    private fun mapError(error: Throwable): String = when {
        error.message?.contains("network", ignoreCase = true) == true ||
        error.message?.contains("Unable to resolve host") == true ->
            "Brak połączenia z internetem"
        else -> "Wystąpił błąd. Spróbuj ponownie"
    }
}
