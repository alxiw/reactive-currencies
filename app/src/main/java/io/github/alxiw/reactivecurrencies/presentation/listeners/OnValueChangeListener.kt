package io.github.alxiw.reactivecurrencies.presentation.listeners

interface OnValueChangeListener<in T, in V> {
    fun onValueChanged(item: T, value: V, position: Int)
}
