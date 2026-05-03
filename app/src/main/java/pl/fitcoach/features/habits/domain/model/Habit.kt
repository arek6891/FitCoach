package pl.fitcoach.features.habits.domain.model

data class Habit(
    val id: String,
    val name: String,
    val description: String?,
    val type: HabitType,
    val targetValue: Double?,
    val unit: String?,
    val isCompleted: Boolean,
    val currentValue: Double?
)

enum class HabitType {
    BOOLEAN,
    QUANTITY
}
