package pl.fitcoach.features.clients.data.dto

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import pl.fitcoach.features.clients.domain.model.InviteCode
import pl.fitcoach.features.clients.domain.model.InviteCodeStatus

@Serializable
data class InviteCodeDto(
    @SerialName("id") val id: String,
    @SerialName("code") val code: String,
    @SerialName("trainer_id") val trainerId: String,
    @SerialName("client_name") val clientName: String? = null,
    @SerialName("status") val status: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("expires_at") val expiresAt: String,
    @SerialName("used_at") val usedAt: String? = null
)

fun InviteCodeDto.toDomain(): InviteCode = InviteCode(
    id = id,
    code = code,
    clientName = clientName,
    status = InviteCodeStatus.fromString(status),
    createdAt = Instant.parse(createdAt),
    expiresAt = Instant.parse(expiresAt),
    usedAt = usedAt?.let { Instant.parse(it) }
)

// Widok public_invite_code_lookup zwraca tylko podzbiór kolumn (brak id, created_at, used_at)
@Serializable
data class PublicInviteCodeLookupDto(
    @SerialName("code") val code: String,
    @SerialName("status") val status: String,
    @SerialName("expires_at") val expiresAt: String,
    @SerialName("trainer_id") val trainerId: String,
    @SerialName("client_name") val clientName: String? = null,
)

fun PublicInviteCodeLookupDto.toDomain(): InviteCode = InviteCode(
    id = "",
    code = code,
    clientName = clientName,
    status = InviteCodeStatus.fromString(status),
    createdAt = Instant.parse(expiresAt), // created_at niedostępny w widoku — używamy expiresAt jako placeholder
    expiresAt = Instant.parse(expiresAt),
    usedAt = null
)
