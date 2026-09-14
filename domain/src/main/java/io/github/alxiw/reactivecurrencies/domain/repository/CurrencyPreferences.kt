package io.github.alxiw.reactivecurrencies.domain.repository

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single

interface CurrencyPreferences {
    val fromCurrency: Maybe<String>
    val toCurrency: Maybe<String>
    fun saveFrom(code: String): Completable
    fun saveTo(code: String): Completable
    fun saveBaseCurrency(code: String, value: String): Completable
    fun loadBaseCurrency(): Single<Pair<String, String>>
    fun saveUpdateDate(date: String): Completable
    fun loadUpdateDate(): Maybe<String>
}
