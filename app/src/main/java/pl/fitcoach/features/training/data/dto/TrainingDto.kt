package pl.fitcoach.features.training.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import pl.fitcoach.features.training.domain.model.Exercise
import pl.fitcoach.features.training.domain.model.TrainingDay
import pl.fitcoach.features.training.domain.model.TrainingDayExercise
import pl.fitcoach.features.training.domain.model.TrainingPlan

// --- Odpowiedzi z bazy ---

@Serializable
data class TrainingExerciseDto(
    val id: String,
    val name: String,
    val category: String,
    @SerialName("muscle_groups") val muscleGroups: List<String> = emptyList(),
    val description: String? = null,
    @SerialName("is_custom") val isCustom: Boolean = false
) {
    fun toDomain() = Exercise(
        id = id,
        name = name,
        category = category,
        muscleGroups = muscleGroups,
        description = description,
        isCustom = isCustom
    )
}

@Serializable
data class TrainingDayExerciseDtoT(
    val id: String,
    @SerialName("day_id") val dayId: String? = null,
    @SerialName("exercise_id") val exerciseId: String,
    val sets: Int,
    val reps: Int? = null,
    @SerialName("weight_kg") val weightKg: Double? = null,
    @SerialName("duration_sec") val durationSec: Int? = null,
    @SerialName("rest_seconds") val restSeconds: Int = 90,
    @SerialName("exercise_order") val exerciseOrder: Int,
    val notes: String? = null,
    val exercises: TrainingExerciseDto? = null
) {
    fun toDomain(): TrainingDayExercise {
        val exercise = exercises?.toDomain() ?: Exercise(
            id = exerciseId,
            name = "",
            category = "",
            muscleGroups = emptyList()
        )
        return TrainingDayExercise(
            id = id,
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
}

@Serializable
data class TrainingDayDtoT(
    val id: String,
    @SerialName("plan_id") val planId: String? = null,
    val name: String,
    @SerialName("day_order") val dayOrder: Int,
    @SerialName("training_day_exercises") val exercises: List<TrainingDayExerciseDtoT> = emptyList()
) {
    fun toDomain() = TrainingDay(
        id = id,
        name = name,
        dayOrder = dayOrder,
        exercises = exercises.sortedBy { it.exerciseOrder }.map { it.toDomain() }
    )
}

@Serializable
data class TrainingPlanDtoT(
    val id: String,
    @SerialName("trainer_id") val trainerId: String? = null,
    @SerialName("client_id") val clientId: String? = null,
    val name: String,
    val description: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("training_days") val days: List<TrainingDayDtoT> = emptyList()
) {
    fun toDomain() = TrainingPlan(
        id = id,
        name = name,
        description = description,
        days = days.sortedBy { it.dayOrder }.map { it.toDomain() }
    )
}

// --- Zapytania insert ---

@Serializable
data class CreateTrainingPlanRequest(
    val name: String,
    val description: String? = null,
    @SerialName("client_id") val clientId: String,
    @SerialName("trainer_id") val trainerId: String,
    @SerialName("is_active") val isActive: Boolean = true
)

@Serializable
data class CreateTrainingDayRequest(
    val name: String,
    @SerialName("day_order") val dayOrder: Int,
    @SerialName("plan_id") val planId: String
)

@Serializable
data class CreateTrainingDayExerciseRequest(
    @SerialName("day_id") val dayId: String,
    @SerialName("exercise_id") val exerciseId: String,
    val sets: Int,
    val reps: Int? = null,
    @SerialName("weight_kg") val weightKg: Double? = null,
    @SerialName("duration_sec") val durationSec: Int? = null,
    @SerialName("rest_seconds") val restSeconds: Int = 90,
    @SerialName("exercise_order") val exerciseOrder: Int,
    val notes: String? = null
)

// --- Helper DTO dla zapytania o trainer profile id ---

@Serializable
data class TrainerProfileIdDto(val id: String)
