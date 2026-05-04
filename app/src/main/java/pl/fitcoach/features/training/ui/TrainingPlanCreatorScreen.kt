package pl.fitcoach.features.training.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
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
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.fitcoach.R
import pl.fitcoach.core.ui.theme.FitCoachTheme
import pl.fitcoach.features.training.ui.components.ExercisePickerBottomSheet

@Composable
fun TrainingPlanCreatorScreen(
    onBack: () -> Unit,
    onPlanSaved: () -> Unit,
    viewModel: TrainingPlanCreatorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    TrainingPlanCreatorContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        onPlanSaved = onPlanSaved
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrainingPlanCreatorContent(
    uiState: TrainingPlanCreatorUiState,
    onEvent: (TrainingPlanCreatorEvent) -> Unit,
    onBack: () -> Unit,
    onPlanSaved: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { message ->
            snackbarHostState.showSnackbar(message)
            onEvent(TrainingPlanCreatorEvent.ErrorDismissed)
        }
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onPlanSaved()
            onEvent(TrainingPlanCreatorEvent.SaveHandled)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.training_plan_new)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    if (uiState.isSaving) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .padding(12.dp)
                        ) {
                            CircularProgressIndicator(strokeWidth = 2.dp)
                        }
                    } else {
                        IconButton(
                            onClick = { onEvent(TrainingPlanCreatorEvent.SavePlan) },
                            enabled = uiState.planName.isNotBlank()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = stringResource(R.string.training_plan_save)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Nazwa planu
            OutlinedTextField(
                value = uiState.planName,
                onValueChange = { onEvent(TrainingPlanCreatorEvent.PlanNameChanged(it)) },
                label = { Text(stringResource(R.string.training_plan_name_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Opis planu
            OutlinedTextField(
                value = uiState.planDescription,
                onValueChange = { onEvent(TrainingPlanCreatorEvent.PlanDescriptionChanged(it)) },
                label = { Text(stringResource(R.string.training_plan_description_label)) },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )

            // Sekcja dni treningowych
            uiState.days.forEachIndexed { index, dayDraft ->
                key(dayDraft.localId) {
                    TrainingDayCard(
                        dayDraft = dayDraft,
                        dayNumber = index + 1,
                        onDayNameChanged = { name ->
                            onEvent(TrainingPlanCreatorEvent.DayNameChanged(dayDraft.localId, name))
                        },
                        onAddExercise = {
                            onEvent(TrainingPlanCreatorEvent.ShowExercisePicker(dayDraft.localId))
                        },
                        onRemoveExercise = { exerciseId ->
                            onEvent(TrainingPlanCreatorEvent.RemoveExercise(dayDraft.localId, exerciseId))
                        },
                        onRemoveDay = {
                            onEvent(TrainingPlanCreatorEvent.RemoveDay(dayDraft.localId))
                        }
                    )
                }
            }

            // Przycisk dodania dnia
            OutlinedButton(
                onClick = { onEvent(TrainingPlanCreatorEvent.AddDay) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.training_plan_add_day))
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Exercise Picker Bottom Sheet
        if (uiState.showExercisePicker) {
            ExercisePickerBottomSheet(
                exercises = uiState.availableExercises,
                onExerciseSelected = { exercise ->
                    onEvent(TrainingPlanCreatorEvent.ExerciseSelected(exercise))
                },
                onDismiss = { onEvent(TrainingPlanCreatorEvent.DismissExercisePicker) }
            )
        }
    }
}

@Composable
private fun TrainingDayCard(
    dayDraft: TrainingDayDraft,
    dayNumber: Int,
    onDayNameChanged: (String) -> Unit,
    onAddExercise: () -> Unit,
    onRemoveExercise: (String) -> Unit,
    onRemoveDay: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.FitnessCenter,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Dzień $dayNumber",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onRemoveDay) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.delete),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            OutlinedTextField(
                value = dayDraft.name,
                onValueChange = onDayNameChanged,
                label = { Text(stringResource(R.string.training_plan_day_name_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Lista ćwiczeń
            dayDraft.exercises.forEach { exerciseDraft ->
                key(exerciseDraft.exercise.id) {
                    ListItem(
                        headlineContent = { Text(exerciseDraft.exercise.name) },
                        supportingContent = {
                            Text(
                                text = buildExerciseSummary(exerciseDraft),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingContent = {
                            IconButton(onClick = { onRemoveExercise(exerciseDraft.exercise.id) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.delete),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    )
                }
            }

            TextButton(
                onClick = onAddExercise,
                modifier = Modifier.align(Alignment.Start)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.training_plan_add_exercise))
            }
        }
    }
}

private fun buildExerciseSummary(draft: ExerciseDraft): String {
    val parts = mutableListOf<String>()
    parts.add("${draft.sets} serii")
    draft.reps?.let { parts.add("$it powt.") }
    draft.weightKg?.let { parts.add("${it} kg") }
    draft.durationSec?.let { parts.add("${it}s") }
    return parts.joinToString(" · ")
}

// --- Previews ---

@Preview(showBackground = true, name = "Kreator planu — pusty")
@Composable
private fun TrainingPlanCreatorEmptyPreview() {
    FitCoachTheme {
        TrainingPlanCreatorContent(
            uiState = TrainingPlanCreatorUiState(),
            onEvent = {},
            onBack = {},
            onPlanSaved = {}
        )
    }
}

@Preview(showBackground = true, name = "Kreator planu — z dniem")
@Composable
private fun TrainingPlanCreatorWithDayPreview() {
    FitCoachTheme {
        TrainingPlanCreatorContent(
            uiState = TrainingPlanCreatorUiState(
                planName = "Plan siłowy A/B",
                planDescription = "Plan dla początkujących",
                days = listOf(
                    TrainingDayDraft(
                        name = "Dzień A — Góra ciała"
                    )
                )
            ),
            onEvent = {},
            onBack = {},
            onPlanSaved = {}
        )
    }
}
