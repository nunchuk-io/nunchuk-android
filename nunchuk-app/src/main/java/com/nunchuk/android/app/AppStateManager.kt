package com.nunchuk.android.app

import android.content.Intent
import android.os.SystemClock
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.nunchuk.android.app.splash.SplashActivity
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.domain.GetWalletPinUseCase
import com.nunchuk.android.core.guestmode.SignInModeHolder
import com.nunchuk.android.core.guestmode.isGuestMode
import com.nunchuk.android.core.guestmode.isPrimaryKey
import com.nunchuk.android.core.manager.ActivityManager
import com.nunchuk.android.core.profile.GetUserProfileUseCase
import com.nunchuk.android.core.util.AppEvenBus
import com.nunchuk.android.core.util.AppEvent
import com.nunchuk.android.settings.walletsecurity.biometric.BiometricActivity
import com.nunchuk.android.settings.walletsecurity.unlock.UnlockPinActivity
import com.nunchuk.android.share.StartConsumeGroupWalletEventUseCase
import com.nunchuk.android.usecase.GetBiometricConfigUseCase
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
    private val signInModeHolder: SignInModeHolder,
    getWalletPinUseCase: GetWalletPinUseCase,
    getLastCloseAppUseCase: GetLastCloseAppUseCase,
    getBiometricConfigUseCase: GetBiometricConfigUseCase
) : DefaultLifecycleObserver {
    private val pin = getWalletPinUseCase(Unit)
        .map { it.getOrDefault("") }
        .stateIn(applicationScope, SharingStarted.Eagerly, "")

    private val biometricConfig = getBiometricConfigUseCase(Unit)
        .map { it.getOrNull() }
        .stateIn(applicationScope, SharingStarted.Eagerly, null)

    private val lastCloseApp = getLastCloseAppUseCase(Unit)
        .map { it.getOrDefault(0L) }
        .stateIn(applicationScope, SharingStarted.Eagerly, 0L)

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        AppEvenBus.instance.publish(AppEvent.AppResumedEvent)
        Timber.d("lastCloseApp: ${lastCloseApp.value}")
        Timber.d("current time: ${SystemClock.elapsedRealtime()}")
        val mode = signInModeHolder.getCurrentMode()
        val isBiometricEnabled = biometricConfig.value?.enabled == true && mode.isGuestMode().not() && mode.isPrimaryKey().not()

        val shouldRequireAuth = SystemClock.elapsedRealtime() - lastCloseApp.value > 5.minutes.inWholeMilliseconds

        if (shouldRequireAuth) {
            ActivityManager.peek()?.let { topActivity ->
                if (topActivity !is SplashActivity) {
                    when {
                        // PIN takes priority when both PIN and biometric are enabled
                        pin.value.isNotEmpty() -> {
                            topActivity.startActivity(Intent(topActivity, UnlockPinActivity::class.java))
                        }
                        // Show biometric if PIN is not set but biometric is enabled
                        isBiometricEnabled -> {
                            topActivity.startActivity(Intent(topActivity, BiometricActivity::class.java))
                        }
                    }
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