package com.nunchuk.android.nav

import android.content.Context
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.WalletType

interface WalletNavigator {

    fun openAddWalletScreen(activityContext: Context)

    fun openAssignSignerScreen(
        activityContext: Context,
        walletName: String,
        walletType: WalletType,
        addressType: AddressType
    )

    fun openWalletConfirmScreen(
        activityContext: Context,
        walletName: String,
        walletType: WalletType,
        addressType: AddressType,
        totalRequireSigns: Int,
        masterSigners: List<MasterSigner>,
        remoteSigners: List<SingleSigner>
    )

    fun openBackupWalletScreen(activityContext: Context, walletId: String, descriptor: String)

    fun openUploadConfigurationScreen(activityContext: Context, walletId: String)

    fun openWalletReviewScreen(activityContext: Context, walletId: String)

    fun openDynamicQRScreen(activityContext: Context, values: List<String>)
}