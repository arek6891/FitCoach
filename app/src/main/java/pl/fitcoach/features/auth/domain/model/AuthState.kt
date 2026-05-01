package pl.fitcoach.features.auth.domain.model

sealed class AuthState {
    data object Loading : AuthState()
    data class Authenticated(val userId: String, val role: UserRole) : AuthState()
    data object Unauthenticated : AuthState()
}

enum class UserRole {
    TRAINER, CLIENT;

    companion object {
        fun fromString(value: String): UserRole = when (value.lowercase()) {
            "trainer" -> TRAINER
            else -> CLIENT
        }
    }
}

data class UserInfo(
    val id: String,
    val email: String,
    val role: UserRole
)
