package pl.fitcoach.features.auth.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import pl.fitcoach.features.auth.domain.model.AuthState
import pl.fitcoach.features.auth.domain.model.UserInfo
import pl.fitcoach.features.auth.domain.model.UserRole
import pl.fitcoach.features.auth.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient
) : AuthRepository {

    override suspend fun login(email: String, password: String): Result<UserInfo> = runCatching {
        supabaseClient.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
        getCurrentUser() ?: error("Nie można pobrać danych użytkownika po zalogowaniu")
    }

    override suspend fun register(
        email: String,
        password: String,
        role: String
    ): Result<UserInfo> = runCatching {
        supabaseClient.auth.signUpWith(Email) {
            this.email = email
            this.password = password
            data = kotlinx.serialization.json.buildJsonObject {
                put("role", kotlinx.serialization.json.JsonPrimitive(role))
            }
        }
        getCurrentUser() ?: error("Nie można pobrać danych użytkownika po rejestracji")
    }

    override suspend fun logout() {
        supabaseClient.auth.signOut()
    }

    override suspend fun getCurrentUser(): UserInfo? {
        val user = supabaseClient.auth.currentUserOrNull() ?: return null
        val roleString = user.userMetadata
            ?.get("role")
            ?.jsonPrimitive
            ?.contentOrNull
            ?: "client"
        return UserInfo(
            id = user.id,
            email = user.email ?: "",
            role = UserRole.fromString(roleString)
        )
    }

    override fun observeAuthState(): Flow<AuthState> = flow {
        emit(AuthState.Loading)
        val user = getCurrentUser()
        if (user != null) {
            emit(AuthState.Authenticated(user.id, user.role))
        } else {
            emit(AuthState.Unauthenticated)
        }
    }
}
