package pl.fitcoach.features.clients.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.fitcoach.features.clients.domain.model.Client
import pl.fitcoach.features.clients.domain.usecase.GetClientByIdUseCase
import javax.inject.Inject

data class ClientDetailUiState(
    val client: Client? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed class ClientDetailEvent {
    data object Retry : ClientDetailEvent()
    data object ErrorDismissed : ClientDetailEvent()
}

@HiltViewModel
class ClientDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getClientByIdUseCase: GetClientByIdUseCase
) : ViewModel() {

    private val clientId: String = checkNotNull(savedStateHandle["clientId"])

    private val _uiState = MutableStateFlow(ClientDetailUiState())
    val uiState: StateFlow<ClientDetailUiState> = _uiState.asStateFlow()

    init {
        loadClient()
    }

    fun onEvent(event: ClientDetailEvent) {
        when (event) {
            is ClientDetailEvent.Retry -> loadClient()
            is ClientDetailEvent.ErrorDismissed ->
                _uiState.update { it.copy(error = null) }
        }
    }

    private fun loadClient() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            getClientByIdUseCase(clientId)
                .onSuccess { client ->
                    _uiState.update { it.copy(isLoading = false, client = client) }
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
        error.message?.contains("network", ignoreCase = true) == true ||
            error.message?.contains("Unable to resolve host") == true ->
            "Brak połączenia z internetem"
        else -> "Wystąpił błąd. Spróbuj ponownie"
    }
}
