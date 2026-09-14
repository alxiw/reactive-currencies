package io.github.alxiw.reactivecurrencies.presentation

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
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import kotlinx.coroutines.launch

@Composable
fun ConverterScreen(viewModel: ConverterViewModel) {

    val uiState by viewModel.state.collectAsStateWithLifecycleRx(initial = ConverterUiState())
    val snackBarHostState = remember { SnackbarHostState() }
    val repeatLabel = stringResource(R.string.converter_repeat)
    val scope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        viewModel.initData()
        onDispose { }
    }

    DisposableEffect(viewModel.events) {
        val disposable = viewModel.events.subscribe { message ->
            scope.launch {
                val result = snackBarHostState.showSnackbar(
                    message = message,
                    actionLabel = repeatLabel,
                    duration = SnackbarDuration.Indefinite
                )
                if (result == SnackbarResult.ActionPerformed) {
                    viewModel.initData()
                }
            }
        }
        onDispose { disposable.dispose() }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackBarHostState) },
        modifier = Modifier.fillMaxSize()
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (uiState.currenciesState) {
                is CurrenciesState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(64.dp))
                    }
                }
                is CurrenciesState.Ready -> {
                    ConverterContent(
                        uiState = uiState,
                        currenciesState = uiState.currenciesState as CurrenciesState.Ready,
                        onValueChange = viewModel::updateValue,
                        onFromCurrencyChange = viewModel::updateFromCurrency,
                        onToCurrencyChange = viewModel::updateToCurrency,
                        onSwap = viewModel::swapCurrencies
                    )
                }
            }
        }
    }
}

@Composable
private fun <T : Any> Observable<T>.collectAsStateWithLifecycleRx(initial: T): State<T> {
    val lifecycleOwner = LocalLifecycleOwner.current
    val state = remember { mutableStateOf(initial) }
    DisposableEffect(lifecycleOwner, this) {
        val disposables = CompositeDisposable()
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> disposables.add(
                    observeOn(AndroidSchedulers.mainThread()).subscribe { state.value = it }
                )
                Lifecycle.Event.ON_STOP -> disposables.clear()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            disposables.dispose()
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    return state
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConverterContent(
    uiState: ConverterUiState,
    currenciesState: CurrenciesState.Ready,
    onValueChange: (TextFieldValue) -> Unit,
    onFromCurrencyChange: (String) -> Unit,
    onToCurrencyChange: (String) -> Unit,
    onSwap: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = uiState.inputValue,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { FieldPlaceholder(stringResource(R.string.converter_hint)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            textStyle = LocalTextStyle.current.copy(fontSize = 24.sp, textAlign = TextAlign.Center)
        )

        Spacer(modifier = Modifier.height(36.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            CurrencySpinner(
                modifier = Modifier.weight(1f),
                selectedCurrency = uiState.selectedFrom,
                currencies = currenciesState.list,
                onCurrencySelected = onFromCurrencyChange
            )

            val rotation by animateFloatAsState(
                targetValue = if (uiState.isSwapped) -360f else 0f,
                animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
                label = "swap_rotation"
            )

            val isLoading = uiState.conversionState is ConversionState.Loading

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false, radius = 24.dp)
                    ) {
                        onSwap()
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_right),
                        contentDescription = null,
                        modifier = Modifier
                            .size(32.dp)
                            .graphicsLayer { rotationZ = rotation }
                    )
                }
            }

            CurrencySpinner(
                modifier = Modifier.weight(1f),
                selectedCurrency = uiState.selectedTo,
                currencies = currenciesState.list,
                onCurrencySelected = onToCurrencyChange
            )
        }

        Spacer(modifier = Modifier.height(36.dp))

        val resultValue = when (val state = uiState.conversionState) {
            is ConversionState.Result -> state.value
            else -> ""
        }

        OutlinedTextField(
            value = resultValue,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { FieldPlaceholder(stringResource(R.string.converter_result)) },
            textStyle = LocalTextStyle.current.copy(fontSize = 24.sp, textAlign = TextAlign.Center),
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
        fontSize = 24.sp,
        modifier = Modifier.fillMaxWidth()
    )
}

@Preview(showBackground = true)
@Composable
fun ConverterContentPreview() {
    MaterialTheme {
        val currenciesState = CurrenciesState.Ready(
            list = listOf(
                Triple("USD", "", "US Dollar"),
                Triple("EUR", "", "Euro"),
                Triple("GBP", "", "British Pound")
            )
        )
        val conversionState = ConversionState.Result("0.92")
        ConverterContent(
            uiState = ConverterUiState(
                currenciesState = currenciesState,
                conversionState = conversionState,
                selectedFrom = "USD",
                selectedTo = "EUR",
                inputValue = TextFieldValue("100")
            ),
            currenciesState = currenciesState,
            onValueChange = {},
            onFromCurrencyChange = {},
            onToCurrencyChange = {},
            onSwap = {}
        )
    }
}
