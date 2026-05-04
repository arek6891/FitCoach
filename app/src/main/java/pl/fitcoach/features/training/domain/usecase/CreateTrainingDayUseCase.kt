package pl.fitcoach.features.training.domain.usecase

import pl.fitcoach.features.training.domain.model.TrainingDay
import pl.fitcoach.features.training.domain.repository.TrainingRepository
import javax.inject.Inject

class CreateTrainingDayUseCase @Inject constructor(
    private val repository: TrainingRepository
) {
    suspend operator fun invoke(
        planId: String,
        name: String,
        dayOrder: Int
    ): Result<TrainingDay> = repository.createTrainingDay(planId, name, dayOrder)
}
