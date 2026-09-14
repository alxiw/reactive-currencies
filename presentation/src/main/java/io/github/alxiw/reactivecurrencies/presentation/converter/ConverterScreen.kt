package io.github.alxiw.reactivecurrencies.presentation.converter

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import io.github.alxiw.reactivecurrencies.presentation.R
import io.github.alxiw.reactivecurrencies.presentation.theme.ConverterContentPadding
import io.github.alxiw.reactivecurrencies.presentation.theme.ConverterFieldSpacing
import io.github.alxiw.reactivecurrencies.presentation.theme.ConverterFieldTextSize
import io.github.alxiw.reactivecurrencies.presentation.theme.ConverterSwapAnimationDuration
import io.github.alxiw.reactivecurrencies.presentation.theme.ConverterSwapButtonSize
import io.github.alxiw.reactivecurrencies.presentation.theme.ConverterSwapLoaderSize
import io.github.alxiw.reactivecurrencies.presentation.theme.ConverterSwapProgressStroke
import io.github.alxiw.reactivecurrencies.presentation.theme.CurrenciesTheme
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import kotlinx.coroutines.launch

@Composable
fun ConverterScreen(
    viewModel: ConverterViewModel,
    modifier: Modifier = Modifier,
) {

    var state by remember { mutableStateOf(ConverterUiState()) }

    val snackbarHostState = remember { SnackbarHostState() }
    val repeatLabel = stringResource(R.string.converter_repeat)
    val scope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        val subscription = viewModel.state
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { state = it }
        onDispose { subscription.dispose() }
    }

    DisposableEffect(Unit) {
        val subscription = viewModel.events
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { event ->
                when (event) {
                    is ConverterEvent.ShowError -> scope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = event.message,
                            actionLabel = repeatLabel,
                            duration = SnackbarDuration.Indefinite,
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            viewModel.submit(ConverterIntent.Retry)
                        }
                    }
                }
            }
        onDispose { subscription.dispose() }
    }

    // Initial load. The ViewModel guards against duplicate loads on config change.
    LaunchedEffect(Unit) {
        viewModel.submit(ConverterIntent.LoadInitial)
    }

    ConverterScreenContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onValueChange = { viewModel.submit(ConverterIntent.ChangeValue(it)) },
        onFromCurrencyChange = { viewModel.submit(ConverterIntent.SelectFrom(it)) },
        onToCurrencyChange = { viewModel.submit(ConverterIntent.SelectTo(it)) },
        onSwap = { viewModel.submit(ConverterIntent.Swap) },
        modifier = modifier,
    )
}

@Composable
fun ConverterScreenContent(
    state: ConverterUiState,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onValueChange: (TextFieldValue) -> Unit = {},
    onFromCurrencyChange: (String) -> Unit = {},
    onToCurrencyChange: (String) -> Unit = {},
    onSwap: () -> Unit = {},
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent,
        modifier = modifier.fillMaxSize(),
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            if (state.showContent) {
                ConverterContent(
                    state = state,
                    onValueChange = onValueChange,
                    onFromCurrencyChange = onFromCurrencyChange,
                    onToCurrencyChange = onToCurrencyChange,
                    onSwap = onSwap,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConverterContent(
    state: ConverterUiState,
    onValueChange: (TextFieldValue) -> Unit,
    onFromCurrencyChange: (String) -> Unit,
    onToCurrencyChange: (String) -> Unit,
    onSwap: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(ConverterContentPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = state.inputValue,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { FieldPlaceholder(stringResource(R.string.converter_hint)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            textStyle = LocalTextStyle.current.copy(fontSize = ConverterFieldTextSize, textAlign = TextAlign.Center)
        )

        Spacer(modifier = Modifier.height(ConverterFieldSpacing))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            CurrencySpinner(
                modifier = Modifier.weight(1f),
                selectedCurrency = state.selectedFrom,
                currencies = state.currencies,
                onCurrencySelected = onFromCurrencyChange
            )

            val rotation by animateFloatAsState(
                targetValue = if (state.isSwapped) -360f else 0f,
                animationSpec = tween(durationMillis = ConverterSwapAnimationDuration, easing = FastOutSlowInEasing),
                label = "swap_rotation"
            )

            val isLoading = state.conversion is ConversionState.Loading

            Box(
                modifier = Modifier
                    .size(ConverterSwapButtonSize)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false, radius = ConverterSwapButtonSize / 2)
                    ) {
                        onSwap()
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(ConverterSwapLoaderSize),
                        strokeWidth = ConverterSwapProgressStroke
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_right),
                        contentDescription = null,
                        modifier = Modifier
                            .size(ConverterSwapLoaderSize)
                            .graphicsLayer { rotationZ = rotation }
                    )
                }
            }

            CurrencySpinner(
                modifier = Modifier.weight(1f),
                selectedCurrency = state.selectedTo,
                currencies = state.currencies,
                onCurrencySelected = onToCurrencyChange
            )
        }

        Spacer(modifier = Modifier.height(ConverterFieldSpacing))

        val resultValue = when (val conversion = state.conversion) {
            is ConversionState.Result -> conversion.value
            else -> ""
        }

        OutlinedTextField(
            value = resultValue,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { FieldPlaceholder(stringResource(R.string.converter_result)) },
            textStyle = LocalTextStyle.current.copy(fontSize = ConverterFieldTextSize, textAlign = TextAlign.Center),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencySpinner(
    modifier: Modifier = Modifier,
    selectedCurrency: String,
    currencies: List<Triple<String, String, String>>,
    onCurrencySelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedCurrency,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            currencies.forEach { (code, flag, name) ->
                DropdownMenuItem(
                    text = { Text(text = "$flag $code • $name") },
                    onClick = {
                        onCurrencySelected(code)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun FieldPlaceholder(text: String) {
    Text(
        text = text,
        fontSize = ConverterFieldTextSize,
        modifier = Modifier.fillMaxWidth()
    )
}

@Preview(showBackground = true)
@Composable
fun ConverterContentPreview() {
    CurrenciesTheme {
        ConverterContent(
            state = ConverterUiState(
                isLoading = false,
                currencies = listOf(
                    Triple("USD", "\uD83C\uDDFA\uD83C\uDDF8", "US Dollar"),
                    Triple("EUR", "\uD83C\uDDEA\uD83C\uDDFA", "Euro"),
                    Triple("GBP", "\uD83C\uDDEC\uD83C\uDDE7", "British Pound")
                ),
                selectedFrom = "USD",
                selectedTo = "EUR",
                inputValue = TextFieldValue("100"),
                conversion = ConversionState.Result("0.92")
            ),
            onValueChange = {},
            onFromCurrencyChange = {},
            onToCurrencyChange = {},
            onSwap = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ConverterScreenLoadingPreview() {
    CurrenciesTheme {
        ConverterScreenContent(
            state = ConverterUiState(
                isLoading = true
            ),
            onValueChange = {},
            onFromCurrencyChange = {},
            onToCurrencyChange = {},
            onSwap = {}
        )
    }
}
