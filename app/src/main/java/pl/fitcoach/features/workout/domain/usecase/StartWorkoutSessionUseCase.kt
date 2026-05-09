package pl.fitcoach.features.workout.domain.usecase

import pl.fitcoach.features.workout.domain.repository.WorkoutRepository
import javax.inject.Inject

class StartWorkoutSessionUseCase @Inject constructor(
    private val workoutRepository: WorkoutRepository
) {
    suspend operator fun invoke(planId: String, dayId: String): Result<String> =
        workoutRepository.startSession(planId, dayId)
}
