package pl.fitcoach.features.dashboard.domain.repository

import pl.fitcoach.features.dashboard.domain.model.ClientProfile
import pl.fitcoach.features.habits.domain.model.Habit
import pl.fitcoach.features.training.domain.model.TrainingPlan

interface ClientDashboardRepository {
    suspend fun getClientProfile(): Result<ClientProfile>
    suspend fun getActivePlan(): Result<TrainingPlan?>
    suspend fun getTodayHabits(): Result<List<Habit>>
    suspend fun logHabit(habitId: String, completed: Boolean, value: Double?): Result<Unit>
}
