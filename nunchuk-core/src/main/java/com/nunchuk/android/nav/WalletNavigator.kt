package com.nunchuk.android.nav

import android.content.Context
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.WalletType

interface PersonalWalletNavigator {

    fun openAddWalletScreen(activityContext: Context)

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
        requireSigns: Int
    )

}

interface WalletNavigator : PersonalWalletNavigator, SharedWalletNavigator {

    fun openAssignSignerScreen(
        activityContext: Context,
        walletName: String,
        walletType: WalletType,
        addressType: AddressType
    )

    fun openBackupWalletScreen(activityContext: Context, walletId: String, descriptor: String)

    fun openUploadConfigurationScreen(activityContext: Context, walletId: String)

    fun openWalletConfigScreen(activityContext: Context, walletId: String)

    fun openDynamicQRScreen(activityContext: Context, values: List<String>)

    fun openWalletDetailsScreen(activityContext: Context, walletId: String)

    fun openWalletEmptySignerScreen(activityContext: Context)
}