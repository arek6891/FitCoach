package pl.fitcoach.features.clients.domain.repository

import pl.fitcoach.features.clients.domain.model.Client
import pl.fitcoach.features.clients.domain.model.InviteCode
import pl.fitcoach.features.clients.domain.model.TrainerProfile

interface ClientRepository {
    suspend fun getClients(): Result<List<Client>>
    suspend fun getClientById(clientId: String): Result<Client>
    suspend fun getTrainerProfile(): Result<TrainerProfile>
    suspend fun generateInviteCode(clientName: String?): Result<String>
    suspend fun getInviteCodes(): Result<List<InviteCode>>
    suspend fun cancelInviteCode(codeId: String): Result<Unit>
    suspend fun validateInviteCode(code: String): Result<InviteCode>
}
