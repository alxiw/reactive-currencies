package io.github.alxiw.reactivecurrencies

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import io.github.alxiw.reactivecurrencies.presentation.di.AppContainer
import io.github.alxiw.reactivecurrencies.presentation.navigation.AppNavHost
import io.github.alxiw.reactivecurrencies.presentation.navigation.rememberNavigator
import io.github.alxiw.reactivecurrencies.presentation.theme.CurrenciesTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            CurrenciesTheme {
                AppNavHost(
                    navigator = rememberNavigator(),
                    viewModelFactory = (application as AppContainer).viewModelFactory,
                )
            }
        }
    }
}
