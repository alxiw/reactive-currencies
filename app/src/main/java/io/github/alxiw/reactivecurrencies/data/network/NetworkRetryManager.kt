package io.github.alxiw.reactivecurrencies.data.network

import io.reactivex.rxjava3.subjects.PublishSubject

class NetworkRetryManager {

    private val retrySubject = PublishSubject.create<RetryEvent>()

    fun observeRetries(): PublishSubject<RetryEvent> {
        return retrySubject
    }

    fun retry() {
        retrySubject.onNext(RetryEvent())
    }

    class RetryEvent
}