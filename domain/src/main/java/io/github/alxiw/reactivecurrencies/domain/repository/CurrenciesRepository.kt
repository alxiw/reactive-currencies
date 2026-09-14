package io.github.alxiw.reactivecurrencies.domain.repository

import io.github.alxiw.reactivecurrencies.domain.model.Currency
import io.reactivex.rxjava3.core.Single
import java.math.BigDecimal

interface CurrenciesRepository {
    fun updateAllCurrencies(): Single<String>
    fun getAllCurrencies(): Single<List<Currency>>
    fun changeBaseCurrency(currency: Currency): Single<List<Currency>>
    fun changeValue(baseCurrency: Currency): Single<List<Currency>>
    fun getCodes(): Single<List<Pair<String, String>>>
    fun convertValue(from: String, to: String, value: BigDecimal): Single<BigDecimal>
}
