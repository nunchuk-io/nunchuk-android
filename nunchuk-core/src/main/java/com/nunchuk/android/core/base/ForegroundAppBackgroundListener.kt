package com.nunchuk.android.core.base

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import timber.log.Timber

class ForegroundAppBackgroundListener(
    private val onResumeAppCallback: () -> Unit
) : DefaultLifecycleObserver {

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        Timber.d("onResume")
        onResumeAppCallback.invoke()
    }
}