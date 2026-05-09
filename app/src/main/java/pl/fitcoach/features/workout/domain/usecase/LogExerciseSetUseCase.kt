package pl.fitcoach.features.workout.domain.usecase

import pl.fitcoach.features.workout.domain.repository.WorkoutRepository
import javax.inject.Inject

class LogExerciseSetUseCase @Inject constructor(
    private val workoutRepository: WorkoutRepository
) {
    suspend operator fun invoke(
        sessionId: String,
        exerciseId: String,
        setNumber: Int,
        reps: Int?,
        weightKg: Double?,
        durationSec: Int?
    ): Result<Unit> = workoutRepository.logSet(
        sessionId = sessionId,
        exerciseId = exerciseId,
        setNumber = setNumber,
        reps = reps,
        weightKg = weightKg,
        durationSec = durationSec
    )
}
