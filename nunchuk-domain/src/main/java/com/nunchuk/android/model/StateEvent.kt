package com.nunchuk.android.model

sealed interface StateEvent {
    data class String(val data: kotlin.String) : StateEvent
    data object None : StateEvent
}