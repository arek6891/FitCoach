package pl.fitcoach.features.workout.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import pl.fitcoach.features.training.data.dto.TrainingDayDtoT
import pl.fitcoach.features.training.domain.model.TrainingDay
import pl.fitcoach.features.workout.data.dto.ClientProfileIdDto
import pl.fitcoach.features.workout.data.dto.CreateSessionSetRequest
import pl.fitcoach.features.workout.data.dto.CreateWorkoutSessionRequest
import pl.fitcoach.features.workout.data.dto.UpdateWorkoutSessionRequest
import pl.fitcoach.features.workout.data.dto.WorkoutSessionResponseDto
import pl.fitcoach.features.workout.domain.repository.WorkoutRepository
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient
) : WorkoutRepository {

    override suspend fun startSession(planId: String, dayId: String): Result<String> =
        runCatching {
            val clientId = getClientId()
            supabaseClient.postgrest["workout_sessions"]
                .insert(
                    CreateWorkoutSessionRequest(
                        planId = planId,
                        dayId = dayId,
                        clientId = clientId
                    )
                ) {
                    select()
                }
                .decodeSingle<WorkoutSessionResponseDto>()
                .id
        }

    override suspend fun logSet(
        sessionId: String,
        exerciseId: String,
        setNumber: Int,
        reps: Int?,
        weightKg: Double?,
        durationSec: Int?
    ): Result<Unit> = runCatching {
        supabaseClient.postgrest["session_sets"]
            .insert(
                CreateSessionSetRequest(
                    sessionId = sessionId,
                    exerciseId = exerciseId,
                    setNumber = setNumber,
                    reps = reps,
                    weightKg = weightKg,
                    durationSec = durationSec
                )
            )
        Unit
    }

    override suspend fun completeSession(
        sessionId: String,
        durationSec: Int,
        notes: String?
    ): Result<Unit> = runCatching {
        supabaseClient.postgrest["workout_sessions"]
            .update(
                UpdateWorkoutSessionRequest(
                    completedAt = Instant.now().toString(),
                    durationSec = durationSec,
                    clientNotes = notes
                )
            ) {
                filter { eq("id", sessionId) }
            }
        Unit
    }

    override suspend fun getTrainingDayWithExercises(dayId: String): Result<TrainingDay> =
        runCatching {
            supabaseClient.postgrest["training_days"]
                .select(
                    columns = Columns.raw(
                        "id, name, day_order, plan_id, " +
                            "training_day_exercises(id, exercise_id, sets, reps, weight_kg, " +
                            "duration_sec, rest_seconds, exercise_order, notes, day_id, " +
                            "exercises(id, name, category, muscle_groups, description, is_custom))"
                    )
                ) {
                    filter { eq("id", dayId) }
                }
                .decodeSingle<TrainingDayDtoT>()
                .toDomain()
        }

    // --- Helpers ---

    private suspend fun getClientId(): String {
        val userId = supabaseClient.auth.currentUserOrNull()?.id
            ?: error("Użytkownik nie jest zalogowany")
        return supabaseClient.postgrest["client_profiles"]
            .select(columns = Columns.raw("id")) {
                filter { eq("user_id", userId) }
            }
            .decodeSingle<ClientProfileIdDto>()
            .id
    }
}
