package pl.fitcoach.features.dashboard.domain.usecase

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import pl.fitcoach.features.dashboard.domain.model.ClientProfile
import pl.fitcoach.features.dashboard.domain.repository.ClientDashboardRepository
import pl.fitcoach.features.habits.domain.model.Habit
import pl.fitcoach.features.training.domain.model.TrainingPlan
import javax.inject.Inject

class GetClientDashboardUseCase @Inject constructor(
    private val repository: ClientDashboardRepository
) {
    suspend operator fun invoke(): Triple<Result<ClientProfile>, Result<TrainingPlan?>, Result<List<Habit>>> =
        coroutineScope {
            val profileDeferred = async { repository.getClientProfile() }
            val planDeferred = async { repository.getActivePlan() }
            val habitsDeferred = async { repository.getTodayHabits() }

            Triple(
                profileDeferred.await(),
                planDeferred.await(),
                habitsDeferred.await()
            )
        }
}
