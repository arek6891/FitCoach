package pl.fitcoach.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import pl.fitcoach.features.auth.ui.LoginScreen
import pl.fitcoach.features.auth.ui.RegisterScreen
import pl.fitcoach.features.dashboard.ui.ClientDashboardScreen
import pl.fitcoach.features.dashboard.ui.TrainerDashboardScreen
import pl.fitcoach.features.splash.ui.SplashScreen

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

        composable(Screen.ClientDashboard.route) {
            ClientDashboardScreen(navController = navController)
        }
    }
}
