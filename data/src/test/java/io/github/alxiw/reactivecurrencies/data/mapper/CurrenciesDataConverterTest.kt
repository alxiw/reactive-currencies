package io.github.alxiw.reactivecurrencies.data.mapper

import io.github.alxiw.reactivecurrencies.data.remote.model.CbrCurrenciesResponse
import org.junit.Assert.assertEquals
import org.junit.Test

class CurrenciesDataConverterTest {

    @Test
    fun mapsNominal_whenPresent() {
        val response = CbrCurrenciesResponse().apply {
            date = "01.01.2024"
            list = arrayListOf(
                CbrCurrenciesResponse.Currency().apply {
                    charCode = "IDR"
                    nominal = "10000"
                    rate = "0,005855"
                }
            )
        }

        val result = CurrenciesDataConverter.fromResponseToDto(response)

        val idr = result.list.first { it.code == "IDR" }
        assertEquals(10000, idr.nominal)
    }

    @Test
    fun defaultsNominalToOne_whenMissing() {
        val response = CbrCurrenciesResponse().apply {
            date = "01.01.2024"
            list = arrayListOf(
                CbrCurrenciesResponse.Currency().apply {
                    charCode = "USD"
                    rate = "90,0"
                }
            )
        }

        val result = CurrenciesDataConverter.fromResponseToDto(response)

        val usd = result.list.first { it.code == "USD" }
        assertEquals(1, usd.nominal)
    }

    @Test
    fun baseCurrencyNominal_isHundred() {
        val response = CbrCurrenciesResponse().apply {
            date = "01.01.2024"
            list = arrayListOf(
                CbrCurrenciesResponse.Currency().apply {
                    charCode = "USD"
                    rate = "90,0"
                }
            )
        }

        val result = CurrenciesDataConverter.fromResponseToDto(response)

        val rub = result.list.first { it.code == "RUB" }
        assertEquals(100, rub.nominal)
        assertEquals("1.0", rub.value)
    }

    @Test
    fun mapsName_fromBackend() {
        val response = CbrCurrenciesResponse().apply {
            date = "01.01.2024"
            list = arrayListOf(
                CbrCurrenciesResponse.Currency().apply {
                    charCode = "USD"
                    name = "US Dollar"
                    rate = "90,0"
                }
            )
        }

        val result = CurrenciesDataConverter.fromResponseToDto(response)

        assertEquals("US Dollar", result.list.first { it.code == "USD" }.name)
    }

    @Test
    fun baseCurrencyName_isRussianRuble() {
        val response = CbrCurrenciesResponse().apply {
            date = "01.01.2024"
            list = arrayListOf(
                CbrCurrenciesResponse.Currency().apply {
                    charCode = "USD"
                    rate = "90,0"
                }
            )
        }

        val result = CurrenciesDataConverter.fromResponseToDto(response)

        assertEquals("Russian Ruble", result.list.first { it.code == "RUB" }.name)
    }
}
