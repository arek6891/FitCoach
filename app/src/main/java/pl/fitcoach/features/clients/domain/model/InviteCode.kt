package pl.fitcoach.features.clients.domain.model

import kotlinx.datetime.Instant

data class InviteCode(
    val id: String,
    val code: String,
    val clientName: String?,
    val status: InviteCodeStatus,
    val createdAt: Instant,
    val expiresAt: Instant,
    val usedAt: Instant?
)

enum class InviteCodeStatus {
    PENDING, USED, EXPIRED, UNKNOWN;

    companion object {
        fun fromString(value: String): InviteCodeStatus = when (value) {
            "pending" -> PENDING
            "used" -> USED
            "expired" -> EXPIRED
            else -> UNKNOWN
        }
    }
}
