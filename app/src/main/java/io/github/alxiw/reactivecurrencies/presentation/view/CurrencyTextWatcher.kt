package io.github.alxiw.reactivecurrencies.presentation.view

import android.widget.EditText
import androidx.core.widget.addTextChangedListener
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.BehaviorSubject
import java.math.BigDecimal

object CurrencyTextWatcher {

    fun fromView(editText: EditText): Observable<BigDecimal> {
        val subject: BehaviorSubject<BigDecimal> = BehaviorSubject.create()
        editText.addTextChangedListener { editable ->
            editable?.let {
                val number = it.toString().toBigDecimalOrNull()
                if (number != null && number.compareTo(BigDecimal.ZERO) != 0) {
                    subject.onNext(it.toString().toBigDecimal())
                } else {
                    subject.onNext(BigDecimal.ZERO)
                }
            } ?: run {
                subject.onNext(BigDecimal.ZERO)
            }
        }
        return subject
    }
}
