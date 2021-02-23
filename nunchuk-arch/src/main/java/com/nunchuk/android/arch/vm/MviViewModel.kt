package com.nunchuk.android.arch.vm

import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

abstract class MviViewModel<State, Event> : ViewModel() {

    private val disposables = CompositeDisposable()

    private val stateMutable: MutableLiveData<State> = MutableLiveData()

    private val eventMutable: SingleLiveEvent<Event> = SingleLiveEvent()

    protected abstract val initialState: State

    val state: LiveData<State> = stateMutable

    val event: LiveData<Event> = eventMutable

    override fun onCleared() {
        disposables.dispose()
        super.onCleared()
    }

    @MainThread
    protected fun updateState(updater: State.() -> State) {
        stateMutable.value = updater(stateMutable.value ?: initialState)
    }

    @MainThread
    protected fun setState(state: State) {
        stateMutable.value = state
    }

    protected fun withState(stateConsumer: State.() -> Unit) {
        stateMutable.value?.let(stateConsumer)
    }

    protected fun event(event: Event) {
        eventMutable.value = event
    }

    protected fun Disposable.addToDisposables() {
        disposables.add(this)
    }

}