package io.github.alxiw.reactivecurrencies.presentation.currencies

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.alxiw.reactivecurrencies.domain.model.Currency
import io.github.alxiw.reactivecurrencies.presentation.R
import io.github.alxiw.reactivecurrencies.presentation.theme.CurrenciesTheme
import io.github.alxiw.reactivecurrencies.presentation.theme.SpacingLarge
import io.github.alxiw.reactivecurrencies.presentation.theme.TitleTextSize
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.math.BigDecimal

@Composable
fun CurrenciesScreen(
    viewModel: CurrenciesViewModel,
    onOpenConverter: () -> Unit,
    modifier: Modifier = Modifier,
) {

    var state by remember { mutableStateOf(CurrenciesUiState()) }

    val successLoadingFormat = stringResource(R.string.success_loading)
    val errorLoadingMessage = stringResource(R.string.error_loading)
    val retryLabel = stringResource(R.string.retry)
    val errorUpdatingMessage = stringResource(R.string.error_updating)

    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var scrollRestored by remember { mutableStateOf(false) }
    var snackbarJob by remember { mutableStateOf<Job?>(null) }

    fun showSnackBar(
        message: String,
        duration: SnackbarDuration,
        actionLabel: String? = null,
        onAction: () -> Unit = {},
    ) {
        snackbarJob?.cancel()
        snackbarJob = scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = actionLabel,
                duration = duration,
            )
            if (result == SnackbarResult.ActionPerformed) {
                onAction()
            }
        }
    }

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
                    is CurrenciesEvent.ShowLoadingSuccess -> showSnackBar(
                        message = String.format(successLoadingFormat, event.info),
                        duration = SnackbarDuration.Short,
                    )
                    is CurrenciesEvent.ShowLoadingError -> showSnackBar(
                        message = errorLoadingMessage,
                        duration = SnackbarDuration.Indefinite,
                        actionLabel = retryLabel,
                        onAction = { viewModel.submit(CurrenciesIntent.Retry) },
                    )
                    is CurrenciesEvent.ShowUpdatingError -> showSnackBar(
                        message = errorUpdatingMessage,
                        duration = SnackbarDuration.Short,
                    )
                    is CurrenciesEvent.ScrollToTop -> scope.launch {
                        if (state.showList) {
                            listState.animateScrollToItem(0)
                        }
                    }
                }
            }
        onDispose { subscription.dispose() }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                viewModel.saveScrollPosition(listState.firstVisibleItemIndex)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(state.showList, state.currencies) {
        if (state.showList && !scrollRestored) {
            scrollRestored = true
            viewModel.restoreScrollPosition()?.let { position ->
                listState.scrollToItem(position)
            }
        }
    }

    // Initial load. The ViewModel guards against duplicate loads on config change.
    LaunchedEffect(Unit) {
        viewModel.submit(CurrenciesIntent.LoadInitial)
    }

    CurrenciesScreenContent(
        state = state,
        listState = listState,
        snackbarHostState = snackbarHostState,
        onRefresh = {
            // Swipe-to-refresh is only enabled when the list or the stub is shown.
            if (state.showList || state.showStub) {
                snackbarJob?.cancel()
                viewModel.submit(CurrenciesIntent.Refresh)
            }
        },
        onItemClick = { currency ->
            viewModel.submit(CurrenciesIntent.SelectCurrency(currency))
        },
        onValueChanged = { currency, value ->
            viewModel.submit(CurrenciesIntent.ChangeValue(Currency(currency.code, value, isBase = true)))
        },
        onOpenConverter = onOpenConverter,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrenciesScreenContent(
    state: CurrenciesUiState,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onRefresh: () -> Unit = {},
    onItemClick: (Currency) -> Unit = {},
    onValueChanged: (Currency, BigDecimal) -> Unit = { _, _ -> },
    onOpenConverter: () -> Unit = {},
) {
    val pullToRefreshState = rememberPullToRefreshState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            if (state.showList) {
                FloatingActionButton(
                    onClick = onOpenConverter,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_swap),
                        contentDescription = stringResource(id = R.string.open_converter)
                    )
                }
            }
        },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize(),
            state = pullToRefreshState,
            indicator = {
                PullToRefreshDefaults.Indicator(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .windowInsetsPadding(WindowInsets.statusBars),
                    isRefreshing = state.isRefreshing,
                    state = pullToRefreshState,
                    containerColor = MaterialTheme.colorScheme.surface,
                    color = MaterialTheme.colorScheme.primary,
                )
            },
        ) {
            Box(Modifier.fillMaxSize()) {
                if (state.showList) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = innerPadding,
                    ) {
                        itemsIndexed(
                            items = state.currencies,
                        ) { index, currency ->
                            Column {
                                CurrencyCard(
                                    currency = currency,
                                    enableInput = currency.isBase,
                                    onItemClick = { onItemClick(currency) },
                                    onValueChanged = { value -> onValueChanged(currency, value) },
                                )
                                if (index < state.currencies.lastIndex) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                }
                            }
                        }
                    }
                }

                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                if (state.showStub) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_nothing_found),
                            contentDescription = null,
                        )
                        Spacer(Modifier.height(SpacingLarge))
                        Text(
                            text = stringResource(R.string.nothing_found),
                            fontSize = TitleTextSize,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .windowInsetsPadding(WindowInsets.safeDrawing),
                ) { data ->
                    Snackbar(
                        action = {
                            data.visuals.actionLabel?.let { label ->
                                TextButton(onClick = { data.performAction() }) {
                                    Text(text = label, color = MaterialTheme.colorScheme.inversePrimary)
                                }
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.inverseSurface,
                        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    ) {
                        Text(text = data.visuals.message)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CurrenciesScreenPreview() {
    CurrenciesTheme {
        CurrenciesScreenContent(
            state = CurrenciesUiState(
                isLoading = false,
                currencies = listOf(
                    Currency("USD", BigDecimal("1.00"), isBase = true),
                    Currency("EUR", BigDecimal("0.85")),
                    Currency("GBP", BigDecimal("0.75")),
                    Currency("RUB", BigDecimal("90.50")),
                ),
            ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CurrenciesScreenLoadingPreview() {
    CurrenciesTheme {
        CurrenciesScreenContent(
            state = CurrenciesUiState(
                isLoading = true,
            ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CurrenciesScreenStubPreview() {
    CurrenciesTheme {
        CurrenciesScreenContent(
            state = CurrenciesUiState(
                isLoading = false,
            ),
        )
    }
}
