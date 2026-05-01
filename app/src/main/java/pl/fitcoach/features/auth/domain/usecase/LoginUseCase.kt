package pl.fitcoach.features.auth.domain.usecase

import pl.fitcoach.features.auth.domain.model.UserInfo
import pl.fitcoach.features.auth.domain.repository.AuthRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): Result<UserInfo> {
        if (email.isBlank()) {
            return Result.failure(IllegalArgumentException("Email jest wymagany"))
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return Result.failure(IllegalArgumentException("Nieprawidłowy format email"))
        }
        if (password.length < 6) {
            return Result.failure(IllegalArgumentException("Hasło musi mieć minimum 6 znaków"))
        }
        return authRepository.login(email.trim(), password)
    }
}
