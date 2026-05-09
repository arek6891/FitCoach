package pl.fitcoach.features.workout.domain.usecase

import pl.fitcoach.features.workout.domain.repository.WorkoutRepository
import javax.inject.Inject

class CompleteWorkoutSessionUseCase @Inject constructor(
    private val workoutRepository: WorkoutRepository
) {
    suspend operator fun invoke(
        sessionId: String,
        durationSec: Int,
        notes: String?
    ): Result<Unit> = workoutRepository.completeSession(
        sessionId = sessionId,
        durationSec = durationSec,
        notes = notes
    )
}
