package pl.fitcoach.features.clients.domain.usecase

import pl.fitcoach.features.clients.domain.model.InviteCode
import pl.fitcoach.features.clients.domain.repository.ClientRepository
import javax.inject.Inject

class GetInviteCodesUseCase @Inject constructor(
    private val repository: ClientRepository
) {
    suspend operator fun invoke(): Result<List<InviteCode>> =
        repository.getInviteCodes()
}
