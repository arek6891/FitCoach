package pl.fitcoach.features.workout.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import pl.fitcoach.R
import pl.fitcoach.core.ui.theme.FitCoachTheme
import pl.fitcoach.features.training.domain.model.Exercise
import pl.fitcoach.features.training.domain.model.TrainingDayExercise

// ─── Screen ──────────────────────────────────────────────────────────────────

@Composable
fun ActiveWorkoutScreen(
    navController: NavController,
    viewModel: ActiveWorkoutViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isSessionFinished) {
        if (uiState.isSessionFinished) {
            navController.popBackStack()
            viewModel.onEvent(ActiveWorkoutEvent.SessionFinishHandled)
        }
    }

    ActiveWorkoutContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onBack = { navController.popBackStack() }
    )
}

// ─── Content ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActiveWorkoutContent(
    uiState: ActiveWorkoutUiState,
    onEvent: (ActiveWorkoutEvent) -> Unit,
    onBack: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var showBackConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { message ->
            snackbarHostState.showSnackbar(message)
            onEvent(ActiveWorkoutEvent.ErrorDismissed)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = uiState.dayName.ifBlank {
                                stringResource(R.string.active_workout_title)
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = uiState.elapsedSeconds.toElapsedString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { showBackConfirmDialog = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                WorkoutBody(
                    uiState = uiState,
                    onEvent = onEvent
                )
            }
        }
    }

    // Potwierdzenie opuszczenia treningu
    if (showBackConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showBackConfirmDialog = false },
            title = { Text(stringResource(R.string.active_workout_back_confirm_title)) },
            text = { Text(stringResource(R.string.active_workout_back_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showBackConfirmDialog = false
                    onBack()
                }) {
                    Text(stringResource(R.string.back))
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackConfirmDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Dialog zakończenia treningu
    if (uiState.showFinishDialog) {
        FinishWorkoutDialog(
            onConfirm = { notes -> onEvent(ActiveWorkoutEvent.ConfirmFinish(notes)) },
            onDismiss = { onEvent(ActiveWorkoutEvent.DismissFinishDialog) }
        )
    }
}

// ─── Body ─────────────────────────────────────────────────────────────────────

@Composable
private fun WorkoutBody(
    uiState: ActiveWorkoutUiState,
    onEvent: (ActiveWorkoutEvent) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Rest timer — sticky jako pierwszy element gdy aktywny
        uiState.restTimer?.let { restTimer ->
            item(key = "rest_timer") {
                RestTimerCard(
                    restTimer = restTimer,
                    onSkip = { onEvent(ActiveWorkoutEvent.SkipRestTimer) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }
        }

        // Ćwiczenia
        itemsIndexed(
            items = uiState.exercises,
            key = { _, ex -> ex.dayExercise.id }
        ) { exIdx, exerciseState ->
            ExerciseCard(
                exerciseState = exerciseState,
                exIdx = exIdx,
                onEvent = onEvent,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
        }

        // Przycisk zakończenia
        item(key = "finish_button") {
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = { onEvent(ActiveWorkoutEvent.FinishWorkoutClicked) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(text = stringResource(R.string.active_workout_finish))
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ─── Rest Timer Card ─────────────────────────────────────────────────────────

@Composable
private fun RestTimerCard(
    restTimer: RestTimerState,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.active_workout_rest_timer),
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                Text(
                    text = restTimer.remainingSeconds.toElapsedString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = {
                    if (restTimer.totalSeconds > 0)
                        restTimer.remainingSeconds.toFloat() / restTimer.totalSeconds
                    else 0f
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.active_workout_skip_rest))
            }
        }
    }
}

// ─── Exercise Card ────────────────────────────────────────────────────────────

@Composable
private fun ExerciseCard(
    exerciseState: ExerciseWorkoutState,
    exIdx: Int,
    onEvent: (ActiveWorkoutEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val exercise = exerciseState.dayExercise
    ElevatedCard(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Text(
                text = exercise.exercise.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = exercise.exercise.category,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Nagłówek tabeli
            SetTableHeader()

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // Wiersze serii
            exerciseState.sets.forEachIndexed { setIdx, setInput ->
                SetRow(
                    setNumber = setIdx + 1,
                    setInput = setInput,
                    onRepsChanged = { value ->
                        onEvent(ActiveWorkoutEvent.RepsChanged(exIdx, setIdx, value))
                    },
                    onWeightChanged = { value ->
                        onEvent(ActiveWorkoutEvent.WeightChanged(exIdx, setIdx, value))
                    },
                    onComplete = {
                        onEvent(ActiveWorkoutEvent.SetCompleted(exIdx, setIdx))
                    }
                )
            }
        }
    }
}

// ─── Set Table Header ────────────────────────────────────────────────────────

@Composable
private fun SetTableHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.sets),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(40.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.reps_label),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.weight_label),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        // Placeholder dla przycisku
        Spacer(modifier = Modifier.size(40.dp))
    }
}

// ─── Set Row ─────────────────────────────────────────────────────────────────

@Composable
private fun SetRow(
    setNumber: Int,
    setInput: SetInputState,
    onRepsChanged: (String) -> Unit,
    onWeightChanged: (String) -> Unit,
    onComplete: () -> Unit
) {
    val completedBackground = if (setInput.isCompleted)
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
    else
        MaterialTheme.colorScheme.surface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(completedBackground)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Numer serii
        Text(
            text = "$setNumber",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(40.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))

        // Pole powtórzeń
        OutlinedTextField(
            value = setInput.actualReps,
            onValueChange = onRepsChanged,
            modifier = Modifier.weight(1f),
            enabled = !setInput.isCompleted,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            label = { Text(stringResource(R.string.reps_label)) },
            textStyle = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.width(8.dp))

        // Pole ciężaru
        OutlinedTextField(
            value = setInput.actualWeight,
            onValueChange = onWeightChanged,
            modifier = Modifier.weight(1f),
            enabled = !setInput.isCompleted,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            label = { Text(stringResource(R.string.weight_label)) },
            textStyle = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.width(8.dp))

        // Przycisk ukończenia
        FilledIconButton(
            onClick = onComplete,
            enabled = !setInput.isCompleted,
            modifier = Modifier.size(40.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = if (setInput.isCompleted)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.primary,
                contentColor = if (setInput.isCompleted)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = stringResource(R.string.active_workout_set_completed),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ─── Finish Dialog ────────────────────────────────────────────────────────────

@Composable
private fun FinishWorkoutDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var notes by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.active_workout_finish)) },
        text = {
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.active_workout_notes_hint)) },
                minLines = 3,
                maxLines = 5
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(notes) }) {
                Text(stringResource(R.string.active_workout_finish_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

// ─── Extension ───────────────────────────────────────────────────────────────

private fun Int.toElapsedString(): String {
    val m = this / 60
    val s = this % 60
    return "%02d:%02d".format(m, s)
}

// ─── Preview ─────────────────────────────────────────────────────────────────

private val previewExercise = Exercise(
    id = "ex1",
    name = "Przysiad ze sztangą",
    category = "Siłowe",
    muscleGroups = listOf("Nogi", "Pośladki")
)

private val previewDayExercise = TrainingDayExercise(
    id = "tde1",
    exercise = previewExercise,
    sets = 4,
    reps = 10,
    weightKg = 80.0,
    durationSec = null,
    restSeconds = 90,
    exerciseOrder = 0,
    notes = null
)

private val previewUiState = ActiveWorkoutUiState(
    dayName = "Dzień A — Nogi",
    isLoading = false,
    elapsedSeconds = 754,
    exercises = listOf(
        ExerciseWorkoutState(
            dayExercise = previewDayExercise,
            sets = listOf(
                SetInputState(targetReps = 10, targetWeight = 80.0, isCompleted = true),
                SetInputState(targetReps = 10, targetWeight = 80.0, actualReps = "9"),
                SetInputState(targetReps = 10, targetWeight = 80.0),
                SetInputState(targetReps = 10, targetWeight = 80.0)
            )
        ),
        ExerciseWorkoutState(
            dayExercise = TrainingDayExercise(
                id = "tde2",
                exercise = Exercise("ex2", "Martwy ciąg", "Siłowe", listOf("Plecy")),
                sets = 3,
                reps = 6,
                weightKg = 120.0,
                durationSec = null,
                restSeconds = 180,
                exerciseOrder = 1,
                notes = null
            ),
            sets = listOf(
                SetInputState(targetReps = 6, targetWeight = 120.0),
                SetInputState(targetReps = 6, targetWeight = 120.0),
                SetInputState(targetReps = 6, targetWeight = 120.0)
            )
        )
    )
)

@Preview(showBackground = true, name = "Active Workout")
@Composable
private fun ActiveWorkoutPreview() {
    FitCoachTheme {
        ActiveWorkoutContent(
            uiState = previewUiState,
            onEvent = {},
            onBack = {}
        )
    }
}

@Preview(showBackground = true, name = "Active Workout — Loading")
@Composable
private fun ActiveWorkoutLoadingPreview() {
    FitCoachTheme {
        ActiveWorkoutContent(
            uiState = ActiveWorkoutUiState(isLoading = true),
            onEvent = {},
            onBack = {}
        )
    }
}

@Preview(showBackground = true, name = "Active Workout — Rest Timer")
@Composable
private fun ActiveWorkoutRestTimerPreview() {
    FitCoachTheme {
        ActiveWorkoutContent(
            uiState = previewUiState.copy(
                restTimer = RestTimerState(totalSeconds = 90, remainingSeconds = 67)
            ),
            onEvent = {},
            onBack = {}
        )
    }
}

@Preview(showBackground = true, name = "Active Workout — Finish Dialog")
@Composable
private fun ActiveWorkoutFinishDialogPreview() {
    FitCoachTheme {
        ActiveWorkoutContent(
            uiState = previewUiState.copy(showFinishDialog = true),
            onEvent = {},
            onBack = {}
        )
    }
}

@Preview(showBackground = true, name = "Active Workout — Screen wrapper (no VM)")
@Composable
private fun ActiveWorkoutScreenPreview() {
    FitCoachTheme {
        ActiveWorkoutContent(
            uiState = previewUiState,
            onEvent = {},
            onBack = {}
        )
    }
}
