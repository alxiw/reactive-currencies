package io.github.alxiw.reactivecurrencies.data.local

import androidx.annotation.WorkerThread
import io.github.alxiw.reactivecurrencies.data.model.Currency
import io.github.alxiw.reactivecurrencies.data.local.model.CurrencyDto
import io.reactivex.rxjava3.core.Single
import java.math.BigDecimal

class LocalDataSource(private val database: AppDatabase) {

    @WorkerThread
    fun saveCurrencyList(set: Set<CurrencyDto>) {
        database.currencyDao().save(set.toList())
    }

    @WorkerThread
    fun calculateCurrencyList(baseCode: String, baseValue: String): Single<List<Currency>> {
        return database.currencyDao().load()
            .flatMap { list ->
                if (list.isNotEmpty()) {
                    Single.just(list)
                } else {
                    Single.error(RuntimeException("local storage is empty"))
                }
            }
            .map { list ->
                val map = list.associate { it.code to 1.0 / it.value.toDouble() }
                val parsedBaseValue = BigDecimal.valueOf(baseValue.toDouble())
                val coefficient = map.getValue(baseCode)

                map.map { (code, rate) ->
                    val value = BigDecimal.valueOf(rate / coefficient).multiply(parsedBaseValue)
                    Currency(code, value, code == baseCode)
                }.sortedWith(compareBy({ !it.isBase }, { it.code }))
            }
    }
}
