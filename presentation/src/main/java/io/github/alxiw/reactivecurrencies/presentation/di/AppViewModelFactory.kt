package io.github.alxiw.reactivecurrencies.presentation.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import io.github.alxiw.reactivecurrencies.domain.prefs.CurrencyPreferences
import io.github.alxiw.reactivecurrencies.domain.usecase.ChangeBaseCurrencyUseCase
import io.github.alxiw.reactivecurrencies.domain.usecase.ChangeValueUseCase
import io.github.alxiw.reactivecurrencies.domain.usecase.ConvertValueUseCase
import io.github.alxiw.reactivecurrencies.domain.usecase.GetCodesUseCase
import io.github.alxiw.reactivecurrencies.domain.usecase.GetCurrenciesUseCase
import io.github.alxiw.reactivecurrencies.domain.usecase.UpdateCurrenciesUseCase
import io.github.alxiw.reactivecurrencies.presentation.converter.ConverterViewModel
import io.github.alxiw.reactivecurrencies.presentation.currencies.CurrenciesViewModel

class AppViewModelFactory(
    private val getCurrenciesUseCase: GetCurrenciesUseCase,
    private val updateCurrenciesUseCase: UpdateCurrenciesUseCase,
    private val changeBaseCurrencyUseCase: ChangeBaseCurrencyUseCase,
    private val changeValueUseCase: ChangeValueUseCase,
    private val getCodesUseCase: GetCodesUseCase,
    private val convertValueUseCase: ConvertValueUseCase,
    private val prefs: CurrencyPreferences,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        return when {
            modelClass.isAssignableFrom(CurrenciesViewModel::class.java) ->
                CurrenciesViewModel(
                    getCurrenciesUseCase,
                    updateCurrenciesUseCase,
                    changeBaseCurrencyUseCase,
                    changeValueUseCase,
                    extras.createSavedStateHandle()
                ) as T
            modelClass.isAssignableFrom(ConverterViewModel::class.java) ->
                ConverterViewModel(
                    getCodesUseCase,
                    convertValueUseCase,
                    prefs
                ) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
