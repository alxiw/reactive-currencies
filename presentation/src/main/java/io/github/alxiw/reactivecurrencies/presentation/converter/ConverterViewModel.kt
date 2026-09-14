package io.github.alxiw.reactivecurrencies.presentation.converter

import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import io.github.alxiw.reactivecurrencies.domain.repository.CurrencyPreferences
import io.github.alxiw.reactivecurrencies.domain.usecase.ConvertValueUseCase
import io.github.alxiw.reactivecurrencies.domain.usecase.GetCodesUseCase
import io.github.alxiw.reactivecurrencies.presentation.util.CurrencyUtil
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.PublishSubject

sealed interface ConverterIntent {
    data object LoadInitial : ConverterIntent
    data object Retry : ConverterIntent
    data class SelectFrom(val code: String) : ConverterIntent
    data class SelectTo(val code: String) : ConverterIntent
    data class ChangeValue(val value: TextFieldValue) : ConverterIntent
    data object Swap : ConverterIntent
}

data class ConverterUiState(
    val isLoading: Boolean = true,
    val currencies: List<Triple<String, String, String>> = emptyList(),
    val selectedFrom: String = "",
    val selectedTo: String = "",
    val inputValue: TextFieldValue = TextFieldValue(""),
    val isSwapped: Boolean = false,
    val conversion: ConversionState = ConversionState.Idle,
) {
    val showContent: Boolean get() = currencies.isNotEmpty()
}

sealed interface ConversionState {
    data object Idle : ConversionState
    data object Loading : ConversionState
    data class Result(val value: String) : ConversionState
}

sealed interface ConverterEvent {
    data class ShowError(val message: String) : ConverterEvent
}

class ConverterViewModel(
    private val getCodes: GetCodesUseCase,
    private val convertValue: ConvertValueUseCase,
    private val prefs: CurrencyPreferences,
) : ViewModel() {

    private val compositeDisposable = CompositeDisposable()

    private val intents = PublishSubject.create<ConverterIntent>()
    private val stateSubject = BehaviorSubject.createDefault(ConverterUiState())
    private val eventSubject = PublishSubject.create<ConverterEvent>()

    private var initialLoadSubmitted = false

    val state: Observable<ConverterUiState> = stateSubject.hide()
    val events: Observable<ConverterEvent> = eventSubject.hide()

    private val results: Observable<Result> = intents
        .switchMap(::handleIntent)
        .share()

    init {
        compositeDisposable.add(
            results
                .scan(ConverterUiState(), ::reduce)
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

    fun submit(intent: ConverterIntent) {
        intents.onNext(intent)
    }

    override fun onCleared() {
        compositeDisposable.dispose()
    }

    private fun handleIntent(intent: ConverterIntent): Observable<Result> = when (intent) {
        is ConverterIntent.LoadInitial -> {
            if (initialLoadSubmitted) {
                // already loaded or loading — no need to reload on configuration change
                Observable.empty()
            } else {
                initialLoadSubmitted = true
                loadInitial().startWithItem(Result.Loading)
            }
        }
        is ConverterIntent.Retry -> {
            initialLoadSubmitted = true
            loadInitial().startWithItem(Result.Loading)
        }
        is ConverterIntent.SelectFrom -> selectFrom(intent.code)
        is ConverterIntent.SelectTo -> selectTo(intent.code)
        is ConverterIntent.ChangeValue -> changeValue(intent.value)
        is ConverterIntent.Swap -> swap()
    }

    private fun loadInitial(): Observable<Result> =
        getCodes()
            .toObservable()
            .flatMap { currencies ->
                val codes = currencies.mapTo(mutableSetOf()) { it.first }
                val firstCode = currencies.firstOrNull()?.first ?: ""

                Single.zip(
                    prefs.fromCurrency.toObservable().first(""),
                    prefs.toCurrency.toObservable().first(""),
                ) { savedFrom, savedTo ->
                    val from = savedFrom.takeIf { it in codes } ?: firstCode
                    val to = savedTo.takeIf { it in codes } ?: firstCode

                    Result.DataLoaded(
                        currencies = currencies.map {
                            Triple(it.first, CurrencyUtil.getCurrencyIcon(it.first), it.second)
                        },
                        from = from,
                        to = to,
                    ) as Result
                }.toObservable()
            }
            .flatMap { result ->
                val data = result as Result.DataLoaded
                convert(data.from, data.to, TextFieldValue("")).startWithItem(result)
            }
            .onErrorReturn { Result.LoadFailed(it.message ?: it.toString()) }
            .subscribeOn(Schedulers.io())

    private fun selectFrom(code: String): Observable<Result> {
        val current = stateSubject.value ?: ConverterUiState()
        saveFrom(code)
        return Observable
            .just<Result>(Result.FromChanged(code))
            .concatWith(convert(code, current.selectedTo, current.inputValue))
    }

    private fun selectTo(code: String): Observable<Result> {
        val current = stateSubject.value ?: ConverterUiState()
        saveTo(code)
        return Observable
            .just<Result>(Result.ToChanged(code))
            .concatWith(convert(current.selectedFrom, code, current.inputValue))
    }

    private fun changeValue(value: TextFieldValue): Observable<Result> {
        val current = stateSubject.value ?: ConverterUiState()
        return Observable
            .just<Result>(Result.ValueChanged(value))
            .concatWith(convert(current.selectedFrom, current.selectedTo, value))
    }

    private fun swap(): Observable<Result> {
        val current = stateSubject.value ?: ConverterUiState()
        val from = current.selectedTo
        val to = current.selectedFrom
        saveFrom(from)
        saveTo(to)
        return Observable
            .just<Result>(Result.Swapped(from, to))
            .concatWith(convert(from, to, current.inputValue))
    }

    private fun convert(from: String, to: String, input: TextFieldValue): Observable<Result> {
        if (from.isBlank() || to.isBlank()) {
            return Observable.empty()
        }
        return convertValue(from, to, input.text.toBigDecimalOrNull())
            .toObservable()
            .map<Result> { Result.ConversionResult(it) }
            .onErrorReturn { Result.ConversionFailed(it.message ?: it.toString()) }
            .subscribeOn(Schedulers.io())
            .startWithItem(Result.ConversionLoading)
    }

    private fun saveFrom(code: String) {
        compositeDisposable.add(
            prefs.saveFrom(code)
                .subscribeOn(Schedulers.io())
                .subscribe({}, {})
        )
    }

    private fun saveTo(code: String) {
        compositeDisposable.add(
            prefs.saveTo(code)
                .subscribeOn(Schedulers.io())
                .subscribe({}, {})
        )
    }

    private fun reduce(state: ConverterUiState, result: Result): ConverterUiState = when (result) {
        is Result.Loading -> state.copy(isLoading = true, currencies = emptyList())
        is Result.DataLoaded -> state.copy(
            isLoading = false,
            currencies = result.currencies,
            selectedFrom = result.from,
            selectedTo = result.to,
        )
        is Result.LoadFailed -> state.copy(isLoading = false)
        is Result.FromChanged -> state.copy(selectedFrom = result.code)
        is Result.ToChanged -> state.copy(selectedTo = result.code)
        is Result.ValueChanged -> state.copy(inputValue = result.value)
        is Result.Swapped -> state.copy(
            selectedFrom = result.from,
            selectedTo = result.to,
            isSwapped = !state.isSwapped,
        )
        is Result.ConversionLoading -> state.copy(conversion = ConversionState.Loading)
        is Result.ConversionResult -> state.copy(conversion = ConversionState.Result(result.value))
        is Result.ConversionFailed -> state.copy(conversion = ConversionState.Idle)
    }

    private fun toEvent(result: Result): ConverterEvent? = when (result) {
        is Result.LoadFailed -> ConverterEvent.ShowError(result.message)
        is Result.ConversionFailed -> ConverterEvent.ShowError(result.message)
        else -> null
    }

    private sealed interface Result {
        data object Loading : Result
        data class DataLoaded(
            val currencies: List<Triple<String, String, String>>,
            val from: String,
            val to: String,
        ) : Result
        data class LoadFailed(val message: String) : Result
        data class FromChanged(val code: String) : Result
        data class ToChanged(val code: String) : Result
        data class ValueChanged(val value: TextFieldValue) : Result
        data class Swapped(val from: String, val to: String) : Result
        data object ConversionLoading : Result
        data class ConversionResult(val value: String) : Result
        data class ConversionFailed(val message: String) : Result
    }
}
