package pl.fitcoach.features.workout.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateWorkoutSessionRequest(
    @SerialName("plan_id") val planId: String,
    @SerialName("day_id") val dayId: String,
    @SerialName("client_id") val clientId: String
)

@Serializable
data class WorkoutSessionResponseDto(val id: String)

@Serializable
data class CreateSessionSetRequest(
    @SerialName("session_id") val sessionId: String,
    @SerialName("exercise_id") val exerciseId: String,
    @SerialName("set_number") val setNumber: Int,
    val reps: Int? = null,
    @SerialName("weight_kg") val weightKg: Double? = null,
    @SerialName("duration_sec") val durationSec: Int? = null
)

@Serializable
data class UpdateWorkoutSessionRequest(
    @SerialName("completed_at") val completedAt: String,
    @SerialName("duration_sec") val durationSec: Int,
    @SerialName("client_notes") val clientNotes: String? = null
)

@Serializable
data class ClientProfileIdDto(val id: String)
