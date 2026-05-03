package pl.fitcoach.features.dashboard.domain.usecase

import pl.fitcoach.features.dashboard.domain.repository.ClientDashboardRepository
import javax.inject.Inject

class LogHabitUseCase @Inject constructor(
    private val repository: ClientDashboardRepository
) {
    suspend operator fun invoke(
        habitId: String,
        completed: Boolean,
        value: Double? = null
    ): Result<Unit> = repository.logHabit(habitId, completed, value)
}
