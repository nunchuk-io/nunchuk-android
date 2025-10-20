/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2022, 2023 Nunchuk                                       *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

package com.nunchuk.android.nav

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import com.nunchuk.android.core.data.model.QuickWalletParam
import com.nunchuk.android.model.KeyPolicy
import com.nunchuk.android.model.RecoverWalletData
import com.nunchuk.android.model.RoomWalletData
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.nav.args.BackUpWalletArgs
import com.nunchuk.android.nav.args.AddWalletArgs
import com.nunchuk.android.nav.args.ConfigureWalletArgs
import com.nunchuk.android.nav.args.ReviewWalletArgs
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.WalletType

interface PersonalWalletNavigator {

    fun openAddWalletScreen(
        activityContext: Context,
        launcher: ActivityResultLauncher<Intent>? = null,
        args: AddWalletArgs
    )

    fun openWalletIntermediaryScreen(
        activityContext: Context,
        isHideAddKeyDialog: Boolean = false,
        quickWalletParam: QuickWalletParam? = null
    )

    fun openAddRecoverWalletScreen(
        activityContext: Context,
        data: RecoverWalletData,
        quickWalletParam: QuickWalletParam?
    )

    fun openRecoverWalletQRCodeScreen(
        activityContext: Context,
        launcher: ActivityResultLauncher<Intent>? = null,
        isCollaborativeWallet: Boolean = false,
        isGroupWallet: Boolean = false,
        isParseOnly: Boolean = false,
        quickWalletParam: QuickWalletParam? = null
    )

    fun openReviewWalletScreen(
        activityContext: Context,
        args: ReviewWalletArgs,
    )
}

interface SharedWalletNavigator {

    fun openCreateSharedWalletScreen(activityContext: Context)

    fun openConfigureSharedWalletScreen(
        activityContext: Context,
        walletName: String,
        walletType: WalletType,
        addressType: AddressType,
    )

    fun openReviewSharedWalletScreen(
        activityContext: Context,
        walletName: String,
        walletType: WalletType,
        addressType: AddressType,
        totalSigns: Int,
        requireSigns: Int,
        signers: List<SingleSigner>,
    )

    fun openAssignSignerSharedWalletScreen(
        activityContext: Context,
        walletName: String,
        walletType: WalletType,
        addressType: AddressType,
        totalSigns: Int,
        requireSigns: Int,
        signers: List<SingleSigner>,
    )

    fun openSharedWalletConfigScreen(
        activityContext: Context,
        roomWalletData: RoomWalletData,
    )

    fun openRecoverSharedWalletScreen(
        activityContext: Context,
    )

    fun openAddRecoverSharedWalletScreen(
        activityContext: Context,
        wallet: Wallet,
    )
}

interface WalletNavigator : PersonalWalletNavigator, SharedWalletNavigator {

    fun openConfigureWalletScreen(
        activityContext: Context,
        args: ConfigureWalletArgs,
    )

    fun openBackupWalletScreen(
        activityContext: Context,
        args: BackUpWalletArgs,
    )

    fun buildBackupWalletIntent(
        activityContext: Context,
        args: BackUpWalletArgs,
    ) : Intent

    fun openUploadConfigurationScreen(activityContext: Context, walletId: String, isOnChainFlow: Boolean = false)

    fun openWalletConfigScreen(
        activityContext: Context,
        walletId: String,
        keyPolicy: KeyPolicy? = null,
    )

    fun openWalletConfigScreen(
        launcher: ActivityResultLauncher<Intent>,
        activityContext: Context,
        walletId: String,
        keyPolicy: KeyPolicy? = null,
    )

    fun openDynamicQRScreen(
        activityContext: Context,
        launcher: ActivityResultLauncher<Intent>,
        walletId: String,
        qrCodeType: Int,
    )

    fun openWalletDetailsScreen(activityContext: Context, walletId: String)

    fun openWalletEmptySignerScreen(activityContext: Context)

    fun openTaprootScreen(
        activityContext: Context,
        walletName: String,
        walletType: WalletType,
        addressType: AddressType,
        decoyPin: String = "",
        groupSandboxId: String = "",
        quickWalletParam: QuickWalletParam?
    )

    fun openQuickWalletScreen(activityContext: Context, quickWalletParam: QuickWalletParam?)

    fun openCoinList(
        launcher: ActivityResultLauncher<Intent>? = null,
        context: Context,
        walletId: String,
        txId: String = "",
        inputs: List<UnspentOutput> = emptyList(),
        amount: Double = 0.0,
    )

    fun openCoinDetail(
        launcher: ActivityResultLauncher<Intent>? = null,
        context: Context,
        walletId: String,
        output: UnspentOutput,
    )

    fun openSearchTransaction(
        context: Context,
        walletId: String, roomId: String,
    )
}