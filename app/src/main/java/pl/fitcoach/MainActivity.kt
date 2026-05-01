package pl.fitcoach

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import pl.fitcoach.core.ui.theme.FitCoachTheme
import pl.fitcoach.navigation.NavGraph

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FitCoachTheme {
                val navController = rememberNavController()
                NavGraph(navController = navController)
            }
        }
    }
}
