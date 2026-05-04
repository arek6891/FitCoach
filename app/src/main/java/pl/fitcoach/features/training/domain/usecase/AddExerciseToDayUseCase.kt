package pl.fitcoach.features.training.domain.usecase

import pl.fitcoach.features.training.domain.model.Exercise
import pl.fitcoach.features.training.domain.model.TrainingDayExercise
import pl.fitcoach.features.training.domain.repository.TrainingRepository
import javax.inject.Inject

class AddExerciseToDayUseCase @Inject constructor(
    private val repository: TrainingRepository
) {
    suspend operator fun invoke(
        dayId: String,
        exercise: Exercise,
        sets: Int,
        reps: Int?,
        weightKg: Double?,
        durationSec: Int?,
        restSeconds: Int,
        exerciseOrder: Int,
        notes: String?
    ): Result<TrainingDayExercise> = repository.addExerciseToDay(
        dayId = dayId,
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
