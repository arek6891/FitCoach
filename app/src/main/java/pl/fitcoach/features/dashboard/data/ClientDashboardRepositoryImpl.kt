package pl.fitcoach.features.dashboard.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import pl.fitcoach.features.dashboard.data.dto.ClientProfileDto
import pl.fitcoach.features.dashboard.data.dto.HabitDto
import pl.fitcoach.features.dashboard.data.dto.HabitLogDto
import pl.fitcoach.features.dashboard.data.dto.TrainingDayDto
import pl.fitcoach.features.dashboard.data.dto.TrainingDayExerciseDto
import pl.fitcoach.features.dashboard.data.dto.TrainingPlanDto
import pl.fitcoach.features.dashboard.domain.model.ClientProfile
import pl.fitcoach.features.dashboard.domain.repository.ClientDashboardRepository
import pl.fitcoach.features.habits.domain.model.Habit
import pl.fitcoach.features.habits.domain.model.HabitType
import pl.fitcoach.features.training.domain.model.Exercise
import pl.fitcoach.features.training.domain.model.TrainingDay
import pl.fitcoach.features.training.domain.model.TrainingDayExercise
import pl.fitcoach.features.training.domain.model.TrainingPlan
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClientDashboardRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient
) : ClientDashboardRepository {

    override suspend fun getClientProfile(): Result<ClientProfile> = runCatching {
        val userId = requireUserId()
        supabaseClient.postgrest["client_profiles"]
            .select {
                filter { eq("user_id", userId) }
            }
            .decodeSingle<ClientProfileDto>()
            .toDomain()
    }

    override suspend fun getActivePlan(): Result<TrainingPlan?> = runCatching {
        val userId = requireUserId()
        val clientId = getClientId(userId)

        val plans = supabaseClient.postgrest["training_plans"]
            .select(
                columns = Columns.raw(
                    "id, name, description, " +
                        "training_days(id, name, day_order, " +
                        "training_day_exercises(id, exercise_id, sets, reps, weight_kg, " +
                        "duration_sec, rest_seconds, exercise_order, notes, " +
                        "exercises(id, name, category, muscle_groups)))"
                )
            ) {
                filter {
                    eq("client_id", clientId)
                    eq("is_active", true)
                }
            }
            .decodeList<TrainingPlanDto>()

        plans.firstOrNull()?.toDomain()
    }

    override suspend fun getTodayHabits(): Result<List<Habit>> = runCatching {
        val userId = requireUserId()
        val clientId = getClientId(userId)
        val today = LocalDate.now().toString()

        val habits = supabaseClient.postgrest["habits"]
            .select {
                filter {
                    eq("client_id", clientId)
                    eq("is_active", true)
                }
            }
            .decodeList<HabitDto>()

        val habitIds = habits.map { it.id }
        val logs = if (habitIds.isEmpty()) {
            emptyList()
        } else {
            supabaseClient.postgrest["habit_logs"]
                .select {
                    filter {
                        isIn("habit_id", habitIds)
                        eq("date", today)
                    }
                }
                .decodeList<HabitLogDto>()
        }

        val logsByHabitId = logs.associateBy { it.habitId }

        habits.map { habit ->
            val log = logsByHabitId[habit.id]
            habit.toDomain(
                isCompleted = log?.completed ?: false,
                currentValue = log?.value
            )
        }
    }

    override suspend fun logHabit(
        habitId: String,
        completed: Boolean,
        value: Double?
    ): Result<Unit> = runCatching {
        val userId = requireUserId()
        val clientId = getClientId(userId)
        val today = LocalDate.now().toString()

        val logDto = HabitLogDto(
            habitId = habitId,
            clientId = clientId,
            date = today,
            completed = completed,
            value = value
        )

        supabaseClient.postgrest["habit_logs"]
            .upsert(logDto, onConflict = "habit_id,date")
    }

    // --- Helpers ---

    private fun requireUserId(): String =
        supabaseClient.auth.currentUserOrNull()?.id
            ?: error("Użytkownik nie jest zalogowany")

    private suspend fun getClientId(userId: String): String =
        supabaseClient.postgrest["client_profiles"]
            .select(columns = Columns.raw("id")) {
                filter { eq("user_id", userId) }
            }
            .decodeSingle<ClientProfileDto>()
            .id

    // --- Mapowanie DTO → domain ---

    private fun ClientProfileDto.toDomain() = ClientProfile(
        id = id,
        firstName = firstName,
        goal = goal,
        avatarUrl = avatarUrl
    )

    private fun TrainingPlanDto.toDomain() = TrainingPlan(
        id = id,
        name = name,
        description = description,
        days = days.sortedBy { it.dayOrder }.map { it.toDomain() }
    )

    private fun TrainingDayDto.toDomain() = TrainingDay(
        id = id,
        name = name,
        dayOrder = dayOrder,
        exercises = exercises.sortedBy { it.exerciseOrder }.map { it.toDomain() }
    )

    private fun TrainingDayExerciseDto.toDomain() = TrainingDayExercise(
        id = id,
        exercise = Exercise(
            id = exercise.id,
            name = exercise.name,
            category = exercise.category,
            muscleGroups = exercise.muscleGroups
        ),
        sets = sets,
        reps = reps,
        weightKg = weightKg,
        durationSec = durationSec,
        restSeconds = restSeconds,
        exerciseOrder = exerciseOrder,
        notes = notes
    )

    private fun HabitDto.toDomain(isCompleted: Boolean, currentValue: Double?) = Habit(
        id = id,
        name = name,
        description = description,
        type = when (type) {
            "quantity" -> HabitType.QUANTITY
            else -> HabitType.BOOLEAN
        },
        targetValue = targetValue,
        unit = unit,
        isCompleted = isCompleted,
        currentValue = currentValue
    )
}
