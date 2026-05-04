package pl.fitcoach.features.training.domain.usecase

import pl.fitcoach.features.training.domain.model.TrainingPlan
import pl.fitcoach.features.training.domain.repository.TrainingRepository
import javax.inject.Inject

class CreateTrainingPlanUseCase @Inject constructor(
    private val repository: TrainingRepository
) {
    suspend operator fun invoke(
        clientId: String,
        name: String,
        description: String?
    ): Result<TrainingPlan> = repository.createTrainingPlan(clientId, name, description)
}
