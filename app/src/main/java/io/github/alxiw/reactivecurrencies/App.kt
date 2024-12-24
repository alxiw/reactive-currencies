package io.github.alxiw.reactivecurrencies

import android.app.Application

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        Dependencies.init(applicationContext)
    }
}
