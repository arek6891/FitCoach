package pl.fitcoach.features.training.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.fitcoach.features.training.domain.model.TrainingPlan
import pl.fitcoach.features.training.domain.usecase.GetTrainingPlansUseCase
import javax.inject.Inject

data class TrainingPlanListUiState(
    val plans: List<TrainingPlan> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed class TrainingPlanListEvent {
    data object Refresh : TrainingPlanListEvent()
    data object ErrorDismissed : TrainingPlanListEvent()
}

@HiltViewModel
class TrainingPlanListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getTrainingPlansUseCase: GetTrainingPlansUseCase
) : ViewModel() {

    val clientId: String = checkNotNull(savedStateHandle["clientId"])

    private val _uiState = MutableStateFlow(TrainingPlanListUiState())
    val uiState: StateFlow<TrainingPlanListUiState> = _uiState.asStateFlow()

    init {
        loadPlans()
    }

    fun onEvent(event: TrainingPlanListEvent) {
        when (event) {
            is TrainingPlanListEvent.Refresh -> loadPlans()
            is TrainingPlanListEvent.ErrorDismissed ->
                _uiState.update { it.copy(error = null) }
        }
    }

    private fun loadPlans() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            getTrainingPlansUseCase(clientId)
                .onSuccess { plans ->
                    _uiState.update { it.copy(isLoading = false, plans = plans) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, error = mapError(error))
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
