package com.nunchuk.android.core.util

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner

fun LifecycleOwner.isAtLeastStarted() = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
