package io.github.alxiw.reactivecurrencies

import android.app.Application
import io.github.alxiw.reactivecurrencies.di.AppContainer
import io.github.alxiw.reactivecurrencies.di.DefaultAppContainer

class App : Application() {

    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}
