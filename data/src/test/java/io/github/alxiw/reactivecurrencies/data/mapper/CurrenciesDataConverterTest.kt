package io.github.alxiw.reactivecurrencies.data.mapper

import io.github.alxiw.reactivecurrencies.data.remote.model.CbrCurrenciesResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
                },
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

    @Test
    fun convertsCommaRate_toDot() {
        val response = CbrCurrenciesResponse().apply {
            date = "01.01.2024"
            list = arrayListOf(
                CbrCurrenciesResponse.Currency().apply {
                    charCode = "USD"
                    rate = "90,1234"
                }
            )
        }

        val result = CurrenciesDataConverter.fromResponseToDto(response)

        assertEquals("90.1234", result.list.first { it.code == "USD" }.value)
    }

    @Test
    fun mapsDate_whenPresent() {
        val response = CbrCurrenciesResponse().apply {
            date = "15.03.2024"
            list = arrayListOf(
                CbrCurrenciesResponse.Currency().apply {
                    charCode = "USD"
                    rate = "90,0"
                }
            )
        }

        val result = CurrenciesDataConverter.fromResponseToDto(response)

        assertEquals("15.03.2024", result.date)
    }

    @Test
    fun usesCurrentDate_whenDateMissing() {
        val response = CbrCurrenciesResponse().apply {
            list = arrayListOf(
                CbrCurrenciesResponse.Currency().apply {
                    charCode = "USD"
                    rate = "90,0"
                }
            )
        }

        val result = CurrenciesDataConverter.fromResponseToDto(response)

        val expected = SimpleDateFormat("dd.MM.yyyy", Locale.ROOT)
            .format(Date(System.currentTimeMillis()))
        assertEquals(expected, result.date)
    }

    @Test
    fun addsBaseCurrency_whenListEmpty() {
        val response = CbrCurrenciesResponse().apply {
            date = "01.01.2024"
            list = arrayListOf()
        }

        val result = CurrenciesDataConverter.fromResponseToDto(response)

        assertEquals(1, result.list.size)
        val rub = result.list.first { it.code == "RUB" }
        assertEquals("1.0", rub.value)
        assertEquals(100, rub.nominal)
        assertEquals("Russian Ruble", rub.name)
    }

    @Test
    fun addsBaseCurrency_whenListNull() {
        val response = CbrCurrenciesResponse().apply {
            date = "01.01.2024"
            list = null
        }

        val result = CurrenciesDataConverter.fromResponseToDto(response)

        assertEquals(1, result.list.size)
        assertEquals("RUB", result.list.first().code)
    }

    @Test
    fun skipsItem_whenCharCodeMissing() {
        val response = CbrCurrenciesResponse().apply {
            date = "01.01.2024"
            list = arrayListOf(
                CbrCurrenciesResponse.Currency().apply {
                    rate = "90,0"
                }
            )
        }

        val result = CurrenciesDataConverter.fromResponseToDto(response)

        assertEquals(1, result.list.size)
        assertEquals("RUB", result.list.first().code)
    }

    @Test
    fun skipsItem_whenRateMissing() {
        val response = CbrCurrenciesResponse().apply {
            date = "01.01.2024"
            list = arrayListOf(
                CbrCurrenciesResponse.Currency().apply {
                    charCode = "USD"
                }
            )
        }

        val result = CurrenciesDataConverter.fromResponseToDto(response)

        assertEquals(1, result.list.size)
        assertEquals("RUB", result.list.first().code)
    }

    @Test
    fun keepsValidItems_andAddsBaseCurrency() {
        val response = CbrCurrenciesResponse().apply {
            date = "01.01.2024"
            list = arrayListOf(
                CbrCurrenciesResponse.Currency().apply {
                    charCode = "USD"
                    rate = "90,0"
                },
                CbrCurrenciesResponse.Currency().apply {
                    // missing rate -> skipped
                    charCode = "EUR"
                },
                CbrCurrenciesResponse.Currency().apply {
                    // missing charCode -> skipped
                    rate = "1,0"
                }
            )
        }

        val result = CurrenciesDataConverter.fromResponseToDto(response)

        assertEquals(2, result.list.size)
        assertTrue(result.list.any { it.code == "USD" })
        assertTrue(result.list.any { it.code == "RUB" })
    }
}
