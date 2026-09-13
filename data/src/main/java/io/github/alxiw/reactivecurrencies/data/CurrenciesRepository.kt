package io.github.alxiw.reactivecurrencies.data

import io.github.alxiw.reactivecurrencies.data.local.CurrencyDataStore
import io.github.alxiw.reactivecurrencies.data.local.LocalDataSource
import io.github.alxiw.reactivecurrencies.domain.model.Currency
import io.github.alxiw.reactivecurrencies.domain.repository.CurrenciesRepository as DomainRepository
import io.github.alxiw.reactivecurrencies.data.remote.RemoteDataSource
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.rx3.await
import kotlinx.coroutines.rx3.rxSingle

class CurrenciesRepository(
    private val localDataSource: LocalDataSource,
    private val remoteDataSource: RemoteDataSource,
    private val dataStore: CurrencyDataStore,
) : DomainRepository {

    override fun updateAllCurrencies(): Single<String> {
        return remoteDataSource.updateCurrenciesData()
            .flatMap { data ->
                rxSingle {
                    localDataSource.saveCurrencyList(data.list).await()
                    dataStore.saveUpdateDate(data.date)
                    data.date
                }
            }
    }

    override fun getAllCurrencies(): Single<List<Currency>> {
        return rxSingle { dataStore.loadBaseCurrency() }
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

    private fun updateBaseCurrency(code: String, value: String): Single<List<Currency>> {
        return rxSingle { dataStore.saveBaseCurrency(code, value) }
            .flatMap { localDataSource.calculateCurrencyList(code, value) }
    }
}
