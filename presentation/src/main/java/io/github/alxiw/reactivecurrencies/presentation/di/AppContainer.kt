package io.github.alxiw.reactivecurrencies.presentation.di

import io.github.alxiw.reactivecurrencies.presentation.CurrenciesViewModelFactory

interface AppContainer {
    val viewModelFactory: CurrenciesViewModelFactory
}
