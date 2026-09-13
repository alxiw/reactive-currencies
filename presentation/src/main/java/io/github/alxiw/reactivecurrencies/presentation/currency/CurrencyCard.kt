package io.github.alxiw.reactivecurrencies.presentation.currency

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.alxiw.reactivecurrencies.domain.model.Currency
import io.github.alxiw.reactivecurrencies.presentation.theme.CurrenciesTheme
import kotlinx.coroutines.delay
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.time.Duration.Companion.milliseconds

private const val MAX_VALUE_LENGTH = 20

@Composable
fun CurrencyCard(
    currency: Currency,
    enableInput: Boolean,
    onItemClick: () -> Unit,
    onValueChanged: (BigDecimal) -> Unit,
    modifier: Modifier = Modifier,
) {
    val formattedValue = remember(currency.value) {
        currency.value.setScale(2, RoundingMode.HALF_UP).toPlainString()
    }
    var text by remember(currency.code) { mutableStateOf(formattedValue) }
    var lastEmitted by remember(currency.code) { mutableStateOf<BigDecimal?>(null) }

    // Push external (payload) updates into the field without emitting a change event.
    LaunchedEffect(formattedValue) {
        if (text != formattedValue) {
            text = formattedValue
        }
    }

    // User-input pipeline: mirrors CurrencyTextWatcher + debounce(1s) + distinctUntilChanged,
    // skipping invalid numbers and zero.
    LaunchedEffect(text) {
        if (text == formattedValue) return@LaunchedEffect
        delay(1_000.milliseconds)
        val number = text.toBigDecimalOrNull()
        if ((number != null) && (number.signum() != 0) && (number != lastEmitted)) {
            lastEmitted = number
            onValueChanged(number)
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (enableInput) Modifier else Modifier.clickable(onClick = onItemClick))
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.width(16.dp))
        Text(
            text = CurrencyUtil.getCurrencyIcon(currency.code),
            fontSize = 36.sp,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = CurrencyUtil.getCurrencyFullName(currency.code, currency.name),
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = currency.code,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.offset(y = 12.dp),
        ) {
            if (enableInput) {
                BasicTextField(
                    value = text,
                    onValueChange = { newText ->
                        if (newText.length <= MAX_VALUE_LENGTH) text = newText
                    },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.End,
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                )
            } else {
                Text(
                    text = text,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    textAlign = TextAlign.End,
                )
            }
            Text(
                modifier = Modifier.padding(start = 4.dp),
                text = CurrencyUtil.getCurrencySignBy(currency.code),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(16.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun CurrencyCardPreview() {
    CurrenciesTheme {
        Column {
            CurrencyCard(
                currency = Currency("RUB", BigDecimal("90.50")),
                enableInput = true,
                onItemClick = {},
                onValueChanged = {},
            )
            CurrencyCard(
                currency = Currency("USD", BigDecimal("1.00"), isBase = true),
                enableInput = false,
                onItemClick = {},
                onValueChanged = {},
            )
            CurrencyCard(
                currency = Currency("EUR", BigDecimal("0.85")),
                enableInput = false,
                onItemClick = {},
                onValueChanged = {},
            )
        }
    }
}
