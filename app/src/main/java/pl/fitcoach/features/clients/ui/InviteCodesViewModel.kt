package pl.fitcoach.features.clients.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.fitcoach.features.clients.domain.model.InviteCode
import pl.fitcoach.features.clients.domain.usecase.CancelInviteCodeUseCase
import pl.fitcoach.features.clients.domain.usecase.GenerateInviteCodeUseCase
import pl.fitcoach.features.clients.domain.usecase.GetInviteCodesUseCase
import javax.inject.Inject

data class InviteCodesUiState(
    val codes: List<InviteCode> = emptyList(),
    val isLoading: Boolean = false,
    val isGenerating: Boolean = false,
    val error: String? = null,
    val newCodeDialog: NewCodeDialogState? = null,
    val generatedCode: String? = null
)

data class NewCodeDialogState(
    val clientName: String = ""
)

sealed class InviteCodesEvent {
    data object LoadCodes : InviteCodesEvent()
    data object ShowGenerateDialog : InviteCodesEvent()
    data object DismissGenerateDialog : InviteCodesEvent()
    data class ClientNameChanged(val name: String) : InviteCodesEvent()
    data object GenerateCode : InviteCodesEvent()
    data class CancelCode(val codeId: String) : InviteCodesEvent()
    data object ClearGeneratedCode : InviteCodesEvent()
    data object ErrorDismissed : InviteCodesEvent()
}

@HiltViewModel
class InviteCodesViewModel @Inject constructor(
    private val getInviteCodesUseCase: GetInviteCodesUseCase,
    private val generateInviteCodeUseCase: GenerateInviteCodeUseCase,
    private val cancelInviteCodeUseCase: CancelInviteCodeUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(InviteCodesUiState())
    val uiState: StateFlow<InviteCodesUiState> = _uiState.asStateFlow()

    init {
        loadCodes()
    }

    fun onEvent(event: InviteCodesEvent) {
        when (event) {
            is InviteCodesEvent.LoadCodes -> loadCodes()
            is InviteCodesEvent.ShowGenerateDialog -> showGenerateDialog()
            is InviteCodesEvent.DismissGenerateDialog -> dismissGenerateDialog()
            is InviteCodesEvent.ClientNameChanged -> onClientNameChanged(event.name)
            is InviteCodesEvent.GenerateCode -> generateCode()
            is InviteCodesEvent.CancelCode -> cancelCode(event.codeId)
            is InviteCodesEvent.ClearGeneratedCode -> clearGeneratedCode()
            is InviteCodesEvent.ErrorDismissed ->
                _uiState.update { it.copy(error = null) }
        }
    }

    private fun loadCodes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            getInviteCodesUseCase()
                .onSuccess { codes ->
                    _uiState.update { it.copy(isLoading = false, codes = codes) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, error = mapError(error))
                    }
                }
        }
    }

    private fun showGenerateDialog() {
        _uiState.update { it.copy(newCodeDialog = NewCodeDialogState()) }
    }

    private fun dismissGenerateDialog() {
        _uiState.update { it.copy(newCodeDialog = null) }
    }

    private fun onClientNameChanged(name: String) {
        _uiState.update { state ->
            state.copy(newCodeDialog = state.newCodeDialog?.copy(clientName = name))
        }
    }

    private fun generateCode() {
        val clientName = _uiState.value.newCodeDialog?.clientName?.trim()
            ?.takeIf { it.isNotEmpty() }
        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true, newCodeDialog = null) }
            generateInviteCodeUseCase(clientName)
                .onSuccess { code ->
                    loadCodes()
                    _uiState.update { it.copy(isGenerating = false, generatedCode = code) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isGenerating = false, error = mapError(error))
                    }
                }
        }
    }

    private fun cancelCode(codeId: String) {
        val previousCodes = _uiState.value.codes
        _uiState.update { state ->
            state.copy(codes = state.codes.filter { it.id != codeId })
        }
        viewModelScope.launch {
            cancelInviteCodeUseCase(codeId)
                .onFailure { error ->
                    _uiState.update {
                        it.copy(codes = previousCodes, error = mapError(error))
                    }
                }
        }
    }

    private fun clearGeneratedCode() {
        _uiState.update { it.copy(generatedCode = null) }
    }

    private fun mapError(error: Throwable): String = when {
        error.message?.contains("network", ignoreCase = true) == true ||
        error.message?.contains("Unable to resolve host") == true ->
            "Brak połączenia z internetem"
        else -> "Wystąpił błąd. Spróbuj ponownie"
    }
}
