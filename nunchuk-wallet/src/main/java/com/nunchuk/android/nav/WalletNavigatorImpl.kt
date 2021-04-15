package com.nunchuk.android.nav

import android.content.Context
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.wallet.add.AddWalletActivity
import com.nunchuk.android.wallet.assign.AssignSignerActivity
import com.nunchuk.android.wallet.backup.BackupWalletActivity
import com.nunchuk.android.wallet.confirm.WalletConfirmActivity
import com.nunchuk.android.wallet.upload.UploadConfigurationActivity

open class WalletNavigatorImpl : WalletNavigator {

    override fun openAddWalletScreen(activityContext: Context) {
        AddWalletActivity.start(activityContext)
    }

    override fun openAssignSignerScreen(
        activityContext: Context,
        walletName: String,
        walletType: WalletType,
        addressType: AddressType
    ) {
        AssignSignerActivity.start(activityContext, walletName, walletType, addressType)
    }

    override fun openWalletConfirmScreen(
        activityContext: Context,
        walletName: String,
        walletType: WalletType,
        addressType: AddressType,
        totalRequireSigns: Int,
        signers: List<SingleSigner>
    ) {
        WalletConfirmActivity.start(
            activityContext = activityContext,
            walletName = walletName,
            walletType = walletType,
            addressType = addressType,
            totalRequireSigns = totalRequireSigns,
            signers = signers
        )
    }

    override fun openBackupWalletScreen(activityContext: Context, descriptor: String) {
        BackupWalletActivity.start(activityContext, descriptor)
    }

    override fun openUploadConfigurationScreen(activityContext: Context) {
        UploadConfigurationActivity.start(activityContext)
    }

}