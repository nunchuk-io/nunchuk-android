package com.nunchuk.android.arch.vm

import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

abstract class NunchukViewModel<State, Event> : ViewModel() {

    private val disposables = CompositeDisposable()

    private val _state = MutableLiveData<State>()

    // TODO investigate side effect
    private val _event = MutableLiveData<Event>()

    protected abstract val initialState: State

    val state: LiveData<State> get() = _state

    val event: LiveData<Event> get() = _event

    @MainThread
    protected fun updateState(updater: State.() -> State) {
        _state.value = updater(_state.value ?: initialState)
    }

    protected fun event(event: Event) {
        _event.postValue(event)
    }

    protected fun getState() = _state.value ?: initialState

    override fun onCleared() {
        disposables.dispose()
        super.onCleared()
    }

    protected fun Disposable.addToDisposables() {
        disposables.add(this)
    }

}