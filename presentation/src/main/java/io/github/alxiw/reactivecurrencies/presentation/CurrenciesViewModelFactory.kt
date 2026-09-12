package io.github.alxiw.reactivecurrencies.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import io.github.alxiw.reactivecurrencies.domain.usecase.ChangeBaseCurrencyUseCase
import io.github.alxiw.reactivecurrencies.domain.usecase.ChangeValueUseCase
import io.github.alxiw.reactivecurrencies.domain.usecase.GetCurrenciesUseCase
import io.github.alxiw.reactivecurrencies.domain.usecase.UpdateCurrenciesUseCase

class CurrenciesViewModelFactory(
    private val getCurrenciesUseCase: GetCurrenciesUseCase,
    private val updateCurrenciesUseCase: UpdateCurrenciesUseCase,
    private val changeBaseCurrencyUseCase: ChangeBaseCurrencyUseCase,
    private val changeValueUseCase: ChangeValueUseCase,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        if (modelClass.isAssignableFrom(CurrenciesViewModel::class.java)) {
            return CurrenciesViewModel(
                getCurrenciesUseCase,
                updateCurrenciesUseCase,
                changeBaseCurrencyUseCase,
                changeValueUseCase,
                extras.createSavedStateHandle()
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}