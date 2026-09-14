package io.github.alxiw.reactivecurrencies

import android.app.Application
import io.github.alxiw.reactivecurrencies.di.DefaultAppContainer
import io.github.alxiw.reactivecurrencies.presentation.di.AppViewModelFactory
import io.github.alxiw.reactivecurrencies.presentation.di.AppContainer

class App : Application(), AppContainer {

    private lateinit var container: DefaultAppContainer

    override val viewModelFactory: AppViewModelFactory
        get() = container.viewModelFactory

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}
