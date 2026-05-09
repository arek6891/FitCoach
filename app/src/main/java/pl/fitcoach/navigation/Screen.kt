package pl.fitcoach.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Login : Screen("login")
    data object Register : Screen("register")

    // Trainer
    data object TrainerDashboard : Screen("trainer/dashboard")
    data object InviteCodes : Screen("trainer/invite-codes")
    data object ClientDetail : Screen("trainer/client/{clientId}") {
        fun createRoute(clientId: String) = "trainer/client/$clientId"
    }
    data object TrainingPlanList : Screen("trainer/plans/{clientId}") {
        fun createRoute(clientId: String) = "trainer/plans/$clientId"
    }
    data object CreatePlan : Screen("trainer/plan/create/{clientId}") {
        fun createRoute(clientId: String) = "trainer/plan/create/$clientId"
    }

    // Client
    data object ClientDashboard : Screen("client/dashboard")
    data object ActiveWorkout : Screen("client/workout/{planId}/{dayId}") {
        fun createRoute(planId: String, dayId: String) = "client/workout/$planId/$dayId"
    }
    data object FoodLog : Screen("client/food")
    data object Progress : Screen("client/progress")
    data object Habits : Screen("client/habits")

    // Shared
    data object Settings : Screen("settings")
    data object Subscription : Screen("settings/subscription")
}
