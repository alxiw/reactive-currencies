package io.github.alxiw.reactivecurrencies.presentation.currency

import android.widget.EditText
import androidx.core.widget.addTextChangedListener
import io.reactivex.rxjava3.core.Observable
import java.math.BigDecimal

object CurrencyTextWatcher {

    /**
     * Emits the parsed value of [editText], skipping intermediate invalid
     * states (e.g. "1." while typing "1.5") and zero.
     * Disposing the subscription removes the underlying TextWatcher.
     */
    fun fromView(editText: EditText): Observable<BigDecimal> =
        Observable.create { emitter ->
            val watcher = editText.addTextChangedListener { editable ->
                val number = editable?.toString()?.toBigDecimalOrNull()
                if ((number != null) && (number.signum() != 0)) {
                    emitter.onNext(number)
                }
            }
            emitter.setCancellable { editText.removeTextChangedListener(watcher) }
        }
}
