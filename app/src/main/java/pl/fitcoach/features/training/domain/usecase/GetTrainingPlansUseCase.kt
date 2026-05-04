package pl.fitcoach.features.training.domain.usecase

import pl.fitcoach.features.training.domain.model.TrainingPlan
import pl.fitcoach.features.training.domain.repository.TrainingRepository
import javax.inject.Inject

class GetTrainingPlansUseCase @Inject constructor(
    private val repository: TrainingRepository
) {
    suspend operator fun invoke(clientId: String): Result<List<TrainingPlan>> =
        repository.getTrainingPlans(clientId)
}
