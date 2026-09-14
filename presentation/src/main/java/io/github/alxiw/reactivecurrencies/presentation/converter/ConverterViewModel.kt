package io.github.alxiw.reactivecurrencies.presentation.converter

import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import io.github.alxiw.reactivecurrencies.domain.repository.CurrencyPreferences
import io.github.alxiw.reactivecurrencies.domain.usecase.ConvertValueUseCase
import io.github.alxiw.reactivecurrencies.domain.usecase.GetCodesUseCase
import io.github.alxiw.reactivecurrencies.presentation.util.CurrencyUtil
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.PublishSubject
import java.math.BigDecimal

data class ConverterUiState(
    val currenciesState: CurrenciesState = CurrenciesState.Loading,
    val conversionState: ConversionState = ConversionState.Idle,
    val selectedFrom: String = "",
    val selectedTo: String = "",
    val inputValue: TextFieldValue = TextFieldValue(""),
    val isSwapped: Boolean = false
)

sealed interface CurrenciesState {
    data object Loading : CurrenciesState
    data class Ready(val list: List<Triple<String, String, String>>) : CurrenciesState
}

sealed interface ConversionState {
    data object Idle : ConversionState
    data object Loading : ConversionState
    data class Result(val value: String) : ConversionState
}

class ConverterViewModel(
    private val getCodes: GetCodesUseCase,
    private val convertValue: ConvertValueUseCase,
    private val prefs: CurrencyPreferences,
) : ViewModel() {

    private val compositeDisposable = CompositeDisposable()

    private var loadDisposable: Disposable? = null
    private var convertDisposable: Disposable? = null

    private val stateSubject = BehaviorSubject.createDefault(ConverterUiState())
    private val eventSubject = PublishSubject.create<String>()

    private var codeFrom = ""
    private var codeTo = ""
    private var value: BigDecimal? = null

    private var isInitialized = false

    val state: Observable<ConverterUiState> = stateSubject.hide()
    val events: Observable<String> = eventSubject.hide()

    fun initData() {
        if (isInitialized) return
        isInitialized = true

        loadDisposable?.dispose()
        update { it.copy(currenciesState = CurrenciesState.Loading) }

        loadDisposable = getCodes()
            .subscribeOn(Schedulers.io())
            .flatMap { currencies ->
                val codes = currencies.mapTo(mutableSetOf()) { it.first }
                val firstCode = currencies.firstOrNull()?.first ?: ""

                Single.zip(
                    prefs.fromCurrency.toObservable().first(""),
                    prefs.toCurrency.toObservable().first(""),
                ) { savedFrom, savedTo ->
                    val from = savedFrom.takeIf { it in codes } ?: firstCode
                    val to = savedTo.takeIf { it in codes } ?: firstCode
                    Triple(currencies.map { Triple(it.first, CurrencyUtil.getCurrencyIcon(it.first), it.second) }, from, to)
                }
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { (currencies, from, to) ->
                    codeFrom = from
                    codeTo = to
                    update {
                        it.copy(
                            currenciesState = CurrenciesState.Ready(currencies),
                            selectedFrom = from,
                            selectedTo = to
                        )
                    }
                    convert()
                }
            ) { throwable ->
                isInitialized = false
                eventSubject.onNext(throwable.message ?: throwable.toString())
            }
    }

    fun updateFromCurrency(value: String?) {
        codeFrom = value ?: ""
        update { it.copy(selectedFrom = codeFrom) }
        compositeDisposable.add(
            prefs.saveFrom(codeFrom)
                .subscribeOn(Schedulers.io())
                .subscribe({}, {})
        )
        convert()
    }

    fun updateToCurrency(value: String?) {
        codeTo = value ?: ""
        update { it.copy(selectedTo = codeTo) }
        compositeDisposable.add(
            prefs.saveTo(codeTo)
                .subscribeOn(Schedulers.io())
                .subscribe({}, {})
        )
        convert()
    }

    fun updateValue(value: TextFieldValue) {
        this.value = value.text.toBigDecimalOrNull()
        update { it.copy(inputValue = value) }
        convert()
    }

    fun swapCurrencies() {
        val temp = codeFrom
        codeFrom = codeTo
        codeTo = temp
        update {
            it.copy(
                selectedFrom = codeFrom,
                selectedTo = codeTo,
                isSwapped = !it.isSwapped
            )
        }
        compositeDisposable.add(
            prefs.saveFrom(codeFrom)
                .subscribeOn(Schedulers.io())
                .subscribe({}, {})
        )
        compositeDisposable.add(
            prefs.saveTo(codeTo)
                .subscribeOn(Schedulers.io())
                .subscribe({}, {})
        )
        convert()
    }

    private fun convert() {
        if (codeFrom.isEmpty() || codeTo.isEmpty()) return

        convertDisposable?.dispose()
        update { it.copy(conversionState = ConversionState.Loading) }

        convertDisposable = convertValue(codeFrom, codeTo, value)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { result -> update { it.copy(conversionState = ConversionState.Result(result)) } },
                { throwable ->
                    isInitialized = false
                    eventSubject.onNext(throwable.message ?: throwable.toString())
                }
            )
    }

    private fun update(block: (ConverterUiState) -> ConverterUiState) {
        stateSubject.onNext(block(stateSubject.value ?: ConverterUiState()))
    }

    override fun onCleared() {
        compositeDisposable.dispose()
        loadDisposable?.dispose()
        convertDisposable?.dispose()
    }
}
