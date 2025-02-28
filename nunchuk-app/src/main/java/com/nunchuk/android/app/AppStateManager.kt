package com.nunchuk.android.app

import android.content.Intent
import android.os.SystemClock
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.nunchuk.android.app.splash.SplashActivity
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.domain.GetWalletPinUseCase
import com.nunchuk.android.core.manager.ActivityManager
import com.nunchuk.android.core.profile.GetUserProfileUseCase
import com.nunchuk.android.core.util.AppEvenBus
import com.nunchuk.android.core.util.AppEvent
import com.nunchuk.android.settings.walletsecurity.unlock.UnlockPinActivity
import com.nunchuk.android.share.StartConsumeGroupWalletEventUseCase
import com.nunchuk.android.usecase.pin.GetLastCloseAppUseCase
import com.nunchuk.android.usecase.pin.SetLastCloseAppUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes

class AppStateManager @Inject constructor(
    private val applicationScope: CoroutineScope,
    private val accountManager: AccountManager,
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val setLastCloseAppUseCase: SetLastCloseAppUseCase,
    private val startConsumeGroupWalletEventUseCase: StartConsumeGroupWalletEventUseCase,
    getWalletPinUseCase: GetWalletPinUseCase,
    getLastCloseAppUseCase: GetLastCloseAppUseCase
) : DefaultLifecycleObserver {
    private val pin = getWalletPinUseCase(Unit)
        .map { it.getOrDefault("") }
        .stateIn(applicationScope, SharingStarted.Eagerly, "")

    private val lastCloseApp = getLastCloseAppUseCase(Unit)
        .map { it.getOrDefault(0L) }
        .stateIn(applicationScope, SharingStarted.Eagerly, 0L)

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        AppEvenBus.instance.publish(AppEvent.AppResumedEvent)
        Timber.d("lastCloseApp: ${lastCloseApp.value}")
        Timber.d("current time: ${SystemClock.elapsedRealtime()}")
        if (pin.value.isNotEmpty() && SystemClock.elapsedRealtime() - lastCloseApp.value > 5.minutes.inWholeMilliseconds) {
            ActivityManager.peek()?.let { topActivity ->
                if (topActivity !is SplashActivity) {
                    // ignore splash activity
                    topActivity.startActivity(Intent(topActivity, UnlockPinActivity::class.java))
                }
            }
        }
        applicationScope.launch {
            if (accountManager.getAccount().token.isNotEmpty()) {
                getUserProfileUseCase(Unit)
            }
        }
        applicationScope.launch {
            startConsumeGroupWalletEventUseCase(Unit)
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        applicationScope.launch {
            val time = SystemClock.elapsedRealtime()
            Timber.d("setLastCloseAppUseCase: $time")
            setLastCloseAppUseCase(time)
        }
    }
}