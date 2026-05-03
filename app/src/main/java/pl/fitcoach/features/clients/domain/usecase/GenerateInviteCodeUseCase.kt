package pl.fitcoach.features.clients.domain.usecase

import pl.fitcoach.features.clients.domain.repository.ClientRepository
import javax.inject.Inject

class GenerateInviteCodeUseCase @Inject constructor(
    private val repository: ClientRepository
) {
    suspend operator fun invoke(clientName: String?): Result<String> =
        repository.generateInviteCode(clientName)
}
