package io.github.alxiw.reactivecurrencies

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.alxiw.reactivecurrencies.presentation.CurrenciesScreen
import io.github.alxiw.reactivecurrencies.presentation.CurrenciesViewModel
import io.github.alxiw.reactivecurrencies.presentation.di.AppContainer
import io.github.alxiw.reactivecurrencies.presentation.theme.CurrenciesTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            CurrenciesTheme {
                val viewModel: CurrenciesViewModel = viewModel(
                    factory = (application as AppContainer).viewModelFactory,
                )
                CurrenciesScreen(viewModel = viewModel)
            }
        }
    }
}
