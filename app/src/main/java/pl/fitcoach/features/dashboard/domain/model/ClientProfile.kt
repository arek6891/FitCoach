package pl.fitcoach.features.dashboard.domain.model

data class ClientProfile(
    val id: String,
    val firstName: String,
    val goal: String?,
    val avatarUrl: String?
)
