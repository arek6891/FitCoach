package pl.fitcoach.features.splash.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import pl.fitcoach.features.auth.domain.model.AuthState
import pl.fitcoach.features.auth.domain.repository.AuthRepository
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    authRepository: AuthRepository
) : ViewModel() {

    val authState: StateFlow<AuthState> = authRepository
        .observeAuthState()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AuthState.Loading
        )
}
