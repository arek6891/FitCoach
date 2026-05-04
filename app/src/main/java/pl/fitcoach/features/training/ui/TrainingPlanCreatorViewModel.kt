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
import pl.fitcoach.features.training.domain.model.Exercise
import pl.fitcoach.features.training.domain.usecase.AddExerciseToDayUseCase
import pl.fitcoach.features.training.domain.usecase.CreateTrainingDayUseCase
import pl.fitcoach.features.training.domain.usecase.CreateTrainingPlanUseCase
import pl.fitcoach.features.training.domain.usecase.GetExercisesUseCase
import java.util.UUID
import javax.inject.Inject

data class ExerciseDraft(
    val exercise: Exercise,
    val sets: Int = 3,
    val reps: Int? = 10,
    val weightKg: Double? = null,
    val durationSec: Int? = null,
    val restSeconds: Int = 90,
    val notes: String? = null
)

data class TrainingDayDraft(
    val localId: String = UUID.randomUUID().toString(),
    val name: String = "",
    val exercises: List<ExerciseDraft> = emptyList()
)

data class TrainingPlanCreatorUiState(
    val planName: String = "",
    val planDescription: String = "",
    val days: List<TrainingDayDraft> = emptyList(),
    val availableExercises: List<Exercise> = emptyList(),
    val isLoadingExercises: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val isSaved: Boolean = false,
    val showExercisePicker: Boolean = false,
    val pickerTargetDayLocalId: String? = null
)

sealed class TrainingPlanCreatorEvent {
    data class PlanNameChanged(val name: String) : TrainingPlanCreatorEvent()
    data class PlanDescriptionChanged(val desc: String) : TrainingPlanCreatorEvent()
    data object AddDay : TrainingPlanCreatorEvent()
    data class DayNameChanged(val localId: String, val name: String) : TrainingPlanCreatorEvent()
    data class RemoveDay(val localId: String) : TrainingPlanCreatorEvent()
    data class ShowExercisePicker(val dayLocalId: String) : TrainingPlanCreatorEvent()
    data object DismissExercisePicker : TrainingPlanCreatorEvent()
    data class ExerciseSelected(val exercise: Exercise) : TrainingPlanCreatorEvent()
    data class RemoveExercise(val dayLocalId: String, val exerciseId: String) : TrainingPlanCreatorEvent()
    data object SavePlan : TrainingPlanCreatorEvent()
    data object ErrorDismissed : TrainingPlanCreatorEvent()
    data object SaveHandled : TrainingPlanCreatorEvent()
}

@HiltViewModel
class TrainingPlanCreatorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getExercisesUseCase: GetExercisesUseCase,
    private val createTrainingPlanUseCase: CreateTrainingPlanUseCase,
    private val createTrainingDayUseCase: CreateTrainingDayUseCase,
    private val addExerciseToDayUseCase: AddExerciseToDayUseCase
) : ViewModel() {

    private val clientId: String = checkNotNull(savedStateHandle["clientId"])

    private val _uiState = MutableStateFlow(TrainingPlanCreatorUiState())
    val uiState: StateFlow<TrainingPlanCreatorUiState> = _uiState.asStateFlow()

    init {
        loadExercises()
    }

    fun onEvent(event: TrainingPlanCreatorEvent) {
        when (event) {
            is TrainingPlanCreatorEvent.PlanNameChanged ->
                _uiState.update { it.copy(planName = event.name) }

            is TrainingPlanCreatorEvent.PlanDescriptionChanged ->
                _uiState.update { it.copy(planDescription = event.desc) }

            is TrainingPlanCreatorEvent.AddDay -> {
                _uiState.update { state ->
                    state.copy(days = state.days + TrainingDayDraft())
                }
            }

            is TrainingPlanCreatorEvent.DayNameChanged -> {
                _uiState.update { state ->
                    state.copy(
                        days = state.days.map { day ->
                            if (day.localId == event.localId) day.copy(name = event.name) else day
                        }
                    )
                }
            }

            is TrainingPlanCreatorEvent.RemoveDay -> {
                _uiState.update { state ->
                    state.copy(days = state.days.filter { it.localId != event.localId })
                }
            }

            is TrainingPlanCreatorEvent.ShowExercisePicker -> {
                _uiState.update { it.copy(showExercisePicker = true, pickerTargetDayLocalId = event.dayLocalId) }
            }

            is TrainingPlanCreatorEvent.DismissExercisePicker -> {
                _uiState.update { it.copy(showExercisePicker = false, pickerTargetDayLocalId = null) }
            }

            is TrainingPlanCreatorEvent.ExerciseSelected -> {
                val targetDayId = _uiState.value.pickerTargetDayLocalId ?: return
                _uiState.update { state ->
                    state.copy(
                        showExercisePicker = false,
                        pickerTargetDayLocalId = null,
                        days = state.days.map { day ->
                            if (day.localId == targetDayId) {
                                day.copy(exercises = day.exercises + ExerciseDraft(exercise = event.exercise))
                            } else {
                                day
                            }
                        }
                    )
                }
            }

            is TrainingPlanCreatorEvent.RemoveExercise -> {
                _uiState.update { state ->
                    state.copy(
                        days = state.days.map { day ->
                            if (day.localId == event.dayLocalId) {
                                day.copy(
                                    exercises = day.exercises.filter {
                                        it.exercise.id != event.exerciseId
                                    }
                                )
                            } else {
                                day
                            }
                        }
                    )
                }
            }

            is TrainingPlanCreatorEvent.SavePlan -> savePlan()

            is TrainingPlanCreatorEvent.ErrorDismissed ->
                _uiState.update { it.copy(error = null) }

            is TrainingPlanCreatorEvent.SaveHandled ->
                _uiState.update { it.copy(isSaved = false) }
        }
    }

    private fun loadExercises() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingExercises = true) }
            getExercisesUseCase()
                .onSuccess { exercises ->
                    _uiState.update { it.copy(isLoadingExercises = false, availableExercises = exercises) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoadingExercises = false, error = mapError(error)) }
                }
        }
    }

    private fun savePlan() {
        val state = _uiState.value
        if (state.planName.isBlank()) {
            _uiState.update { it.copy(error = "Podaj nazwę planu treningowego") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }

            // Krok 1: utwórz plan
            val planResult = createTrainingPlanUseCase(
                clientId = clientId,
                name = state.planName.trim(),
                description = state.planDescription.trim().takeIf { it.isNotBlank() }
            )
            if (planResult.isFailure) {
                _uiState.update { it.copy(isSaving = false, error = mapError(planResult.exceptionOrNull())) }
                return@launch
            }
            val plan = planResult.getOrThrow()

            // Krok 2: utwórz dni i ćwiczenia sekwencyjnie
            state.days.forEachIndexed { dayIndex, dayDraft ->
                val dayResult = createTrainingDayUseCase(
                    planId = plan.id,
                    name = dayDraft.name.trim().ifBlank { "Dzień ${dayIndex + 1}" },
                    dayOrder = dayIndex + 1
                )
                if (dayResult.isFailure) {
                    _uiState.update { it.copy(isSaving = false, error = mapError(dayResult.exceptionOrNull())) }
                    return@launch
                }
                val day = dayResult.getOrThrow()

                // Krok 3: dodaj ćwiczenia do dnia
                dayDraft.exercises.forEachIndexed { exerciseIndex, exerciseDraft ->
                    val exerciseResult = addExerciseToDayUseCase(
                        dayId = day.id,
                        exercise = exerciseDraft.exercise,
                        sets = exerciseDraft.sets,
                        reps = exerciseDraft.reps,
                        weightKg = exerciseDraft.weightKg,
                        durationSec = exerciseDraft.durationSec,
                        restSeconds = exerciseDraft.restSeconds,
                        exerciseOrder = exerciseIndex + 1,
                        notes = exerciseDraft.notes
                    )
                    if (exerciseResult.isFailure) {
                        _uiState.update { it.copy(isSaving = false, error = mapError(exerciseResult.exceptionOrNull())) }
                        return@launch
                    }
                }
            }

            _uiState.update { it.copy(isSaving = false, isSaved = true) }
        }
    }

    private fun mapError(error: Throwable?): String = when {
        error?.message?.contains("network", ignoreCase = true) == true ||
            error?.message?.contains("Unable to resolve host") == true ->
            "Brak połączenia z internetem"
        else -> "Wystąpił błąd. Spróbuj ponownie"
    }
}
