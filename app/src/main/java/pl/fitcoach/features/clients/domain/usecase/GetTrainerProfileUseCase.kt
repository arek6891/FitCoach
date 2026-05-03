package pl.fitcoach.features.clients.domain.usecase

import pl.fitcoach.features.clients.domain.model.TrainerProfile
import pl.fitcoach.features.clients.domain.repository.ClientRepository
import javax.inject.Inject

class GetTrainerProfileUseCase @Inject constructor(
    private val repository: ClientRepository
) {
    suspend operator fun invoke(): Result<TrainerProfile> = repository.getTrainerProfile()
}
