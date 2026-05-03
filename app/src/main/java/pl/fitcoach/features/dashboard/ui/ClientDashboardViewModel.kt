package pl.fitcoach.features.dashboard.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.fitcoach.features.auth.domain.repository.AuthRepository
import pl.fitcoach.features.dashboard.domain.usecase.GetClientDashboardUseCase
import pl.fitcoach.features.dashboard.domain.usecase.LogHabitUseCase
import pl.fitcoach.features.habits.domain.model.Habit
import pl.fitcoach.features.training.domain.model.TrainingDay
import pl.fitcoach.features.training.domain.model.TrainingPlan
import java.time.LocalDate
import javax.inject.Inject

data class ClientDashboardUiState(
    val clientName: String = "",
    val goal: String? = null,
    val activePlan: TrainingPlan? = null,
    val habits: List<Habit> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val isLoggedOut: Boolean = false
) {
    // dayOfWeek: Mon=1..Sun=7, mapped to 0=Sun, 1=Mon... matching Supabase day_order convention
    val todayDayOfWeek: Int get() = LocalDate.now().dayOfWeek.value % 7
    val todayTrainingDay: TrainingDay? get() = activePlan?.days?.find { it.dayOrder == todayDayOfWeek }
    val completedHabitsCount: Int get() = habits.count { it.isCompleted }
}

sealed class ClientDashboardEvent {
    data object Refresh : ClientDashboardEvent()
    data object LogoutClicked : ClientDashboardEvent()
    data object LogoutHandled : ClientDashboardEvent()
    data object ErrorDismissed : ClientDashboardEvent()
    data class HabitToggled(val habitId: String, val completed: Boolean) : ClientDashboardEvent()
    data class HabitValueChanged(val habitId: String, val value: Double?) : ClientDashboardEvent()
}

@OptIn(FlowPreview::class)
@HiltViewModel
class ClientDashboardViewModel @Inject constructor(
    private val getClientDashboardUseCase: GetClientDashboardUseCase,
    private val logHabitUseCase: LogHabitUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ClientDashboardUiState())
    val uiState: StateFlow<ClientDashboardUiState> = _uiState.asStateFlow()

    // Debounce stream for quantity habit changes: habitId → value
    private val habitValueFlow = MutableSharedFlow<Pair<String, Double?>>(extraBufferCapacity = 16)

    init {
        loadData()
        observeHabitValueDebounced()
    }

    fun onEvent(event: ClientDashboardEvent) {
        when (event) {
            is ClientDashboardEvent.Refresh -> loadData()
            is ClientDashboardEvent.LogoutClicked -> logout()
            is ClientDashboardEvent.LogoutHandled ->
                _uiState.update { it.copy(isLoggedOut = false) }
            is ClientDashboardEvent.ErrorDismissed ->
                _uiState.update { it.copy(error = null) }
            is ClientDashboardEvent.HabitToggled ->
                toggleHabit(event.habitId, event.completed)
            is ClientDashboardEvent.HabitValueChanged ->
                onHabitValueChanged(event.habitId, event.value)
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val (profileResult, planResult, habitsResult) = getClientDashboardUseCase()

            val firstError = profileResult.exceptionOrNull()
                ?: habitsResult.exceptionOrNull()

            _uiState.update {
                it.copy(
                    isLoading = false,
                    clientName = profileResult.getOrNull()?.firstName ?: it.clientName,
                    goal = profileResult.getOrNull()?.goal ?: it.goal,
                    activePlan = planResult.getOrNull(),
                    habits = habitsResult.getOrElse { emptyList() },
                    error = firstError?.let { err -> mapError(err) }
                )
            }
        }
    }

    private fun toggleHabit(habitId: String, completed: Boolean) {
        // Optimistic update
        _uiState.update { state ->
            state.copy(
                habits = state.habits.map { habit ->
                    if (habit.id == habitId) habit.copy(isCompleted = completed) else habit
                }
            )
        }
        viewModelScope.launch {
            logHabitUseCase(habitId = habitId, completed = completed)
                .onFailure { err ->
                    // Revert optimistic update on failure
                    _uiState.update { state ->
                        state.copy(
                            habits = state.habits.map { habit ->
                                if (habit.id == habitId) habit.copy(isCompleted = !completed) else habit
                            },
                            error = mapError(err)
                        )
                    }
                }
        }
    }

    private fun onHabitValueChanged(habitId: String, value: Double?) {
        // Optimistic update in UI immediately
        _uiState.update { state ->
            state.copy(
                habits = state.habits.map { habit ->
                    if (habit.id == habitId) {
                        habit.copy(
                            currentValue = value,
                            isCompleted = value != null && habit.targetValue != null && value >= habit.targetValue
                        )
                    } else {
                        habit
                    }
                }
            )
        }
        viewModelScope.launch {
            habitValueFlow.emit(habitId to value)
        }
    }

    private fun observeHabitValueDebounced() {
        viewModelScope.launch {
            habitValueFlow
                .debounce(600L)
                .collect { (habitId, value) ->
                    val habit = _uiState.value.habits.find { it.id == habitId } ?: return@collect
                    logHabitUseCase(
                        habitId = habitId,
                        completed = habit.isCompleted,
                        value = value
                    ).onFailure { err ->
                        _uiState.update { it.copy(error = mapError(err)) }
                    }
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
