package pl.fitcoach.features.auth.domain.repository

import kotlinx.coroutines.flow.Flow
import pl.fitcoach.features.auth.domain.model.AuthState
import pl.fitcoach.features.auth.domain.model.UserInfo

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<UserInfo>
    suspend fun register(email: String, password: String, role: String): Result<UserInfo>
    suspend fun redeemInviteCode(code: String): Result<Unit>
    suspend fun logout()
    suspend fun getCurrentUser(): UserInfo?
    fun observeAuthState(): Flow<AuthState>
}
