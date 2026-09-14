package io.github.alxiw.reactivecurrencies.presentation.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.alxiw.reactivecurrencies.presentation.converter.ConverterScreen
import io.github.alxiw.reactivecurrencies.presentation.converter.ConverterViewModel
import io.github.alxiw.reactivecurrencies.presentation.currencies.CurrenciesScreen
import io.github.alxiw.reactivecurrencies.presentation.currencies.CurrenciesViewModel

/**
 * Every place the app can navigate to.
 *
 * [Currencies] is the root of the back stack and is always rendered. Destinations pushed on top of
 * the root are rendered as overlays (currently only the [Converter] modal bottom sheet), which keeps
 * an overlay's trigger (the FAB today) decoupled from the overlay itself.
 */
sealed interface Destination {

    /** The main list of currencies; the root of the back stack. */
    data object Currencies : Destination

    /** The currency converter, shown as a modal bottom sheet. */
    data object Converter : Destination

    /** Stable identifier used to persist the navigation state. */
    val key: String
        get() = when (this) {
            Currencies -> "currencies"
            Converter -> "converter"
        }

    companion object {
        fun fromKey(key: String): Destination? = when (key) {
            "currencies" -> Currencies
            "converter" -> Converter
            else -> null
        }
    }
}

/**
 * Holds the navigation back stack.
 *
 * Intentionally free of any third-party navigation library: it is just a snapshot-backed list of
 * [Destination]s that survives configuration changes through [rememberNavigator].
 */
@Stable
class Navigator internal constructor(initialDestination: Destination) {

    private val backStack = mutableStateListOf(initialDestination)

    /** Destinations rendered on top of the root, e.g. modal bottom sheets. */
    val overlays: List<Destination> get() = backStack.drop(1)

    /** `true` when there is something to pop. */
    val canNavigateUp: Boolean get() = backStack.size > 1

    /** Pushes [destination] onto the back stack (no-op when it is already on top). */
    fun navigateTo(destination: Destination) {
        if (backStack.last() != destination) {
            backStack.add(destination)
        }
    }

    /** Pops the top destination; returns `true` when the back stack actually changed. */
    fun navigateUp(): Boolean {
        if (!canNavigateUp) return false
        backStack.removeAt(backStack.lastIndex)
        return true
    }

    internal fun backStackKeys(): List<String> = backStack.map { it.key }

    companion object {
        fun fromBackStack(destinations: List<Destination>): Navigator =
            Navigator(destinations.firstOrNull() ?: Destination.Currencies).apply {
                destinations.drop(1).forEach(::navigateTo)
            }
    }
}

private val NavigatorSaver: Saver<Navigator, List<String>> = Saver(
    save = { navigator -> navigator.backStackKeys() },
    restore = { keys -> Navigator.fromBackStack(keys.mapNotNull(Destination::fromKey)) },
)

/** Remembers a [Navigator] across recompositions and configuration changes. */
@Composable
fun rememberNavigator(
    initialDestination: Destination = Destination.Currencies,
): Navigator = rememberSaveable(saver = NavigatorSaver) { Navigator(initialDestination) }

/**
 * Renders the current navigation state without any third-party navigation library.
 *
 * The root destination is always composed, and every destination pushed on top of it is rendered as
 * an overlay. As a result an overlay (e.g. the converter bottom sheet) can be opened from any
 * trigger, not just the currencies FAB.
 */
@Composable
fun AppNavHost(
    navigator: Navigator,
    viewModelFactory: ViewModelProvider.Factory,
    modifier: Modifier = Modifier,
) {
    val currenciesViewModel: CurrenciesViewModel = viewModel(factory = viewModelFactory)

    CurrenciesScreen(
        viewModel = currenciesViewModel,
        onOpenConverter = { navigator.navigateTo(Destination.Converter) },
        modifier = modifier,
    )

    navigator.overlays.forEach { destination ->
        key(destination.key) {
            when (destination) {
                Destination.Converter -> ConverterBottomSheet(
                    onDismissRequest = { navigator.navigateUp() },
                    viewModelFactory = viewModelFactory,
                )

                Destination.Currencies -> Unit // the root is never rendered as an overlay
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConverterBottomSheet(
    onDismissRequest: () -> Unit,
    viewModelFactory: ViewModelProvider.Factory,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val converterViewModel: ConverterViewModel = viewModel(factory = viewModelFactory)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.statusBarsPadding(),
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        ConverterScreen(viewModel = converterViewModel)
    }
}
