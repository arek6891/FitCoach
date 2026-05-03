package pl.fitcoach.features.auth.data

import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.functions.functions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
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

    override suspend fun redeemInviteCode(code: String): Result<Unit> = runCatching {
        supabaseClient.functions.invoke(
            function = "redeem-invite-code",
            body = buildJsonObject { put("code", code)  }
        )
        Unit
    }.onFailure { error ->
        Log.e("AuthRepositoryImpl", "redeemInviteCode failed for code=$code", error)
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
