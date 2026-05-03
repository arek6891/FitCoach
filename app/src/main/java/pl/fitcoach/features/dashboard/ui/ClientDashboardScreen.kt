package pl.fitcoach.features.dashboard.ui

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.SportsGymnastics
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import pl.fitcoach.R
import pl.fitcoach.core.ui.theme.FitCoachTheme
import pl.fitcoach.features.habits.domain.model.Habit
import pl.fitcoach.features.habits.domain.model.HabitType
import pl.fitcoach.features.training.domain.model.Exercise
import pl.fitcoach.features.training.domain.model.TrainingDay
import pl.fitcoach.features.training.domain.model.TrainingDayExercise
import pl.fitcoach.features.training.domain.model.TrainingPlan
import pl.fitcoach.navigation.Screen

@Composable
fun ClientDashboardScreen(
    navController: NavController,
    viewModel: ClientDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isLoggedOut) {
        if (uiState.isLoggedOut) {
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
            }
            viewModel.onEvent(ClientDashboardEvent.LogoutHandled)
        }
    }

    ClientDashboardContent(
        uiState = uiState,
        onEvent = viewModel::onEvent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClientDashboardContent(
    uiState: ClientDashboardUiState,
    onEvent: (ClientDashboardEvent) -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { message ->
            snackbarHostState.showSnackbar(message)
            onEvent(ClientDashboardEvent.ErrorDismissed)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.client_dashboard_title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                actions = {
                    IconButton(onClick = { onEvent(ClientDashboardEvent.LogoutClicked) }) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = stringResource(R.string.logout)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
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
                ClientDashboardBody(uiState = uiState, onEvent = onEvent)
            }
        }
    }
}

@Composable
private fun ClientDashboardBody(
    uiState: ClientDashboardUiState,
    onEvent: (ClientDashboardEvent) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Sekcja powitalna
        item {
            WelcomeSection(
                clientName = uiState.clientName,
                goal = uiState.goal,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
        }

        // 2. Karta dzisiejszego treningu
        item {
            TodayTrainingCard(
                plan = uiState.activePlan,
                todayDay = uiState.todayTrainingDay,
                onStartWorkout = { /* TODO: nawigacja do ActiveWorkout */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
        }

        // 3. Sekcja nawyków — nagłówek
        if (uiState.habits.isNotEmpty()) {
            item {
                HabitsSectionHeader(
                    completed = uiState.completedHabitsCount,
                    total = uiState.habits.size,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }

            items(
                items = uiState.habits,
                key = { habit -> habit.id }
            ) { habit ->
                HabitItem(
                    habit = habit,
                    onToggle = { completed ->
                        onEvent(ClientDashboardEvent.HabitToggled(habit.id, completed))
                    },
                    onValueChange = { value ->
                        onEvent(ClientDashboardEvent.HabitValueChanged(habit.id, value))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

// ─── Sekcja powitalna ────────────────────────────────────────────────────────

@Composable
private fun WelcomeSection(
    clientName: String,
    goal: String?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        if (clientName.isNotBlank()) {
            Text(
                text = stringResource(R.string.hello_client, clientName),
                style = MaterialTheme.typography.headlineSmall
            )
        }
        if (!goal.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = goal,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ─── Karta treningu ──────────────────────────────────────────────────────────

@Composable
private fun TodayTrainingCard(
    plan: TrainingPlan?,
    todayDay: TrainingDay?,
    onStartWorkout: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.FitnessCenter,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.todays_training),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            when {
                plan == null -> NoPlanState()
                todayDay == null -> RestDayState()
                else -> TrainingDayContent(
                    day = todayDay,
                    onStartWorkout = onStartWorkout
                )
            }
        }
    }
}

@Composable
private fun NoPlanState() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.SportsGymnastics,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.no_plan_title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.no_plan_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun RestDayState() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.EventBusy,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.rest_day_title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.rest_day_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun TrainingDayContent(
    day: TrainingDay,
    onStartWorkout: () -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.CalendarToday,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = day.name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        day.exercises.forEach { exercise ->
            key(exercise.id) {
                ExerciseRow(exercise = exercise)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onStartWorkout,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.start_workout))
        }
    }
}

@Composable
private fun ExerciseRow(exercise: TrainingDayExercise) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = exercise.exercise.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val detail = buildExerciseDetail(exercise)
            if (detail.isNotBlank()) {
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun buildExerciseDetail(exercise: TrainingDayExercise): String {
    val setsReps = if (exercise.reps != null) {
        stringResource(R.string.exercise_sets_reps, exercise.sets, exercise.reps)
    } else {
        stringResource(R.string.exercise_sets_only, exercise.sets)
    }
    val weight = exercise.weightKg?.let { kg ->
        "  ·  " + stringResource(R.string.exercise_weight_kg, kg)
    } ?: ""
    return setsReps + weight
}

// ─── Nawyki ───────────────────────────────────────────────────────────────────

@Composable
private fun HabitsSectionHeader(
    completed: Int,
    total: Int,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.todays_habits),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(R.string.habits_progress, completed, total),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { if (total > 0) completed.toFloat() / total else 0f },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun HabitItem(
    habit: Habit,
    onToggle: (Boolean) -> Unit,
    onValueChange: (Double?) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        when (habit.type) {
            HabitType.BOOLEAN -> BooleanHabitRow(
                habit = habit,
                onToggle = onToggle,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )
            HabitType.QUANTITY -> QuantityHabitRow(
                habit = habit,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            )
        }
    }
}

@Composable
private fun BooleanHabitRow(
    habit: Habit,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = habit.isCompleted,
            onCheckedChange = onToggle
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = habit.name,
                style = MaterialTheme.typography.bodyMedium
            )
            if (!habit.description.isNullOrBlank()) {
                Text(
                    text = habit.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun QuantityHabitRow(
    habit: Habit,
    onValueChange: (Double?) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    var textValue by rememberSaveable(habit.id) {
        mutableStateOf(habit.currentValue?.toString() ?: "")
    }

    Column(modifier = modifier) {
        Text(
            text = habit.name,
            style = MaterialTheme.typography.bodyMedium
        )
        if (!habit.description.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = habit.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = textValue,
                onValueChange = { input ->
                    textValue = input
                    onValueChange(input.toDoubleOrNull())
                },
                modifier = Modifier.width(120.dp),
                label = { Text(stringResource(R.string.quantity_habit_value_label)) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                ),
                singleLine = true
            )
            if (!habit.unit.isNullOrBlank()) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = habit.unit,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (habit.targetValue != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "/ ${habit.targetValue.toInt()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ─── Preview ─────────────────────────────────────────────────────────────────

private val previewExercise = Exercise(
    id = "ex1",
    name = "Przysiad ze sztangą",
    category = "Siłowe",
    muscleGroups = listOf("Nogi", "Pośladki")
)

private val previewDayExercises = listOf(
    TrainingDayExercise(
        id = "tde1",
        exercise = previewExercise,
        sets = 4,
        reps = 10,
        weightKg = 80.0,
        durationSec = null,
        restSeconds = 90,
        exerciseOrder = 0,
        notes = null
    ),
    TrainingDayExercise(
        id = "tde2",
        exercise = Exercise("ex2", "Wykroki z hantlami", "Siłowe", listOf("Nogi")),
        sets = 3,
        reps = 12,
        weightKg = 20.0,
        durationSec = null,
        restSeconds = 60,
        exerciseOrder = 1,
        notes = null
    )
)

private val previewTrainingDay = TrainingDay(
    id = "day1",
    name = "Nogi i pośladki",
    dayOrder = 1,
    exercises = previewDayExercises
)

private val previewPlan = TrainingPlan(
    id = "plan1",
    name = "Plan siłowy A/B",
    description = null,
    days = listOf(previewTrainingDay)
)

private val previewHabits = listOf(
    Habit(
        id = "h1",
        name = "Wypij 2 litry wody",
        description = null,
        type = HabitType.QUANTITY,
        targetValue = 2.0,
        unit = "l",
        isCompleted = false,
        currentValue = 1.2
    ),
    Habit(
        id = "h2",
        name = "Spacer 30 minut",
        description = "Wyjdź na świeże powietrze",
        type = HabitType.BOOLEAN,
        targetValue = null,
        unit = null,
        isCompleted = true,
        currentValue = null
    ),
    Habit(
        id = "h3",
        name = "Medytacja",
        description = null,
        type = HabitType.BOOLEAN,
        targetValue = null,
        unit = null,
        isCompleted = false,
        currentValue = null
    )
)

@Preview(showBackground = true, name = "Loading")
@Composable
private fun ClientDashboardLoadingPreview() {
    FitCoachTheme {
        ClientDashboardContent(
            uiState = ClientDashboardUiState(isLoading = true),
            onEvent = {}
        )
    }
}

@Preview(showBackground = true, name = "No plan")
@Composable
private fun ClientDashboardNoPlanPreview() {
    FitCoachTheme {
        ClientDashboardContent(
            uiState = ClientDashboardUiState(
                isLoading = false,
                clientName = "Anna",
                goal = "Redukcja wagi o 10 kg",
                activePlan = null,
                habits = previewHabits
            ),
            onEvent = {}
        )
    }
}

@Preview(showBackground = true, name = "Rest day")
@Composable
private fun ClientDashboardRestDayPreview() {
    FitCoachTheme {
        ClientDashboardContent(
            uiState = ClientDashboardUiState(
                isLoading = false,
                clientName = "Anna",
                goal = "Redukcja wagi o 10 kg",
                activePlan = TrainingPlan(
                    id = "plan1",
                    name = "Plan A/B",
                    description = null,
                    days = emptyList() // brak dnia dla dzisiaj
                ),
                habits = previewHabits
            ),
            onEvent = {}
        )
    }
}

@Preview(showBackground = true, name = "With training")
@Composable
private fun ClientDashboardWithTrainingPreview() {
    FitCoachTheme {
        ClientDashboardContent(
            uiState = ClientDashboardUiState(
                isLoading = false,
                clientName = "Anna",
                goal = "Redukcja wagi o 10 kg",
                activePlan = previewPlan,
                habits = previewHabits
            ),
            onEvent = {}
        )
    }
}
