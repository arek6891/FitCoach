package pl.fitcoach.features.dashboard.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ClientProfileDto(
    @SerialName("id") val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("first_name") val firstName: String,
    @SerialName("goal") val goal: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null
)

@Serializable
data class ExerciseDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("category") val category: String,
    @SerialName("muscle_groups") val muscleGroups: List<String> = emptyList()
)

@Serializable
data class TrainingDayExerciseDto(
    @SerialName("id") val id: String,
    @SerialName("exercise_id") val exerciseId: String,
    @SerialName("sets") val sets: Int,
    @SerialName("reps") val reps: Int? = null,
    @SerialName("weight_kg") val weightKg: Double? = null,
    @SerialName("duration_sec") val durationSec: Int? = null,
    @SerialName("rest_seconds") val restSeconds: Int = 90,
    @SerialName("exercise_order") val exerciseOrder: Int,
    @SerialName("notes") val notes: String? = null,
    @SerialName("exercises") val exercise: ExerciseDto
)

@Serializable
data class TrainingDayDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("day_order") val dayOrder: Int,
    @SerialName("training_day_exercises") val exercises: List<TrainingDayExerciseDto> = emptyList()
)

@Serializable
data class TrainingPlanDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("description") val description: String? = null,
    @SerialName("training_days") val days: List<TrainingDayDto> = emptyList()
)

@Serializable
data class HabitDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("description") val description: String? = null,
    @SerialName("type") val type: String,
    @SerialName("target_value") val targetValue: Double? = null,
    @SerialName("unit") val unit: String? = null,
    @SerialName("is_active") val isActive: Boolean = true
)

@Serializable
data class HabitLogDto(
    @SerialName("id") val id: String? = null,
    @SerialName("habit_id") val habitId: String,
    @SerialName("client_id") val clientId: String,
    @SerialName("date") val date: String,
    @SerialName("completed") val completed: Boolean,
    @SerialName("value") val value: Double? = null
)
