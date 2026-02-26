package com.nunchuk.android.settings.walletsecurity

import androidx.compose.runtime.Composable
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.nunchuk.android.core.domain.membership.PasswordVerificationHelper
import com.nunchuk.android.core.guestmode.SignInModeHolder
import com.nunchuk.android.core.wallet.WalletSecurityArgs
import com.nunchuk.android.core.wallet.WalletSecurityType
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.settings.walletsecurity.createpin.navigateToWalletSecurityCreatePin
import com.nunchuk.android.settings.walletsecurity.createpin.walletSecurityCreatePinScreen
import com.nunchuk.android.settings.walletsecurity.decoy.decoyPinNoteScreen
import com.nunchuk.android.settings.walletsecurity.decoy.decoyPinScreen
import com.nunchuk.android.settings.walletsecurity.decoy.decoyWalletCreateScreen
import com.nunchuk.android.settings.walletsecurity.DecoyWalletCreateRoute
import com.nunchuk.android.settings.walletsecurity.decoy.decoyWalletIntroScreen
import com.nunchuk.android.settings.walletsecurity.decoy.decoyWalletSuccessScreen
import com.nunchuk.android.settings.walletsecurity.decoy.navigateToDecoyPin
import com.nunchuk.android.settings.walletsecurity.decoy.navigateToDecoyPinNote
import com.nunchuk.android.settings.walletsecurity.decoy.navigateToDecoyWalletSuccess
import com.nunchuk.android.settings.walletsecurity.pin.navigateToPinStatus
import com.nunchuk.android.settings.walletsecurity.pin.pinStatusScreen
import com.nunchuk.android.settings.walletsecurity.unlock.navigateToUnlockPin
import com.nunchuk.android.settings.walletsecurity.unlock.unlockPinScreen

@Composable
internal fun WalletSecuritySettingNavHost(
    args: WalletSecurityArgs,
    activity: FragmentActivity,
    navigator: NunchukNavigator,
    signInModeHolder: SignInModeHolder,
    passwordVerificationHelper: PasswordVerificationHelper,
) {
    val navController = rememberNavController()
    val startDestination = when (args.type) {
        WalletSecurityType.CREATE_DECOY_WALLET -> DecoyWalletIntroRoute
        WalletSecurityType.CREATE_DECOY_SUCCESS -> DecoyWalletSuccessRoute
        WalletSecurityType.CREATE_PIN -> WalletSecuritySettingRoute
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        walletSecuritySettingScreen(
            activity = activity,
            signInModeHolder = signInModeHolder,
            passwordVerificationHelper = passwordVerificationHelper,
            onBack = activity::finish,
            onOpenPinStatus = {
                navController.navigateToPinStatus()
            },
        )

        pinStatusScreen(
            activity = activity,
            onOpenCreatePin = { isEnable ->
                navController.navigateToWalletSecurityCreatePin(isEnable)
            },
            onOpenUnlockPin = {
                navController.navigateToUnlockPin(isRemovePin = true)
            },
        )

        walletSecurityCreatePinScreen(
            activity = activity,
            activityType = args.type,
            onNavigateBack = {
                navController.popBackStack()
            },
            onNavigateToDecoyPin = {
                navController.popBackStack()
                navController.navigateToDecoyPin()
            },
        )

        unlockPinScreen(
            activity = activity,
            navigator = navigator,
            onPinRemoved = {
                navController.popBackStack()
            },
        )

        decoyWalletIntroScreen(
            activity = activity,
            onOpenDecoyPin = {
                navController.navigateToDecoyPin()
            },
            onOpenCreatePin = {
                navController.navigateToWalletSecurityCreatePin(false)
            },
        )

        decoyPinScreen(
            activity = activity,
            navigator = navigator,
            quickWalletParam = args.quickWalletParam,
            onOpenDecoyWalletCreate = { decoyPin ->
                navController.navigate(DecoyWalletCreateRoute(decoyPin = decoyPin))
            },
        )

        decoyWalletCreateScreen(
            activity = activity,
            fragmentManager = activity.supportFragmentManager,
            navigator = navigator,
            quickWalletParam = args.quickWalletParam,
            onNavigateToSuccess = {
                navController.navigateToDecoyWalletSuccess()
            },
        )

        decoyWalletSuccessScreen(
            onContinueClick = {
                navController.navigateToDecoyPinNote()
            },
        )

        decoyPinNoteScreen(
            activity = activity,
            signInModeHolder = signInModeHolder,
            navigator = navigator,
            quickWalletParam = args.quickWalletParam,
        )
    }
}
