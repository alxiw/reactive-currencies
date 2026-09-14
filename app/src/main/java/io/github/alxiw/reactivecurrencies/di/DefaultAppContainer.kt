package io.github.alxiw.reactivecurrencies.di

import android.content.Context
import io.github.alxiw.reactivecurrencies.data.di.DataContainer
import io.github.alxiw.reactivecurrencies.data.di.DefaultDataContainer
import io.github.alxiw.reactivecurrencies.domain.usecase.ChangeBaseCurrencyUseCase
import io.github.alxiw.reactivecurrencies.domain.usecase.ChangeValueUseCase
import io.github.alxiw.reactivecurrencies.domain.usecase.ConvertValueUseCase
import io.github.alxiw.reactivecurrencies.domain.usecase.GetCodesUseCase
import io.github.alxiw.reactivecurrencies.domain.usecase.GetCurrenciesUseCase
import io.github.alxiw.reactivecurrencies.domain.usecase.UpdateCurrenciesUseCase
import io.github.alxiw.reactivecurrencies.presentation.CurrenciesViewModelFactory
import io.github.alxiw.reactivecurrencies.presentation.di.AppContainer

class DefaultAppContainer(context: Context) : AppContainer {

    private val dataContainer: DataContainer = DefaultDataContainer(context)

    override val viewModelFactory: CurrenciesViewModelFactory by lazy {
        CurrenciesViewModelFactory(
            GetCurrenciesUseCase(dataContainer.currenciesRepository),
            UpdateCurrenciesUseCase(dataContainer.currenciesRepository),
            ChangeBaseCurrencyUseCase(dataContainer.currenciesRepository),
            ChangeValueUseCase(dataContainer.currenciesRepository),
            GetCodesUseCase(dataContainer.currenciesRepository),
            ConvertValueUseCase(dataContainer.currenciesRepository),
            dataContainer.currencyPreferences
        )
    }
}
