package io.github.alxiw.reactivecurrencies.data.storage

import androidx.annotation.WorkerThread
import io.github.alxiw.reactivecurrencies.data.model.Currency
import io.github.alxiw.reactivecurrencies.data.storage.model.CurrencyDto
import io.reactivex.rxjava3.core.Single
import java.math.BigDecimal

private const val REMOTE_CURRENCY = "RUB"

class LocalDataSource(private val database: AppDatabase) {

    @WorkerThread
    fun saveCurrencyList(set: Set<CurrencyDto>) {
        val list = set.map { item -> CurrencyDto(item.code, item.value) }
        database.currencyDao().save(list)
    }

    @WorkerThread
    fun calculateCurrencyList(baseCode: String, baseValue: String): Single<List<Currency>> {
        return database.currencyDao().load()
            .flatMap<List<CurrencyDto>> { list ->
                if (list.isNotEmpty()) {
                    Single.just(list)
                } else {
                    val e = RuntimeException("local storage is empty")
                    Single.error<List<CurrencyDto>>(e)
                }
            }
            .map { list ->
                val map = list.associate { it.code to 1.0 / it.value.toDouble() }
                val baseValue: BigDecimal = BigDecimal.valueOf(baseValue.toDouble())

                if (baseCode == REMOTE_CURRENCY) {
                    map.map { it ->
                        val code: String = it.key
                        val bd: BigDecimal = BigDecimal.valueOf(it.value)
                        // if baseValue is 0 transform to BigDecimal.ZERO
                        val value: BigDecimal = bd.multiply(baseValue)
                        Currency(code, value, code == baseCode)
                    }
                } else {
                    val coefficient: Double = map.entries.first { it.key == baseCode }.value
                    map.map { it ->
                        val code: String = it.key
                        val div = it.value / coefficient
                        val bdd = BigDecimal.valueOf(div)
                        // if baseValue is 0 transform to BigDecimal.ZERO
                        val value = bdd.multiply(baseValue)
                        Currency(code, value, code == baseCode)
                    }
                }.sortedWith(compareBy({ !it.isBase }, { it.code }))
            }
    }
}
