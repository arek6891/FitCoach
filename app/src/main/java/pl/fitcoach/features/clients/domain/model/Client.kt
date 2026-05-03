package pl.fitcoach.features.clients.domain.model

data class Client(
    val id: String,
    val userId: String,
    val firstName: String,
    val lastName: String,
    val goal: String?,
    val avatarUrl: String?,
    val isActive: Boolean,
    val inviteCode: String?,
    val createdAt: String
) {
    val fullName: String get() = "$firstName $lastName"
}
