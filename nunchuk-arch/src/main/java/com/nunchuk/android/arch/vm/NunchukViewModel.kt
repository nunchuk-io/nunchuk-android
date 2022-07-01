package com.nunchuk.android.arch.vm

import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.utils.onException
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

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

    @WorkerThread
    protected fun postState(updater: State.() -> State) {
        _state.postValue(updater(_state.value ?: initialState))
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

    protected fun sendErrorEvent(roomId: String, t: Throwable, executable: (String, Throwable) -> Flow<Unit>) {
        viewModelScope.launch {
            executable(roomId, t)
                .flowOn(Dispatchers.IO)
                .onException { }
                .collect { }
        }
    }

}