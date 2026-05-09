package pl.fitcoach.features.workout.domain.usecase

import pl.fitcoach.features.training.domain.model.TrainingDay
import pl.fitcoach.features.workout.domain.repository.WorkoutRepository
import javax.inject.Inject

class GetTrainingDayUseCase @Inject constructor(
    private val workoutRepository: WorkoutRepository
) {
    suspend operator fun invoke(dayId: String): Result<TrainingDay> =
        workoutRepository.getTrainingDayWithExercises(dayId)
}
