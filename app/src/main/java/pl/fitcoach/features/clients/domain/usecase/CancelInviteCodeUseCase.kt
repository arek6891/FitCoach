package pl.fitcoach.features.clients.domain.usecase

import pl.fitcoach.features.clients.domain.repository.ClientRepository
import javax.inject.Inject

class CancelInviteCodeUseCase @Inject constructor(
    private val repository: ClientRepository
) {
    suspend operator fun invoke(codeId: String): Result<Unit> =
        repository.cancelInviteCode(codeId)
}
