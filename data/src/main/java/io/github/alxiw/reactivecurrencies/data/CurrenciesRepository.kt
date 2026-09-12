package io.github.alxiw.reactivecurrencies.data

import androidx.annotation.WorkerThread
import io.github.alxiw.reactivecurrencies.data.local.CurrencySharedPreferences
import io.github.alxiw.reactivecurrencies.data.local.LocalDataSource
import io.github.alxiw.reactivecurrencies.domain.model.Currency
import io.github.alxiw.reactivecurrencies.domain.repository.CurrenciesRepository as DomainRepository
import io.github.alxiw.reactivecurrencies.data.remote.RemoteDataSource
import io.reactivex.rxjava3.core.Single
import java.math.BigDecimal

class CurrenciesRepository(
    private val localDataSource: LocalDataSource,
    private val remoteDataSource: RemoteDataSource,
    private val sharedPreferences: CurrencySharedPreferences
) : DomainRepository {

    @WorkerThread
    override fun updateAllCurrencies(): Single<String> {
        return remoteDataSource.updateCurrenciesData()
            .doOnSuccess {
                localDataSource.saveCurrencyList(it.list)
                sharedPreferences.saveUpdateDate(it.date)
            }
            .flatMap { Single.just(it.date) }
    }

    @WorkerThread
    override fun getAllCurrencies(): Single<List<Currency>> {
        val pair = sharedPreferences.loadBaseCurrency()
        return localDataSource.calculateCurrencyList(pair.first, pair.second)
    }

    @WorkerThread
    override fun changeBaseCurrency(currency: Currency): Single<List<Currency>> {
        val code = currency.code
        val value = BigDecimal.ONE.toString() // reset value of new base currency to 1.00
        return updateBaseCurrency(code, value)
    }

    @WorkerThread
    override fun changeValue(baseCurrency: Currency): Single<List<Currency>> {
        val code = baseCurrency.code
        val value = baseCurrency.value.toString()
        return updateBaseCurrency(code, value)
    }

    private fun updateBaseCurrency(code: String, value: String): Single<List<Currency>> {
        sharedPreferences.saveBaseCurrency(code, value)
        return localDataSource.calculateCurrencyList(code, value)
    }
}
