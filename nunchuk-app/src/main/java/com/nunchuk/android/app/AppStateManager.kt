package com.nunchuk.android.app

import android.app.Activity
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
    private val getWalletPinUseCase: GetWalletPinUseCase,
    private val getLastCloseAppUseCase: GetLastCloseAppUseCase,
    private val getBiometricConfigUseCase: GetBiometricConfigUseCase
) : DefaultLifecycleObserver {
    // null until loaded: on a cold start straight into a screen (e.g. from a notification)
    // onStart can run before storage is read, and must not mistake that for "no lock set".
    private val pin = getWalletPinUseCase(Unit)
        .map { it.getOrDefault("") }
        .stateIn(applicationScope, SharingStarted.Eagerly, null)

    private val biometricEnabled = getBiometricConfigUseCase(Unit)
        .map { it.getOrNull()?.enabled == true }
        .stateIn(applicationScope, SharingStarted.Eagerly, null)

    private val lastCloseApp = getLastCloseAppUseCase(Unit)
        .map { it.getOrDefault(0L) }
        .stateIn(applicationScope, SharingStarted.Eagerly, null)

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        AppEvenBus.instance.publish(AppEvent.AppResumedEvent)
        val startedAt = SystemClock.elapsedRealtime()
        Timber.d("lastCloseApp: ${lastCloseApp.value}")
        Timber.d("current time: $startedAt")

        // SplashActivity runs its own unlock check on a launcher start.
        ActivityManager.peek()?.takeIf { it !is SplashActivity }?.let { topActivity ->
            // Not loaded yet: read storage synchronously so the unlock screen is started before
            // the screen underneath draws its first frame. The use case flows run on IO.
            val pinValue = pin.value
                ?: runBlocking { getWalletPinUseCase(Unit).first().getOrDefault("") }
            val biometricValue = biometricEnabled.value
                ?: runBlocking { getBiometricConfigUseCase(Unit).first().getOrNull()?.enabled == true }
            val lastCloseValue = lastCloseApp.value
                ?: runBlocking { getLastCloseAppUseCase(Unit).first().getOrDefault(0L) }
            requireAuthIfNeeded(topActivity, startedAt, pinValue, biometricValue, lastCloseValue)
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

    private fun requireAuthIfNeeded(
        topActivity: Activity,
        startedAt: Long,
        pin: String,
        biometricEnabled: Boolean,
        lastCloseApp: Long,
    ) {
        // elapsedRealtime restarts at boot, so a stored close time ahead of now means a reboot.
        val shouldRequireAuth = startedAt - lastCloseApp !in 0..5.minutes.inWholeMilliseconds
        if (!shouldRequireAuth) return
        val mode = signInModeHolder.getCurrentMode()
        val isBiometricEnabled = biometricEnabled && mode.isGuestMode().not() && mode.isPrimaryKey().not()
        when {
            // PIN takes priority when both PIN and biometric are enabled
            pin.isNotEmpty() -> {
                topActivity.startActivity(Intent(topActivity, UnlockPinActivity::class.java))
            }
            // Show biometric if PIN is not set but biometric is enabled
            isBiometricEnabled -> {
                topActivity.startActivity(Intent(topActivity, BiometricActivity::class.java))
            }
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