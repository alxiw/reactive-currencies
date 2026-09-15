package io.github.alxiw.reactivecurrencies.presentation.currencies

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import io.github.alxiw.reactivecurrencies.domain.model.Currency
import io.github.alxiw.reactivecurrencies.domain.usecase.ChangeBaseCurrencyUseCase
import io.github.alxiw.reactivecurrencies.domain.usecase.ChangeValueUseCase
import io.github.alxiw.reactivecurrencies.domain.usecase.GetCurrenciesUseCase
import io.github.alxiw.reactivecurrencies.domain.usecase.UpdateCurrenciesUseCase
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.PublishSubject

internal sealed interface CurrenciesIntent {
    data object LoadInitial : CurrenciesIntent
    data object Refresh : CurrenciesIntent
    data object Retry : CurrenciesIntent
    data class SelectCurrency(val currency: Currency) : CurrenciesIntent
    data class ChangeValue(val currency: Currency) : CurrenciesIntent
}

internal data class CurrenciesUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val currencies: List<Currency> = emptyList(),
) {
    val showList: Boolean get() = currencies.isNotEmpty()
    val showStub: Boolean get() = currencies.isEmpty() && !isLoading
}

internal sealed interface CurrenciesEvent {
    data class ShowLoadingSuccess(val info: String) : CurrenciesEvent
    data object ShowLoadingError : CurrenciesEvent
    data object ShowUpdatingError : CurrenciesEvent
    data object ScrollToTop : CurrenciesEvent
}

internal class CurrenciesViewModel(
    private val getCurrenciesUseCase: GetCurrenciesUseCase,
    private val updateCurrenciesUseCase: UpdateCurrenciesUseCase,
    private val changeBaseCurrencyUseCase: ChangeBaseCurrencyUseCase,
    private val changeValueUseCase: ChangeValueUseCase,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val compositeDisposable = CompositeDisposable()

    private val intents = PublishSubject.create<CurrenciesIntent>()
    private val stateSubject = BehaviorSubject.createDefault(CurrenciesUiState())
    private val eventSubject = PublishSubject.create<CurrenciesEvent>()

    private var initialLoadSubmitted = false

    val state: Observable<CurrenciesUiState> = stateSubject.hide()
    val events: Observable<CurrenciesEvent> = eventSubject.hide()

    private val results: Observable<Result> = intents
        .flatMap(::handleIntent)
        .share()

    init {
        compositeDisposable.add(
            results
                .scan(CurrenciesUiState(), ::reduce)
                .distinctUntilChanged()
                .subscribe(stateSubject::onNext)
        )
        compositeDisposable.add(
            results
                .subscribe { result ->
                    toEvent(result)?.let(eventSubject::onNext)
                }
        )
    }

    fun submit(intent: CurrenciesIntent) {
        intents.onNext(intent)
    }

    fun saveScrollPosition(position: Int) {
        savedStateHandle[SCROLL_POSITION_KEY] = position
    }

    fun restoreScrollPosition(): Int? = savedStateHandle[SCROLL_POSITION_KEY]

    override fun onCleared() {
        compositeDisposable.dispose()
    }

    private fun handleIntent(intent: CurrenciesIntent): Observable<Result> = when (intent) {
        is CurrenciesIntent.LoadInitial -> {
            if (initialLoadSubmitted) {
                // already loaded or loading — no need to reload on configuration change
                Observable.empty()
            } else {
                initialLoadSubmitted = true
                loadInitial().startWithItem(Result.Loading)
            }
        }
        is CurrenciesIntent.Refresh,
        is CurrenciesIntent.Retry -> refresh().startWithItem(Result.Refreshing)
        is CurrenciesIntent.SelectCurrency -> changeBaseCurrency(intent.currency)
        is CurrenciesIntent.ChangeValue -> changeValue(intent.currency)
    }

    private fun loadInitial(): Observable<Result> =
        getCurrenciesUseCase()
            .map<Result> { Result.DataLoaded(it, null) }
            .onErrorResumeNext {
                updateCurrenciesUseCase()
                    .flatMap { info ->
                        getCurrenciesUseCase()
                            .map { list -> Result.DataLoaded(list, info) as Result }
                    }
                    .onErrorReturn { Result.LoadFailed as Result }
            }
            .toObservable()
            .subscribeOn(Schedulers.io())

    private fun refresh(): Observable<Result> =
        updateCurrenciesUseCase()
            .flatMap { info ->
                getCurrenciesUseCase()
                    .map { list -> Result.DataLoaded(list, info) as Result }
            }
            .onErrorReturn { Result.RefreshFailed as Result }
            .toObservable()
            .subscribeOn(Schedulers.io())

    private fun changeBaseCurrency(currency: Currency): Observable<Result> =
        changeBaseCurrencyUseCase(currency)
            .map<Result> { Result.BaseCurrencyChanged(it) }
            .toObservable()
            .onErrorReturn { Result.UpdateFailed }
            .subscribeOn(Schedulers.io())

    private fun changeValue(currency: Currency): Observable<Result> =
        changeValueUseCase(currency)
            .map<Result> { Result.CurrenciesUpdated(it) }
            .toObservable()
            .onErrorReturn { Result.UpdateFailed }
            .subscribeOn(Schedulers.io())

    private fun reduce(state: CurrenciesUiState, result: Result): CurrenciesUiState = when (result) {
        is Result.Loading -> state.copy(isLoading = true, isRefreshing = false)
        is Result.Refreshing -> state.copy(isRefreshing = true)
        is Result.DataLoaded -> state.copy(
            isLoading = false,
            isRefreshing = false,
            currencies = result.list
        )
        is Result.LoadFailed -> state.copy(isLoading = false, isRefreshing = false)
        is Result.RefreshFailed -> state.copy(isRefreshing = false)
        is Result.BaseCurrencyChanged -> state.copy(
            isLoading = false,
            isRefreshing = false,
            currencies = result.list
        )
        is Result.CurrenciesUpdated -> state.copy(
            isLoading = false,
            isRefreshing = false,
            currencies = result.list
        )
        is Result.UpdateFailed -> state.copy(isRefreshing = false)
    }

    private fun toEvent(result: Result): CurrenciesEvent? = when (result) {
        is Result.DataLoaded -> result.info?.let { CurrenciesEvent.ShowLoadingSuccess(it) }
        is Result.LoadFailed,
        is Result.RefreshFailed -> CurrenciesEvent.ShowLoadingError
        is Result.UpdateFailed -> CurrenciesEvent.ShowUpdatingError
        is Result.BaseCurrencyChanged -> CurrenciesEvent.ScrollToTop
        is Result.Loading,
        is Result.Refreshing,
        is Result.CurrenciesUpdated -> null
    }

    private sealed interface Result {
        data object Loading : Result
        data object Refreshing : Result
        data class DataLoaded(val list: List<Currency>, val info: String?) : Result
        data object LoadFailed : Result
        data object RefreshFailed : Result
        data class BaseCurrencyChanged(val list: List<Currency>) : Result
        data class CurrenciesUpdated(val list: List<Currency>) : Result
        data object UpdateFailed : Result
    }
}

private const val SCROLL_POSITION_KEY = "currencies_scroll_position"
