package pl.fitcoach.features.workout.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.fitcoach.features.training.domain.model.TrainingDayExercise
import pl.fitcoach.features.workout.domain.usecase.CompleteWorkoutSessionUseCase
import pl.fitcoach.features.workout.domain.usecase.GetTrainingDayUseCase
import pl.fitcoach.features.workout.domain.usecase.LogExerciseSetUseCase
import pl.fitcoach.features.workout.domain.usecase.StartWorkoutSessionUseCase
import javax.inject.Inject

// ─── UI State ────────────────────────────────────────────────────────────────

data class SetInputState(
    val targetReps: Int?,
    val targetWeight: Double?,
    val actualReps: String = targetReps?.toString() ?: "",
    val actualWeight: String = targetWeight?.toString() ?: "",
    val isCompleted: Boolean = false
)

data class ExerciseWorkoutState(
    val dayExercise: TrainingDayExercise,
    val sets: List<SetInputState>
)

data class RestTimerState(
    val totalSeconds: Int,
    val remainingSeconds: Int
)

data class ActiveWorkoutUiState(
    val dayName: String = "",
    val exercises: List<ExerciseWorkoutState> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val isSessionFinished: Boolean = false,
    val elapsedSeconds: Int = 0,
    val restTimer: RestTimerState? = null,
    val showFinishDialog: Boolean = false
)

// ─── Events ──────────────────────────────────────────────────────────────────

sealed class ActiveWorkoutEvent {
    data class RepsChanged(val exIdx: Int, val setIdx: Int, val value: String) :
        ActiveWorkoutEvent()
    data class WeightChanged(val exIdx: Int, val setIdx: Int, val value: String) :
        ActiveWorkoutEvent()
    data class SetCompleted(val exIdx: Int, val setIdx: Int) : ActiveWorkoutEvent()
    data object SkipRestTimer : ActiveWorkoutEvent()
    data object FinishWorkoutClicked : ActiveWorkoutEvent()
    data class ConfirmFinish(val notes: String) : ActiveWorkoutEvent()
    data object DismissFinishDialog : ActiveWorkoutEvent()
    data object ErrorDismissed : ActiveWorkoutEvent()
    data object SessionFinishHandled : ActiveWorkoutEvent()
}

// ─── ViewModel ───────────────────────────────────────────────────────────────

@HiltViewModel
class ActiveWorkoutViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val startWorkoutSessionUseCase: StartWorkoutSessionUseCase,
    private val logExerciseSetUseCase: LogExerciseSetUseCase,
    private val completeWorkoutSessionUseCase: CompleteWorkoutSessionUseCase,
    private val getTrainingDayUseCase: GetTrainingDayUseCase
) : ViewModel() {

    private val planId: String = checkNotNull(savedStateHandle["planId"])
    private val dayId: String = checkNotNull(savedStateHandle["dayId"])

    private val _uiState = MutableStateFlow(ActiveWorkoutUiState())
    val uiState: StateFlow<ActiveWorkoutUiState> = _uiState.asStateFlow()

    private var sessionId: String? = null
    private var elapsedJob: Job? = null
    private var restTimerJob: Job? = null

    init {
        startElapsedTimer()
        initSession()
    }

    fun onEvent(event: ActiveWorkoutEvent) {
        when (event) {
            is ActiveWorkoutEvent.RepsChanged -> onRepsChanged(event.exIdx, event.setIdx, event.value)
            is ActiveWorkoutEvent.WeightChanged -> onWeightChanged(event.exIdx, event.setIdx, event.value)
            is ActiveWorkoutEvent.SetCompleted -> onSetCompleted(event.exIdx, event.setIdx)
            is ActiveWorkoutEvent.SkipRestTimer -> skipRestTimer()
            is ActiveWorkoutEvent.FinishWorkoutClicked ->
                _uiState.update { it.copy(showFinishDialog = true) }
            is ActiveWorkoutEvent.ConfirmFinish -> confirmFinish(event.notes)
            is ActiveWorkoutEvent.DismissFinishDialog ->
                _uiState.update { it.copy(showFinishDialog = false) }
            is ActiveWorkoutEvent.ErrorDismissed ->
                _uiState.update { it.copy(error = null) }
            is ActiveWorkoutEvent.SessionFinishHandled ->
                _uiState.update { it.copy(isSessionFinished = false) }
        }
    }

    // ─── Initialization ──────────────────────────────────────────────────────

    private fun startElapsedTimer() {
        elapsedJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                _uiState.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
            }
        }
    }

    private fun initSession() {
        viewModelScope.launch {
            // Załaduj ćwiczenia i wystartuj sesję równolegle
            val dayResult = getTrainingDayUseCase(dayId)
            if (dayResult.isFailure) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = mapError(dayResult.exceptionOrNull())
                    )
                }
                return@launch
            }

            val trainingDay = dayResult.getOrThrow()
            val exerciseStates = trainingDay.exercises.map { dayExercise ->
                ExerciseWorkoutState(
                    dayExercise = dayExercise,
                    sets = List(dayExercise.sets) { _ ->
                        SetInputState(
                            targetReps = dayExercise.reps,
                            targetWeight = dayExercise.weightKg
                        )
                    }
                )
            }

            _uiState.update {
                it.copy(
                    dayName = trainingDay.name,
                    exercises = exerciseStates,
                    isLoading = false
                )
            }

            // Start sesji w Supabase
            val sessionResult = startWorkoutSessionUseCase(planId, dayId)
            sessionResult.fold(
                onSuccess = { id -> sessionId = id },
                onFailure = { err ->
                    _uiState.update { it.copy(error = mapError(err)) }
                }
            )
        }
    }

    // ─── Set input ───────────────────────────────────────────────────────────

    private fun onRepsChanged(exIdx: Int, setIdx: Int, value: String) {
        _uiState.update { state ->
            state.copy(
                exercises = state.exercises.mapIndexed { eIdx, ex ->
                    if (eIdx != exIdx) ex
                    else ex.copy(
                        sets = ex.sets.mapIndexed { sIdx, set ->
                            if (sIdx != setIdx) set else set.copy(actualReps = value)
                        }
                    )
                }
            )
        }
    }

    private fun onWeightChanged(exIdx: Int, setIdx: Int, value: String) {
        _uiState.update { state ->
            state.copy(
                exercises = state.exercises.mapIndexed { eIdx, ex ->
                    if (eIdx != exIdx) ex
                    else ex.copy(
                        sets = ex.sets.mapIndexed { sIdx, set ->
                            if (sIdx != setIdx) set else set.copy(actualWeight = value)
                        }
                    )
                }
            )
        }
    }

    // ─── Set completion ──────────────────────────────────────────────────────

    private fun onSetCompleted(exIdx: Int, setIdx: Int) {
        val state = _uiState.value
        val exercise = state.exercises.getOrNull(exIdx) ?: return
        val set = exercise.sets.getOrNull(setIdx) ?: return
        if (set.isCompleted) return

        // Optimistic update — oznacz jako ukończone
        _uiState.update { s ->
            s.copy(
                exercises = s.exercises.mapIndexed { eIdx, ex ->
                    if (eIdx != exIdx) ex
                    else ex.copy(
                        sets = ex.sets.mapIndexed { sIdx, setItem ->
                            if (sIdx != setIdx) setItem else setItem.copy(isCompleted = true)
                        }
                    )
                }
            )
        }

        // Uruchom rest timer
        val restSeconds = exercise.dayExercise.restSeconds
        if (restSeconds > 0) {
            startRestTimer(restSeconds)
        }

        // Zapisz serię w tle
        val currentSessionId = sessionId ?: return
        viewModelScope.launch {
            logExerciseSetUseCase(
                sessionId = currentSessionId,
                exerciseId = exercise.dayExercise.exercise.id,
                setNumber = setIdx + 1,
                reps = set.actualReps.toIntOrNull(),
                weightKg = set.actualWeight.toDoubleOrNull(),
                durationSec = exercise.dayExercise.durationSec
            ).onFailure { err ->
                _uiState.update { it.copy(error = mapError(err)) }
            }
        }
    }

    // ─── Rest timer ──────────────────────────────────────────────────────────

    private fun startRestTimer(totalSeconds: Int) {
        restTimerJob?.cancel()
        _uiState.update {
            it.copy(restTimer = RestTimerState(totalSeconds = totalSeconds, remainingSeconds = totalSeconds))
        }
        restTimerJob = viewModelScope.launch {
            repeat(totalSeconds) {
                delay(1000L)
                _uiState.update { s ->
                    val remaining = (s.restTimer?.remainingSeconds ?: 0) - 1
                    if (remaining <= 0) s.copy(restTimer = null)
                    else s.copy(restTimer = s.restTimer?.copy(remainingSeconds = remaining))
                }
            }
        }
    }

    private fun skipRestTimer() {
        restTimerJob?.cancel()
        restTimerJob = null
        _uiState.update { it.copy(restTimer = null) }
    }

    // ─── Finish session ──────────────────────────────────────────────────────

    private fun confirmFinish(notes: String) {
        val currentSessionId = sessionId
        if (currentSessionId == null) {
            // Brak sessionId — wróć bez zapisu
            _uiState.update { it.copy(showFinishDialog = false, isSessionFinished = true) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(showFinishDialog = false, isLoading = true) }
            val durationSec = _uiState.value.elapsedSeconds
            completeWorkoutSessionUseCase(
                sessionId = currentSessionId,
                durationSec = durationSec,
                notes = notes.ifBlank { null }
            ).fold(
                onSuccess = {
                    elapsedJob?.cancel()
                    restTimerJob?.cancel()
                    _uiState.update { it.copy(isLoading = false, isSessionFinished = true) }
                },
                onFailure = { err ->
                    _uiState.update { it.copy(isLoading = false, error = mapError(err)) }
                }
            )
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private fun mapError(error: Throwable?): String = when {
        error == null -> "Wystąpił nieznany błąd"
        error.message?.contains("network", ignoreCase = true) == true ||
            error.message?.contains("Unable to resolve host") == true ->
            "Brak połączenia z internetem"
        else -> "Wystąpił błąd. Spróbuj ponownie"
    }
}
