package io.github.alxiw.reactivecurrencies.data.local

import io.github.alxiw.reactivecurrencies.data.local.model.CurrencyDto
import io.github.alxiw.reactivecurrencies.domain.model.Currency
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import java.math.BigDecimal
import java.math.MathContext
import kotlin.collections.sortedWith

internal class LocalDataSource(private val dao: CurrencyDao) {

    fun saveCurrencyList(set: Set<CurrencyDto>): Completable {
        return dao.save(set.toList())
    }

    fun calculateCurrencyList(baseCode: String, baseValue: String): Single<List<Currency>> {
        return dao.load()
            .flatMap { list ->
                if (list.isNotEmpty()) {
                    Single.just(list)
                } else {
                    Single.error(RuntimeException("local storage is empty"))
                }
            }
            .map { list ->
                val baseAmount = baseValue.toBigDecimalOrNull()
                    ?: error("inappropriate base value format: $baseValue")
                val baseRate = list.rateOf(baseCode)

                list.map { dto ->
                    val value = baseAmount
                        .multiply(baseRate)
                        .divide(dto.rate(), MathContext.DECIMAL64)
                    Currency(dto.code, value, dto.code == baseCode, dto.nominal, dto.name)
                }.sortedWith(compareBy({ !it.isBase }, { it.code }))
            }
    }

    fun getCodes(): Single<List<Pair<String, String>>> {
        return dao.load()
            .map { list ->
                list.sortedWith(compareBy { it.code })
                    .map { it.code to it.name.orEmpty() }
            }

    }

    fun convertValue(from: String, to: String, value: BigDecimal): Single<BigDecimal> {
        if (from == to) return Single.just(value)
        return dao.loadByCodes(listOf(from, to))
            .map { list ->
                value
                    .multiply(list.rateOf(from))
                    .divide(list.rateOf(to), MathContext.DECIMAL64)
            }
    }
}

private fun CurrencyDto.rate(): BigDecimal =
    value.toBigDecimalOrNull() ?: error("inappropriate rate format for $code")

private fun List<CurrencyDto>.rateOf(code: String): BigDecimal =
    firstOrNull { it.code == code }?.rate() ?: error("rate not found for $code")
