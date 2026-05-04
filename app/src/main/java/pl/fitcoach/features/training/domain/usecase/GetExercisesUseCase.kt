package pl.fitcoach.features.training.domain.usecase

import pl.fitcoach.features.training.domain.model.Exercise
import pl.fitcoach.features.training.domain.repository.TrainingRepository
import javax.inject.Inject

class GetExercisesUseCase @Inject constructor(
    private val repository: TrainingRepository
) {
    suspend operator fun invoke(): Result<List<Exercise>> = repository.getExercises()
}
