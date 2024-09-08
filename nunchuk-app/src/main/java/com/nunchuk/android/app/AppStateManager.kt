package com.nunchuk.android.app

import android.content.Intent
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
import com.nunchuk.android.usecase.pin.GetLastCloseAppUseCase
import com.nunchuk.android.usecase.pin.SetLastCloseAppUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

class AppStateManager @Inject constructor(
    private val applicationScope: CoroutineScope,
    private val accountManager: AccountManager,
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val setLastCloseAppUseCase: SetLastCloseAppUseCase,
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
        if (pin.value.isNotEmpty() && System.currentTimeMillis() - lastCloseApp.value > 5.seconds.inWholeMilliseconds) {
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
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        applicationScope.launch {
            setLastCloseAppUseCase(System.currentTimeMillis())
        }
    }
}