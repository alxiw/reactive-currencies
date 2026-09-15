package io.github.alxiw.reactivecurrencies.data.repository

import io.github.alxiw.reactivecurrencies.data.local.LocalDataSource
import io.github.alxiw.reactivecurrencies.data.remote.RemoteDataSource
import io.github.alxiw.reactivecurrencies.domain.model.Currency
import io.github.alxiw.reactivecurrencies.domain.prefs.CurrencyPreferences
import io.github.alxiw.reactivecurrencies.domain.repository.CurrenciesRepository
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.rx3.await
import kotlinx.coroutines.rx3.rxSingle
import java.math.BigDecimal

class CurrenciesRepository internal constructor(
    private val localDataSource: LocalDataSource,
    private val remoteDataSource: RemoteDataSource,
    private val prefs: CurrencyPreferences,
) : CurrenciesRepository {

    override fun updateAllCurrencies(): Single<String> {
        return remoteDataSource.updateCurrenciesData()
            .flatMap { data ->
                rxSingle {
                    localDataSource.saveCurrencyList(data.list).await()
                    prefs.saveUpdateDate(data.date).await()
                    data.date
                }
            }
    }

    override fun getAllCurrencies(): Single<List<Currency>> {
        return prefs.loadBaseCurrency()
            .flatMap { (code, value) -> localDataSource.calculateCurrencyList(code, value) }
    }

    override fun changeBaseCurrency(currency: Currency): Single<List<Currency>> {
        val code = currency.code
        // seed the new base with its CBR nominal (e.g. 10000 for IDR)
        val value = currency.nominal.toString()
        return updateBaseCurrency(code, value)
    }

    override fun changeValue(baseCurrency: Currency): Single<List<Currency>> {
        val code = baseCurrency.code
        val value = baseCurrency.value.toString()
        return updateBaseCurrency(code, value)
    }

    override fun getCodes(): Single<List<Pair<String, String>>> {
        return localDataSource.getCodes()
    }

    override fun convertValue(from: String, to: String, value: BigDecimal): Single<BigDecimal> {
        return localDataSource.convertValue(from, to, value)
    }

    private fun updateBaseCurrency(code: String, value: String): Single<List<Currency>> {
        return prefs.saveBaseCurrency(code, value)
            .andThen(localDataSource.calculateCurrencyList(code, value))
    }
}
