package io.github.alxiw.reactivecurrencies.domain.repository

import io.github.alxiw.reactivecurrencies.domain.model.Currency
import io.reactivex.rxjava3.core.Single

interface CurrenciesRepository {
    fun updateAllCurrencies(): Single<String>
    fun getAllCurrencies(): Single<List<Currency>>
    fun changeBaseCurrency(currency: Currency): Single<List<Currency>>
    fun changeValue(baseCurrency: Currency): Single<List<Currency>>
}
