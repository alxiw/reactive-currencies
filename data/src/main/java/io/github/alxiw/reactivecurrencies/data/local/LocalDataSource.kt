package io.github.alxiw.reactivecurrencies.data.local

import androidx.annotation.WorkerThread
import io.github.alxiw.reactivecurrencies.domain.model.Currency
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
                val nominals = list.associate { it.code to it.nominal }
                val names = list.associate { it.code to it.name }
                val parsedBaseValue = BigDecimal.valueOf(baseValue.toDouble())
                val coefficient = map.getValue(baseCode)

                map.map { (code, rate) ->
                    val value = BigDecimal.valueOf(rate / coefficient).multiply(parsedBaseValue)
                    Currency(code, value, code == baseCode, nominals.getValue(code), names.getValue(code))
                }.sortedWith(compareBy({ !it.isBase }, { it.code }))
            }
    }
}
