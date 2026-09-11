package io.github.alxiw.reactivecurrencies.di

import android.content.Context
import io.github.alxiw.reactivecurrencies.data.di.DataContainer
import io.github.alxiw.reactivecurrencies.data.di.DefaultDataContainer
import io.github.alxiw.reactivecurrencies.presentation.CurrenciesViewModelFactory

interface AppContainer {
    val viewModelFactory: CurrenciesViewModelFactory
}

class DefaultAppContainer(context: Context) : AppContainer {

    private val dataContainer: DataContainer = DefaultDataContainer(context)

    override val viewModelFactory: CurrenciesViewModelFactory by lazy {
        CurrenciesViewModelFactory(dataContainer.currenciesRepository)
    }
}
