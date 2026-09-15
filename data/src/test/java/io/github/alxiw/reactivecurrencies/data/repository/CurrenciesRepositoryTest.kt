package io.github.alxiw.reactivecurrencies.data.repository

import io.github.alxiw.reactivecurrencies.data.local.CurrencyDao
import io.github.alxiw.reactivecurrencies.data.prefs.CurrencyPreferences
import io.github.alxiw.reactivecurrencies.data.local.LocalDataSource
import io.github.alxiw.reactivecurrencies.data.local.model.CurrencyDto
import io.github.alxiw.reactivecurrencies.data.remote.CbrApiService
import io.github.alxiw.reactivecurrencies.data.remote.RemoteDataSource
import io.github.alxiw.reactivecurrencies.data.remote.model.CbrCurrenciesResponse
import io.github.alxiw.reactivecurrencies.domain.model.Currency
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import java.math.BigDecimal

/**
 * Fixture: 1 RUB = 1 RUB, 1 USD = 100 RUB, 1 JPY = 0.5 RUB, 1 IDR = 0.005 RUB
 * (i.e. 10000 IDR = 50 RUB).
 */
internal class CurrenciesRepositoryTest {

    private val dao = FakeCurrencyDao()
    private val apiService = FakeCbrApiService()
    private val prefs = mock(CurrencyPreferences::class.java)

    private val localDataSource = LocalDataSource(dao)
    private val remoteDataSource = RemoteDataSource(apiService)
    private val repository = CurrenciesRepository(localDataSource, remoteDataSource, prefs)

    @BeforeEach
    internal fun setUp() {
        dao.currencies = listOf(
            CurrencyDto("RUB", "1.0", 100, "Russian Ruble"),
            CurrencyDto("USD", "100.0", 1, "US Dollar"),
            CurrencyDto("JPY", "0.5", 100, "Japanese Yen"),
            CurrencyDto("IDR", "0.005", 10000, "Indonesian Rupiah"),
        )
    }

    @Test
    internal fun updateAllCurrencies_savesListAndDateAndReturnsDate() {
        apiService.response = CbrCurrenciesResponse().apply {
            date = "01.01.2024"
            list = arrayListOf(
                CbrCurrenciesResponse.Currency().apply {
                    charCode = "USD"
                    nominal = "1"
                    name = "US Dollar"
                    rate = "100,0"
                }
            )
        }
        `when`(prefs.saveUpdateDate("01.01.2024")).thenReturn(Completable.complete())

        val result = repository.updateAllCurrencies().blockingGet()

        assertEquals("01.01.2024", result)
        // the converter always appends the default base currency RUB
        assertEquals(
            setOf(
                CurrencyDto("USD", "100.0", 1, "US Dollar"),
                CurrencyDto("RUB", "1.0", 100, "Russian Ruble"),
            ),
            dao.savedList,
        )
    }

    @Test
    internal fun updateAllCurrencies_propagatesRemoteError() {
        apiService.error = IllegalStateException("network failure")

        val error = assertThrows(IllegalStateException::class.java) {
            repository.updateAllCurrencies().blockingGet()
        }

        assertEquals("network failure", error.message)
    }

    @Test
    internal fun getAllCurrencies_usesPrefsBaseCurrency() {
        `when`(prefs.loadBaseCurrency()).thenReturn(Single.just("RUB" to "100"))

        val list = repository.getAllCurrencies().blockingGet()

        assertEquals(0, list.first { it.code == "USD" }.value.compareTo(BigDecimal.ONE))
    }

    @Test
    internal fun changeBaseCurrency_seedsValueWithNominal() {
        `when`(prefs.saveBaseCurrency("IDR", "10000")).thenReturn(Completable.complete())
        val idr = Currency("IDR", BigDecimal.ZERO, nominal = 10000, name = "Indonesian Rupiah")

        val list = repository.changeBaseCurrency(idr).blockingGet()

        // 10000 IDR = 50 RUB -> 0.5 USD
        assertEquals(0, list.first { it.code == "USD" }.value.compareTo(BigDecimal("0.5")))
    }

    @Test
    internal fun changeValue_seedsValueWithCurrentValue() {
        `when`(prefs.saveBaseCurrency("USD", "100")).thenReturn(Completable.complete())
        val usd = Currency("USD", BigDecimal("100"), name = "US Dollar")

        val list = repository.changeValue(usd).blockingGet()

        // 100 USD = 10000 RUB -> 20000 JPY
        assertEquals(0, list.first { it.code == "JPY" }.value.compareTo(BigDecimal(20000)))
    }

    @Test
    internal fun getCodes_delegatesToLocalDataSource() {
        val result = repository.getCodes().blockingGet()

        assertEquals(listOf("IDR", "JPY", "RUB", "USD"), result.map { it.first })
    }

    @Test
    internal fun convertValue_delegatesToLocalDataSource() {
        // 1 USD = 100 RUB, 1 JPY = 0.5 RUB -> 1 USD = 200 JPY
        val result = repository.convertValue("USD", "JPY", BigDecimal.ONE).blockingGet()

        assertEquals(0, result.compareTo(BigDecimal(200)))
    }
}

private class FakeCurrencyDao : CurrencyDao {

    var currencies: List<CurrencyDto> = emptyList()
    var savedList: Set<CurrencyDto>? = null

    override fun save(list: List<CurrencyDto>): Completable {
        savedList = list.toSet()
        return Completable.complete()
    }

    override fun load(): Single<List<CurrencyDto>> = Single.just(currencies)

    override fun loadByCodes(codes: List<String>): Single<List<CurrencyDto>> =
        Single.just(currencies.filter { it.code in codes })

    override fun delete(): Completable = Completable.complete()
}

private class FakeCbrApiService : CbrApiService {

    var response: CbrCurrenciesResponse? = null
    var error: Throwable? = null

    override fun getCbrCurrencies(): Single<CbrCurrenciesResponse> {
        error?.let { return Single.error(it) }
        return Single.just(response!!)
    }
}
