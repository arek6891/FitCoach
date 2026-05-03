package pl.fitcoach.features.clients.domain.model

data class TrainerProfile(
    val id: String,
    val firstName: String,
    val lastName: String,
    val avatarUrl: String?
) {
    val fullName: String get() = "$firstName $lastName"
}
