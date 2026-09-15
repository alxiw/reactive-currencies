package io.github.alxiw.reactivecurrencies.data.local

import io.github.alxiw.reactivecurrencies.data.local.model.CurrencyDto
import io.github.alxiw.reactivecurrencies.domain.model.Currency
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
 * `value` holds the CBR rate per a single unit (`VunitRate`), so `nominal` takes no part in the
 * calculations: it only becomes the starting value of a currency once it is picked as the base one.
 *
 * Fixture: 1 RUB = 1 RUB, 1 USD = 100 RUB, 1 JPY = 0.5 RUB, 1 IDR = 0.005 RUB
 * (i.e. 10000 IDR = 50 RUB).
 */
internal class LocalDataSourceTest {

    private val currencies = listOf(
        CurrencyDto("RUB", "1.0", 100, "Russian Ruble"),
        CurrencyDto("USD", "100.0", 1, "US Dollar"),
        CurrencyDto("JPY", "0.5", 100, "Japanese Yen"),
        CurrencyDto("IDR", "0.005", 10000, "Indonesian Rupiah"),
    )

    private val localDataSource = LocalDataSource(FakeCurrencyDao(currencies))

    @Test
    internal fun convertValue_usesRatePerSingleUnit() {
        // 1 USD = 100 RUB, 1 JPY = 0.5 RUB -> 1 USD = 200 JPY
        val result = localDataSource.convertValue("USD", "JPY", BigDecimal.ONE).blockingGet()

        assertEquals(0, result.compareTo(BigDecimal(200)))
    }

    @Test
    internal fun convertValue_supportsCurrenciesWithLargeNominal() {
        // 10000 IDR = 50 RUB -> 1 USD = 20000 IDR
        val result = localDataSource.convertValue("USD", "IDR", BigDecimal.ONE).blockingGet()

        assertEquals(0, result.compareTo(BigDecimal(20000)))
    }

    @Test
    internal fun convertValue_ignoresNominal() {
        // the same rates quoted for other nominals must not change the result
        val otherNominal = LocalDataSource(
            FakeCurrencyDao(currencies.map { it.copy(nominal = it.nominal * 10) })
        )

        val result = otherNominal.convertValue("IDR", "USD", BigDecimal(10000)).blockingGet()

        // 10000 IDR = 50 RUB = 0.5 USD
        assertEquals(0, result.compareTo(BigDecimal("0.5")))
    }

    @Test
    internal fun convertValue_isReversible() {
        val result = localDataSource.convertValue("JPY", "USD", BigDecimal(200)).blockingGet()

        assertEquals(0, result.compareTo(BigDecimal.ONE))
    }

    @Test
    internal fun convertValue_returnsSameAmountForSameCurrency() {
        val result = localDataSource.convertValue("USD", "USD", BigDecimal("123.45")).blockingGet()

        assertEquals(0, result.compareTo(BigDecimal("123.45")))
    }

    @Test
    internal fun convertValue_failsWhenRateIsMissing() {
        assertThrows(IllegalStateException::class.java) {
            localDataSource.convertValue("USD", "EUR", BigDecimal.ONE).blockingGet()
        }
    }

    @Test
    internal fun convertValue_failsWhenStorageIsEmpty() {
        val empty = LocalDataSource(FakeCurrencyDao(emptyList()))

        val error = assertThrows(IllegalStateException::class.java) {
            empty.convertValue("USD", "JPY", BigDecimal.ONE).blockingGet()
        }

        assertEquals("rate not found for USD", error.message)
    }

    @Test
    internal fun convertValue_doesNotTouchStorageForSameCurrency() {
        val empty = LocalDataSource(FakeCurrencyDao(emptyList()))

        val result = empty.convertValue("USD", "USD", BigDecimal(10)).blockingGet()

        assertEquals(0, result.compareTo(BigDecimal(10)))
    }

    @Test
    internal fun calculateCurrencyList_failsWhenStorageIsEmpty() {
        val empty = LocalDataSource(FakeCurrencyDao(emptyList()))

        val error = assertThrows(RuntimeException::class.java) {
            empty.calculateCurrencyList("RUB", "100").blockingGet()
        }

        assertEquals("local storage is empty", error.message)
    }

    @Test
    internal fun calculateCurrencyList_expressesValuesInBaseCurrencyUnits() {
        val list = localDataSource.calculateCurrencyList("RUB", "100").blockingGet()

        // 100 RUB = 1 USD = 200 JPY = 20000 IDR
        assertEquals(0, list.amountOf("RUB").compareTo(BigDecimal(100)))
        assertEquals(0, list.amountOf("USD").compareTo(BigDecimal.ONE))
        assertEquals(0, list.amountOf("JPY").compareTo(BigDecimal(200)))
        assertEquals(0, list.amountOf("IDR").compareTo(BigDecimal(20000)))
    }

    @Test
    internal fun calculateCurrencyList_keepsNominalAndName() {
        val list = localDataSource.calculateCurrencyList("RUB", "100").blockingGet()

        assertEquals(100, list.first { it.code == "RUB" }.nominal)
        assertEquals(10000, list.first { it.code == "IDR" }.nominal)
        assertEquals("Indonesian Rupiah", list.first { it.code == "IDR" }.name)
    }

    @Test
    internal fun calculateCurrencyList_putsBaseFirstAndSortsRestByCode() {
        val codes = localDataSource.calculateCurrencyList("RUB", "100").blockingGet().map { it.code }

        assertEquals(listOf("RUB", "IDR", "JPY", "USD"), codes)
    }

    @Test
    internal fun calculateCurrencyList_supportsNonReferenceBase() {
        // 10000 IDR = 50 RUB -> 0.5 USD = 100 JPY
        val list = localDataSource.calculateCurrencyList("IDR", "10000").blockingGet()

        assertEquals(0, list.amountOf("USD").compareTo(BigDecimal("0.5")))
        assertEquals(0, list.amountOf("JPY").compareTo(BigDecimal(100)))
    }

    private fun List<Currency>.amountOf(code: String): BigDecimal = first { it.code == code }.value
}

private class FakeCurrencyDao(private val currencies: List<CurrencyDto>) : CurrencyDao {

    override fun save(list: List<CurrencyDto>): Completable = Completable.complete()

    override fun load(): Single<List<CurrencyDto>> = Single.just(currencies)

    override fun delete(): Completable = Completable.complete()

    override fun loadByCodes(codes: List<String>): Single<List<CurrencyDto>> =
        Single.just(currencies.filter { it.code in codes })
}
