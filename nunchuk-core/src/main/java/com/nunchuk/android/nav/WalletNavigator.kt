package com.nunchuk.android.nav

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.RecoverWalletData
import com.nunchuk.android.model.RoomWalletData
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.WalletType

interface PersonalWalletNavigator {

    fun openAddWalletScreen(activityContext: Context)

    fun openWalletIntermediaryScreen(activityContext: Context, hasSigner: Boolean)

    fun openAddRecoverWalletScreen(activityContext: Context, data: RecoverWalletData)

    fun openRecoverWalletQRCodeScreen(activityContext: Context)

    fun openReviewWalletScreen(
        activityContext: Context,
        walletName: String,
        walletType: WalletType,
        addressType: AddressType,
        totalRequireSigns: Int,
        masterSigners: List<MasterSigner>,
        remoteSigners: List<SingleSigner>
    )
}

interface SharedWalletNavigator {

    fun openCreateSharedWalletScreen(activityContext: Context)

    fun openConfigureSharedWalletScreen(activityContext: Context, walletName: String, walletType: WalletType, addressType: AddressType)

    fun openReviewSharedWalletScreen(
        activityContext: Context,
        walletName: String,
        walletType: WalletType,
        addressType: AddressType,
        totalSigns: Int,
        requireSigns: Int,
        signers: List<SingleSigner>
    )

    fun openAssignSignerSharedWalletScreen(
        activityContext: Context,
        walletName: String,
        walletType: WalletType,
        addressType: AddressType,
        totalSigns: Int,
        requireSigns: Int,
        signers: List<SingleSigner>
    )

    fun openSharedWalletConfigScreen(
        activityContext: Context,
        roomWalletData: RoomWalletData
    )

    fun openRecoverSharedWalletScreen(
        activityContext: Context
    )

    fun openAddRecoverSharedWalletScreen(
        activityContext: Context,
        data: String
    )
}

interface WalletNavigator : PersonalWalletNavigator, SharedWalletNavigator {

    fun openConfigureWalletScreen(
        activityContext: Context,
        walletName: String,
        walletType: WalletType,
        addressType: AddressType
    )

    fun openBackupWalletScreen(activityContext: Context, walletId: String, totalRequireSigns: Int, isQuickWallet: Boolean = false)

    fun openUploadConfigurationScreen(activityContext: Context, walletId: String)

    fun openWalletConfigScreen(activityContext: Context, walletId: String)

    fun openWalletConfigScreen(launcher: ActivityResultLauncher<Intent>, activityContext: Context, walletId: String)

    fun openDynamicQRScreen(activityContext: Context, values: List<String>)

    fun openWalletDetailsScreen(activityContext: Context, walletId: String, shouldReloadPendingTx: Boolean = false)

    fun openWalletEmptySignerScreen(activityContext: Context)

    fun openTaprootWarningScreen(
        activityContext: Context,
        walletName: String,
        walletType: WalletType,
        addressType: AddressType
    )

    fun openQuickWalletScreen(launcher: ActivityResultLauncher<Intent>, activityContext: Context)
}