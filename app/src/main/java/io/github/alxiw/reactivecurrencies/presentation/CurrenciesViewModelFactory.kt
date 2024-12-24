package io.github.alxiw.reactivecurrencies.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.github.alxiw.reactivecurrencies.data.CurrenciesRepository

class CurrenciesViewModelFactory(
    private val currenciesRepository: CurrenciesRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CurrenciesViewModel::class.java)) {
            return CurrenciesViewModel(currenciesRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
