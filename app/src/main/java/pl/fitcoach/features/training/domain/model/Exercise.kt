package pl.fitcoach.features.training.domain.model

data class Exercise(
    val id: String,
    val name: String,
    val category: String,
    val muscleGroups: List<String>,
    val description: String? = null,
    val isCustom: Boolean = false
)

data class TrainingDayExercise(
    val id: String,
    val exercise: Exercise,
    val sets: Int,
    val reps: Int?,
    val weightKg: Double?,
    val durationSec: Int?,
    val restSeconds: Int,
    val exerciseOrder: Int,
    val notes: String?
)

data class TrainingDay(
    val id: String,
    val name: String,
    val dayOrder: Int,
    val exercises: List<TrainingDayExercise>
)

data class TrainingPlan(
    val id: String,
    val name: String,
    val description: String?,
    val days: List<TrainingDay>
)
