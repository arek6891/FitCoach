package pl.fitcoach.features.clients.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import pl.fitcoach.features.clients.data.dto.ClientDto
import pl.fitcoach.features.clients.data.dto.TrainerProfileDto
import pl.fitcoach.features.clients.domain.model.Client
import pl.fitcoach.features.clients.domain.model.TrainerProfile
import pl.fitcoach.features.clients.domain.repository.ClientRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClientRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient
) : ClientRepository {

    override suspend fun getClients(): Result<List<Client>> = runCatching {
        supabaseClient.postgrest["client_profiles"]
            .select()
            .decodeList<ClientDto>()
            .map { it.toDomain() }
    }

    override suspend fun getTrainerProfile(): Result<TrainerProfile> = runCatching {
        val userId = supabaseClient.auth.currentUserOrNull()?.id
            ?: error("Użytkownik nie jest zalogowany")
        supabaseClient.postgrest["trainer_profiles"]
            .select {
                filter {
                    eq("user_id", userId)
                }
            }
            .decodeSingle<TrainerProfileDto>()
            .toDomain()
    }

    private fun ClientDto.toDomain() = Client(
        id = id,
        userId = userId,
        firstName = firstName,
        lastName = lastName,
        goal = goal,
        avatarUrl = avatarUrl,
        isActive = isActive,
        inviteCode = inviteCode,
        createdAt = createdAt
    )

    private fun TrainerProfileDto.toDomain() = TrainerProfile(
        id = id,
        firstName = firstName,
        lastName = lastName,
        avatarUrl = avatarUrl
    )
}
