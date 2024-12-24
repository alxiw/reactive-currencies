package io.github.alxiw.reactivecurrencies.presentation.listeners

interface OnItemClickListener<in T> {
    fun onItemClick(item: T, position: Int)
}
