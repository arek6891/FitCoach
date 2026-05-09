package pl.fitcoach.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import pl.fitcoach.features.auth.ui.LoginScreen
import pl.fitcoach.features.auth.ui.RegisterScreen
import pl.fitcoach.features.clients.ui.ClientDetailScreen
import pl.fitcoach.features.clients.ui.InviteCodesScreen
import pl.fitcoach.features.dashboard.ui.ClientDashboardScreen
import pl.fitcoach.features.dashboard.ui.TrainerDashboardScreen
import pl.fitcoach.features.splash.ui.SplashScreen
import pl.fitcoach.features.training.ui.TrainingPlanCreatorScreen
import pl.fitcoach.features.training.ui.TrainingPlanListScreen
import pl.fitcoach.features.workout.ui.ActiveWorkoutScreen

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(navController = navController)
        }

        composable(Screen.Login.route) {
            LoginScreen(navController = navController)
        }

        composable(Screen.Register.route) {
            RegisterScreen(navController = navController)
        }

        composable(Screen.TrainerDashboard.route) {
            TrainerDashboardScreen(navController = navController)
        }

        composable(Screen.InviteCodes.route) {
            InviteCodesScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.ClientDetail.route,
            arguments = listOf(navArgument("clientId") { type = NavType.StringType })
        ) {
            ClientDetailScreen(
                onBack = { navController.popBackStack() },
                onNavigateToPlans = { profileId ->
                    navController.navigate(Screen.TrainingPlanList.createRoute(profileId))
                }
            )
        }

        composable(
            route = Screen.TrainingPlanList.route,
            arguments = listOf(navArgument("clientId") { type = NavType.StringType })
        ) {
            TrainingPlanListScreen(
                onBack = { navController.popBackStack() },
                onCreatePlan = { clientId ->
                    navController.navigate(Screen.CreatePlan.createRoute(clientId))
                }
            )
        }

        composable(
            route = Screen.CreatePlan.route,
            arguments = listOf(navArgument("clientId") { type = NavType.StringType })
        ) {
            TrainingPlanCreatorScreen(
                onBack = { navController.popBackStack() },
                onPlanSaved = { navController.popBackStack() }
            )
        }

        composable(Screen.ClientDashboard.route) {
            ClientDashboardScreen(navController = navController)
        }

        composable(
            route = Screen.ActiveWorkout.route,
            arguments = listOf(
                navArgument("planId") { type = NavType.StringType },
                navArgument("dayId") { type = NavType.StringType }
            )
        ) {
            ActiveWorkoutScreen(navController = navController)
        }
    }
}
