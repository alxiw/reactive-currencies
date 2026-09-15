package io.github.alxiw.reactivecurrencies.data.remote

import io.github.alxiw.reactivecurrencies.data.remote.model.CbrCurrenciesResponse
import io.reactivex.rxjava3.core.Single
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

internal class RemoteDataSourceTest {

    private val apiService = FakeCbrApiService()

    private val remoteDataSource = RemoteDataSource(apiService)

    @Test
    internal fun updateCurrenciesData_mapsValidResponse() {
        apiService.response = CbrCurrenciesResponse().apply {
            date = "01.01.2024"
            list = arrayListOf(
                CbrCurrenciesResponse.Currency().apply {
                    charCode = "USD"
                    nominal = "1"
                    name = "US Dollar"
                    rate = "90,0"
                }
            )
        }

        val result = remoteDataSource.updateCurrenciesData().blockingGet()

        assertEquals("01.01.2024", result.date)
        val usd = result.list.first { it.code == "USD" }
        assertEquals("90.0", usd.value)
        assertEquals("US Dollar", usd.name)
    }

    @Test
    internal fun updateCurrenciesData_failsWhenDateIsMissing() {
        apiService.response = CbrCurrenciesResponse().apply {
            list = arrayListOf(
                CbrCurrenciesResponse.Currency().apply {
                    charCode = "USD"
                    rate = "90,0"
                }
            )
        }

        val error = assertThrows(IllegalStateException::class.java) {
            remoteDataSource.updateCurrenciesData().blockingGet()
        }

        assertEquals("missing required fields", error.message)
    }

    @Test
    internal fun updateCurrenciesData_failsWhenListIsEmpty() {
        apiService.response = CbrCurrenciesResponse().apply {
            date = "01.01.2024"
            list = arrayListOf()
        }

        val error = assertThrows(IllegalStateException::class.java) {
            remoteDataSource.updateCurrenciesData().blockingGet()
        }

        assertEquals("missing required fields", error.message)
    }

    @Test
    internal fun updateCurrenciesData_failsWhenCurrencyHasNoCharCode() {
        apiService.response = CbrCurrenciesResponse().apply {
            date = "01.01.2024"
            list = arrayListOf(
                CbrCurrenciesResponse.Currency().apply {
                    rate = "90,0"
                }
            )
        }

        val error = assertThrows(IllegalStateException::class.java) {
            remoteDataSource.updateCurrenciesData().blockingGet()
        }

        assertEquals("missing required fields", error.message)
    }

    @Test
    internal fun updateCurrenciesData_failsWhenCurrencyHasNoRate() {
        apiService.response = CbrCurrenciesResponse().apply {
            date = "01.01.2024"
            list = arrayListOf(
                CbrCurrenciesResponse.Currency().apply {
                    charCode = "USD"
                }
            )
        }

        val error = assertThrows(IllegalStateException::class.java) {
            remoteDataSource.updateCurrenciesData().blockingGet()
        }

        assertEquals("missing required fields", error.message)
    }

    @Test
    internal fun updateCurrenciesData_propagatesApiError() {
        apiService.error = IllegalStateException("network failure")

        val error = assertThrows(IllegalStateException::class.java) {
            remoteDataSource.updateCurrenciesData().blockingGet()
        }

        assertEquals("network failure", error.message)
    }
}

private class FakeCbrApiService : CbrApiService {

    var response: CbrCurrenciesResponse? = null
    var error: Throwable? = null

    override fun getCbrCurrencies(): Single<CbrCurrenciesResponse> {
        error?.let { return Single.error(it) }
        return Single.just(response!!)
    }
}
