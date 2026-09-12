package io.github.alxiw.reactivecurrencies.di

import android.content.Context
import io.github.alxiw.reactivecurrencies.data.di.DataContainer
import io.github.alxiw.reactivecurrencies.data.di.DefaultDataContainer
import io.github.alxiw.reactivecurrencies.domain.usecase.ChangeBaseCurrencyUseCase
import io.github.alxiw.reactivecurrencies.domain.usecase.ChangeValueUseCase
import io.github.alxiw.reactivecurrencies.domain.usecase.GetCurrenciesUseCase
import io.github.alxiw.reactivecurrencies.domain.usecase.UpdateCurrenciesUseCase
import io.github.alxiw.reactivecurrencies.presentation.CurrenciesViewModelFactory

interface AppContainer {
    val viewModelFactory: CurrenciesViewModelFactory
}

class DefaultAppContainer(context: Context) : AppContainer {

    private val dataContainer: DataContainer = DefaultDataContainer(context)

    override val viewModelFactory: CurrenciesViewModelFactory by lazy {
        CurrenciesViewModelFactory(
            GetCurrenciesUseCase(dataContainer.currenciesRepository),
            UpdateCurrenciesUseCase(dataContainer.currenciesRepository),
            ChangeBaseCurrencyUseCase(dataContainer.currenciesRepository),
            ChangeValueUseCase(dataContainer.currenciesRepository)
        )
    }
}
