package pl.fitcoach.features.clients.domain.usecase

import pl.fitcoach.features.clients.domain.model.Client
import pl.fitcoach.features.clients.domain.repository.ClientRepository
import javax.inject.Inject

class GetClientByIdUseCase @Inject constructor(
    private val repository: ClientRepository
) {
    suspend operator fun invoke(clientId: String): Result<Client> =
        repository.getClientById(clientId)
}
