package com.nunchuk.android.settings.about

import androidx.lifecycle.ViewModel
import com.nunchuk.android.core.provider.AppInfoProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AboutViewModel @Inject constructor(
    private val appInfoProvider: AppInfoProvider
) : ViewModel() {
    val version = appInfoProvider.getAppVersion()
}