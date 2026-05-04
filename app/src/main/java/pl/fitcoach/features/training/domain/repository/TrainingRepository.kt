package pl.fitcoach.features.training.domain.repository

import pl.fitcoach.features.training.domain.model.Exercise
import pl.fitcoach.features.training.domain.model.TrainingDay
import pl.fitcoach.features.training.domain.model.TrainingDayExercise
import pl.fitcoach.features.training.domain.model.TrainingPlan

interface TrainingRepository {
    suspend fun getExercises(): Result<List<Exercise>>
    suspend fun getTrainingPlans(clientId: String): Result<List<TrainingPlan>>
    suspend fun createTrainingPlan(
        clientId: String,
        name: String,
        description: String?
    ): Result<TrainingPlan>

    suspend fun createTrainingDay(
        planId: String,
        name: String,
        dayOrder: Int
    ): Result<TrainingDay>

    suspend fun addExerciseToDay(
        dayId: String,
        exercise: Exercise,
        sets: Int,
        reps: Int?,
        weightKg: Double?,
        durationSec: Int?,
        restSeconds: Int,
        exerciseOrder: Int,
        notes: String?
    ): Result<TrainingDayExercise>
}
