package com.nunchuk.android.core.base

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

class ForegroundAppBackgroundListener(
    private val onResumeAppCallback: () -> Unit
) : DefaultLifecycleObserver {

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        onResumeAppCallback.invoke()
    }
}