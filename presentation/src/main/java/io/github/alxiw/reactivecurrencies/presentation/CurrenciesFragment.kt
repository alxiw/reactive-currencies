package io.github.alxiw.reactivecurrencies.presentation

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import dev.androidbroadcast.vbpd.viewBinding
import io.github.alxiw.reactivecurrencies.presentation.di.AppContainer
import io.github.alxiw.reactivecurrencies.domain.model.Currency
import io.github.alxiw.reactivecurrencies.presentation.databinding.FragmentCurrenciesBinding
import io.github.alxiw.reactivecurrencies.presentation.currency.CurrenciesAdapter
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable

class CurrenciesFragment : Fragment(R.layout.fragment_currencies) {

    private val viewModel: CurrenciesViewModel by viewModels {
        (requireActivity().application as AppContainer).viewModelFactory
    }

    private val binding by viewBinding(FragmentCurrenciesBinding::bind)

    private val adapter = CurrenciesAdapter(
        onItemClick = { item, _ ->
            viewModel.submit(CurrenciesIntent.SelectCurrency(item))
        },
        onValueChanged = { item, value, _ ->
            viewModel.submit(CurrenciesIntent.ChangeValue(Currency(item.code, value, true)))
        }
    )

    private val disposables = CompositeDisposable()

    private var snackBar: Snackbar? = null

    private var scrollRestored = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupList()
        setupSystemBarsPadding()
        setupSwipeRefresh()

        disposables.add(
            viewModel.state
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(::render)
        )
        disposables.add(
            viewModel.events
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(::handleEvent)
        )

        viewModel.submit(CurrenciesIntent.LoadInitial)
    }

    private fun setupList() {
        binding.currenciesList.also {
            it.setHasFixedSize(true)
            val lm = LinearLayoutManager(context)
            it.layoutManager = lm
            it.addItemDecoration(DividerItemDecoration(activity, lm.orientation))
            it.itemAnimator = DefaultItemAnimator()
            it.adapter = adapter
        }
    }

    private fun setupSystemBarsPadding() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            binding.currenciesList.updatePadding(
                left = systemBars.left,
                top = systemBars.top,
                right = systemBars.right,
                bottom = systemBars.bottom
            )

            val density = resources.displayMetrics.density
            val defaultOffset = (SWIPE_REFRESH_OFFSET_DEFAULT * density).toInt()
            binding.currenciesSwipeRefresh.setProgressViewOffset(
                false,
                systemBars.top,
                systemBars.top + defaultOffset
            )

            insets
        }
    }

    private fun setupSwipeRefresh() {
        binding.currenciesSwipeRefresh.apply {
            setProgressBackgroundColorSchemeResource(
                R.color.swipeRefreshBackground
            )
            setColorSchemeResources(
                R.color.swipeRefreshProgressOne,
                R.color.swipeRefreshProgressTwo
            )
            setOnRefreshListener {
                snackBar?.dismiss()
                viewModel.submit(CurrenciesIntent.Refresh)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        (binding.currenciesList.layoutManager as? LinearLayoutManager)?.let { lm ->
            viewModel.saveScrollPosition(lm.findFirstVisibleItemPosition())
        }
    }

    override fun onDestroyView() {
        disposables.dispose()
        super.onDestroyView()
    }

    private fun render(state: CurrenciesUiState) {
        binding.currenciesProgressBar.isVisible = state.isLoading
        binding.currenciesList.isVisible = state.showList

        binding.currenciesSwipeRefresh.isEnabled = state.showList || state.showStub
        binding.currenciesSwipeRefresh.isRefreshing = state.isRefreshing
        if (state.isRefreshing) {
            snackBar?.dismiss()
        }

        if (state.showList) {
            adapter.updateCurrencies(state.currencies)
            if (!scrollRestored) {
                scrollRestored = true
                viewModel.restoreScrollPosition()?.let { position ->
                    binding.currenciesList.layoutManager?.scrollToPosition(position)
                }
            }
        }

        binding.currenciesStub.isVisible = state.showStub
    }

    private fun handleEvent(event: CurrenciesEvent) {
        when (event) {
            is CurrenciesEvent.ShowLoadingSuccess -> {
                showSnackBar(
                    anchor = binding.currenciesList,
                    message = String.format(getString(R.string.success_loading), event.info),
                    duration = Snackbar.LENGTH_SHORT
                )
            }
            is CurrenciesEvent.ShowLoadingError -> {
                showSnackBar(
                    anchor = binding.currenciesSwipeRefresh,
                    message = getString(R.string.error_loading),
                    duration = Snackbar.LENGTH_INDEFINITE,
                    actionText = getString(R.string.retry),
                    action = { viewModel.submit(CurrenciesIntent.Retry) }
                )
            }
            is CurrenciesEvent.ShowUpdatingError -> {
                showSnackBar(
                    anchor = binding.currenciesList,
                    message = getString(R.string.error_updating),
                    duration = Snackbar.LENGTH_SHORT
                )
            }
            is CurrenciesEvent.ScrollToTop -> {
                binding.currenciesList.smoothScrollToPosition(0)
            }
        }
    }

    private fun showSnackBar(
        anchor: View,
        message: String,
        duration: Int,
        actionText: String? = null,
        action: (() -> Unit)? = null
    ) {
        snackBar?.dismiss()
        snackBar = Snackbar.make(anchor, message, duration).apply {
            view.setBackgroundColor(
                ContextCompat.getColor(requireContext(), R.color.snackbarBackgroundColor)
            )
            if (actionText != null && action != null) {
                setAction(actionText) { action() }
            }
            show()
        }
    }

    companion object {
        private const val SWIPE_REFRESH_OFFSET_DEFAULT = 64

        @JvmStatic
        fun newInstance() = CurrenciesFragment()
    }
}
