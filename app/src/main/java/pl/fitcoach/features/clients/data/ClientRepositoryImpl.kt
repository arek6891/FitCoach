package pl.fitcoach.features.clients.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import pl.fitcoach.features.clients.data.dto.ClientDto
import pl.fitcoach.features.clients.data.dto.InviteCodeDto
import pl.fitcoach.features.clients.data.dto.PublicInviteCodeLookupDto
import pl.fitcoach.features.clients.data.dto.TrainerProfileDto
import pl.fitcoach.features.clients.data.dto.toDomain
import pl.fitcoach.features.clients.domain.model.Client
import pl.fitcoach.features.clients.domain.model.InviteCode
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

    override suspend fun getClientById(clientId: String): Result<Client> = runCatching {
        supabaseClient.postgrest["client_profiles"]
            .select {
                filter {
                    eq("user_id", clientId)
                }
            }
            .decodeSingle<ClientDto>()
            .toDomain()
    }

    override suspend fun getTrainerProfile(): Result<TrainerProfile> = runCatching {
        val userId = requireUserId()
        supabaseClient.postgrest["trainer_profiles"]
            .select {
                filter {
                    eq("user_id", userId)
                }
            }
            .decodeSingle<TrainerProfileDto>()
            .toDomain()
    }

    override suspend fun generateInviteCode(clientName: String?): Result<String> = runCatching {
        val userId = requireUserId()
        val params = buildJsonObject {
            put("p_trainer_id", userId)
            put("p_client_name", clientName)
        }
        supabaseClient.postgrest
            .rpc("generate_invite_code", params)
            .decodeAs<String>()
    }

    override suspend fun getInviteCodes(): Result<List<InviteCode>> = runCatching {
        val userId = requireUserId()
        supabaseClient.postgrest["invite_codes"]
            .select {
                filter {
                    eq("trainer_id", userId)
                }
                order("created_at", Order.DESCENDING)
            }
            .decodeList<InviteCodeDto>()
            .map { it.toDomain() }
    }

    override suspend fun cancelInviteCode(codeId: String): Result<Unit> = runCatching {
        supabaseClient.postgrest["invite_codes"]
            .update(
                buildJsonObject { put("status", "expired") }
            ) {
                filter {
                    eq("id", codeId)
                }
            }
    }

    override suspend fun validateInviteCode(code: String): Result<InviteCode> = runCatching {
        supabaseClient.postgrest["public_invite_code_lookup"]
            .select {
                filter {
                    eq("code", code.uppercase().trim())
                }
            }
            .decodeSingle<PublicInviteCodeLookupDto>()
            .toDomain()
    }

    // --- Helpers ---

    private fun requireUserId(): String =
        supabaseClient.auth.currentUserOrNull()?.id
            ?: error("Użytkownik nie jest zalogowany")

    // --- Mapowanie DTO → domain ---

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
