package pl.fitcoach.features.training.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import pl.fitcoach.features.training.data.dto.CreateTrainingDayExerciseRequest
import pl.fitcoach.features.training.data.dto.CreateTrainingDayRequest
import pl.fitcoach.features.training.data.dto.CreateTrainingPlanRequest
import pl.fitcoach.features.training.data.dto.TrainerProfileIdDto
import pl.fitcoach.features.training.data.dto.TrainingDayDtoT
import pl.fitcoach.features.training.data.dto.TrainingExerciseDto
import pl.fitcoach.features.training.data.dto.TrainingPlanDtoT
import pl.fitcoach.features.training.domain.model.Exercise
import pl.fitcoach.features.training.domain.model.TrainingDay
import pl.fitcoach.features.training.domain.model.TrainingDayExercise
import pl.fitcoach.features.training.domain.model.TrainingPlan
import pl.fitcoach.features.training.domain.repository.TrainingRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrainingRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient
) : TrainingRepository {

    override suspend fun getExercises(): Result<List<Exercise>> = runCatching {
        supabaseClient.postgrest["exercises"]
            .select()
            .decodeList<TrainingExerciseDto>()
            .map { it.toDomain() }
    }

    override suspend fun getTrainingPlans(clientId: String): Result<List<TrainingPlan>> =
        runCatching {
            supabaseClient.postgrest["training_plans"]
                .select(
                    columns = Columns.raw(
                        "id, name, description, is_active, client_id, trainer_id, " +
                            "training_days(id, name, day_order, plan_id, " +
                            "training_day_exercises(id, exercise_id, sets, reps, weight_kg, " +
                            "duration_sec, rest_seconds, exercise_order, notes, day_id, " +
                            "exercises(id, name, category, muscle_groups, description, is_custom)))"
                    )
                ) {
                    filter { eq("client_id", clientId) }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<TrainingPlanDtoT>()
                .map { it.toDomain() }
        }

    override suspend fun createTrainingPlan(
        clientId: String,
        name: String,
        description: String?
    ): Result<TrainingPlan> = runCatching {
        val trainerId = requireTrainerId()
        supabaseClient.postgrest["training_plans"]
            .insert(
                CreateTrainingPlanRequest(
                    name = name,
                    description = description,
                    clientId = clientId,
                    trainerId = trainerId
                )
            ) {
                select()
            }
            .decodeSingle<TrainingPlanDtoT>()
            .toDomain()
    }

    override suspend fun createTrainingDay(
        planId: String,
        name: String,
        dayOrder: Int
    ): Result<TrainingDay> = runCatching {
        supabaseClient.postgrest["training_days"]
            .insert(
                CreateTrainingDayRequest(
                    name = name,
                    dayOrder = dayOrder,
                    planId = planId
                )
            ) {
                select()
            }
            .decodeSingle<TrainingDayDtoT>()
            .toDomain()
    }

    override suspend fun addExerciseToDay(
        dayId: String,
        exercise: Exercise,
        sets: Int,
        reps: Int?,
        weightKg: Double?,
        durationSec: Int?,
        restSeconds: Int,
        exerciseOrder: Int,
        notes: String?
    ): Result<TrainingDayExercise> = runCatching {
        supabaseClient.postgrest["training_day_exercises"]
            .insert(
                CreateTrainingDayExerciseRequest(
                    dayId = dayId,
                    exerciseId = exercise.id,
                    sets = sets,
                    reps = reps,
                    weightKg = weightKg,
                    durationSec = durationSec,
                    restSeconds = restSeconds,
                    exerciseOrder = exerciseOrder,
                    notes = notes
                )
            ) {
                select(columns = Columns.raw("id, day_id, exercise_id, sets, reps, weight_kg, duration_sec, rest_seconds, exercise_order, notes"))
            }

        // Po insercie budujemy TrainingDayExercise z obiektu Exercise który już mamy w pamięci
        TrainingDayExercise(
            id = UUID.randomUUID().toString(),
            exercise = exercise,
            sets = sets,
            reps = reps,
            weightKg = weightKg,
            durationSec = durationSec,
            restSeconds = restSeconds,
            exerciseOrder = exerciseOrder,
            notes = notes
        )
    }

    // --- Helpers ---

    private suspend fun requireTrainerId(): String {
        val userId = supabaseClient.auth.currentUserOrNull()?.id
            ?: error("Użytkownik nie jest zalogowany")
        return supabaseClient.postgrest["trainer_profiles"]
            .select(columns = Columns.raw("id")) {
                filter { eq("user_id", userId) }
            }
            .decodeSingle<TrainerProfileIdDto>()
            .id
    }
}
