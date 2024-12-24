package io.github.alxiw.reactivecurrencies.presentation

import androidx.lifecycle.ViewModel
import io.github.alxiw.reactivecurrencies.data.CurrenciesRepository
import io.github.alxiw.reactivecurrencies.data.model.Currency
import io.github.alxiw.reactivecurrencies.data.network.NetworkRetryManager
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.PublishSubject

class CurrenciesViewModel(private val currenciesRepository: CurrenciesRepository) : ViewModel() {

    private val retryManager = NetworkRetryManager()

    private val compositeDisposable = CompositeDisposable()

    private val eventSubject = PublishSubject.create<LoadEvent>()
    val eventObservable: Observable<LoadEvent> = eventSubject.hide()

    fun init() {
        val disposable = retryManager.observeRetries().subscribe {
            triggerEvent(LoadEvent.ShowRefreshing())
            updateAllCurrencies(fromUi = true)
        }
        compositeDisposable.add(disposable)
    }

    fun getAllCurrencies(fromUi: Boolean) {
        val disposable = currenciesRepository.getAllCurrencies()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { list ->
                    triggerEvent(LoadEvent.ShowList(list, fromUi))
                },
                { error ->
                    if (fromUi) {
                        updateAllCurrencies(fromUi = false)
                    } else {
                        triggerEvent(LoadEvent.ShowStub())
                    }
                }
            )
        compositeDisposable.add(disposable)
    }

    fun updateAllCurrencies(fromUi: Boolean) {
        val disposable = currenciesRepository.updateAllCurrencies()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { info ->
                    getAllCurrencies(fromUi = false)
                    triggerEvent(LoadEvent.ShowLoadingSuccess(info))
                },
                { error ->
                    if (!fromUi) triggerEvent(LoadEvent.ShowStub())
                    triggerEvent(LoadEvent.ShowLoadingError())
                }
            )
        compositeDisposable.add(disposable)
    }

    fun onCurrencyClick(newBaseCurrency: Currency) {
        val disposable = currenciesRepository.changeBaseCurrency(newBaseCurrency)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { list ->
                    triggerEvent(LoadEvent.ShowList(list, false))
                },
                { error ->
                    triggerEvent(LoadEvent.ShowUpdatingError())
                }
            )
        compositeDisposable.add(disposable)
    }

    fun onValueChange(baseItem: Currency) {
        val disposable = currenciesRepository.changeValue(baseItem)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { list ->
                    triggerEvent(LoadEvent.ShowList(list, false))
                },
                { error ->
                    triggerEvent(LoadEvent.ShowUpdatingError())
                }
            )
        compositeDisposable.add(disposable)
    }

    fun retryCall() {
        retryManager.retry()
    }

    fun clear() {
        compositeDisposable.clear()
    }

    private fun triggerEvent(event: LoadEvent) {
        eventSubject.onNext(event)
    }

    sealed class LoadEvent {
        class ShowStub : LoadEvent()
        class ShowLoadingError : LoadEvent()
        class ShowLoadingSuccess(val info: String) : LoadEvent()
        class ShowList(val list: List<Currency>, val useSavedState: Boolean) : LoadEvent()
        class ShowRefreshing : LoadEvent()
        class ShowUpdatingError : LoadEvent()
    }
}
