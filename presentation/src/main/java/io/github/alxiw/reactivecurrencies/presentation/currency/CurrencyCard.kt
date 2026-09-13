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
import io.github.alxiw.reactivecurrencies.domain.model.Currency
import io.github.alxiw.reactivecurrencies.presentation.theme.CurrenciesTheme
import io.github.alxiw.reactivecurrencies.presentation.theme.IconTextSize
import io.github.alxiw.reactivecurrencies.presentation.theme.LabelTextSize
import io.github.alxiw.reactivecurrencies.presentation.theme.SpacingLarge
import io.github.alxiw.reactivecurrencies.presentation.theme.SpacingMedium
import io.github.alxiw.reactivecurrencies.presentation.theme.SpacingSmall
import io.github.alxiw.reactivecurrencies.presentation.theme.SpacingXSmall
import io.github.alxiw.reactivecurrencies.presentation.theme.TitleTextSize
import io.github.alxiw.reactivecurrencies.presentation.theme.ValueTextSize
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
            .padding(vertical = SpacingMedium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.width(SpacingLarge))
        Text(
            text = CurrencyUtil.getCurrencyIcon(currency.code),
            fontSize = IconTextSize,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(SpacingSmall))
        Column(Modifier.weight(1f)) {
            Text(
                text = CurrencyUtil.getCurrencyFullName(currency.code, currency.name),
                fontSize = TitleTextSize,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = currency.code,
                fontSize = LabelTextSize,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(SpacingSmall))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.offset(y = SpacingMedium),
        ) {
            if (enableInput) {
                BasicTextField(
                    value = text,
                    onValueChange = { newText ->
                        if (newText.length <= MAX_VALUE_LENGTH) text = newText
                    },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = ValueTextSize,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.End,
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                )
            } else {
                Text(
                    text = text,
                    fontSize = ValueTextSize,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    textAlign = TextAlign.End,
                )
            }
            Text(
                modifier = Modifier.padding(start = SpacingXSmall),
                text = CurrencyUtil.getCurrencySignBy(currency.code),
                fontSize = ValueTextSize,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(SpacingLarge))
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
