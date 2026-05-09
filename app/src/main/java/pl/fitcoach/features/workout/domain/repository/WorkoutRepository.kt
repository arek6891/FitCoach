package pl.fitcoach.features.workout.domain.repository

import pl.fitcoach.features.training.domain.model.TrainingDay

interface WorkoutRepository {
    suspend fun startSession(planId: String, dayId: String): Result<String>
    suspend fun logSet(
        sessionId: String,
        exerciseId: String,
        setNumber: Int,
        reps: Int?,
        weightKg: Double?,
        durationSec: Int?
    ): Result<Unit>
    suspend fun completeSession(sessionId: String, durationSec: Int, notes: String?): Result<Unit>
    suspend fun getTrainingDayWithExercises(dayId: String): Result<TrainingDay>
}
