package com.nunchuk.android.wallet.nav

import android.content.Context
import com.nunchuk.android.core.qr.DynamicQRCodeActivity
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.nav.WalletNavigator
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.wallet.add.AddWalletActivity
import com.nunchuk.android.wallet.assign.AssignSignerActivity
import com.nunchuk.android.wallet.backup.BackupWalletActivity
import com.nunchuk.android.wallet.config.WalletConfigActivity
import com.nunchuk.android.wallet.confirm.WalletConfirmActivity
import com.nunchuk.android.wallet.details.WalletDetailsActivity
import com.nunchuk.android.wallet.intro.WalletIntroActivity
import com.nunchuk.android.wallet.upload.UploadConfigurationActivity

interface WalletNavigatorDelegate : WalletNavigator {

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
        masterSigners: List<MasterSigner>,
        remoteSigners: List<SingleSigner>
    ) {
        WalletConfirmActivity.start(
            activityContext = activityContext,
            walletName = walletName,
            walletType = walletType,
            addressType = addressType,
            totalRequireSigns = totalRequireSigns,
            masterSigners = masterSigners,
            remoteSigners = remoteSigners
        )
    }

    override fun openBackupWalletScreen(activityContext: Context, walletId: String, descriptor: String) {
        BackupWalletActivity.start(activityContext = activityContext, walletId = walletId, descriptor = descriptor)
    }

    override fun openUploadConfigurationScreen(activityContext: Context, walletId: String) {
        UploadConfigurationActivity.start(activityContext, walletId)
    }

    override fun openWalletConfigScreen(activityContext: Context, walletId: String) {
        WalletConfigActivity.start(activityContext, walletId)
    }

    override fun openDynamicQRScreen(activityContext: Context, values: List<String>) {
        DynamicQRCodeActivity.start(activityContext, values)
    }

    override fun openWalletDetailsScreen(activityContext: Context, walletId: String) {
        WalletDetailsActivity.start(activityContext, walletId)
    }

    override fun openWalletIntroScreen(activityContext: Context) {
        WalletIntroActivity.start(activityContext)
    }

}