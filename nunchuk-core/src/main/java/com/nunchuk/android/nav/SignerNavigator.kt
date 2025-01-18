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
import androidx.fragment.app.Fragment
import com.nunchuk.android.core.domain.membership.WalletsExistingKey
import com.nunchuk.android.core.signer.KeyFlow
import com.nunchuk.android.model.MembershipStep
import com.nunchuk.android.model.PrimaryKey
import com.nunchuk.android.model.signer.SupportedSigner
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.SignerType

interface SignerNavigator {
    fun openSignerIntroScreen(
        activityContext: Context,
        walletId: String = "",
        groupId: String? = null,
        index: Int = -1,
        supportedSigners: List<SupportedSigner>? = null,
    )

    fun openSignerInfoScreen(
        activityContext: Context,
        isMasterSigner: Boolean,
        id: String,
        masterFingerprint: String,
        name: String,
        type: SignerType,
        derivationPath: String = "",
        justAdded: Boolean = false,
        setPassphrase: Boolean = false,
        isReplacePrimaryKey: Boolean = false,
        customMessage: String = "",
        existingKey: WalletsExistingKey? = null
    )

    fun openAddAirSignerScreen(
        activityContext: Context,
        isMembershipFlow: Boolean,
        tag: SignerTag? = null,
        groupId: String = "",
        xfp: String? = null,
        newIndex: Int = -1,
        replacedXfp: String? = null,
        walletId: String = "",
        step: MembershipStep? = null,
    )

    fun openAddAirSignerScreenForResult(
        launcher: ActivityResultLauncher<Intent>,
        activityContext: Context,
        isMembershipFlow: Boolean,
        tag: SignerTag? = null,
        groupId: String = "",
        xfp: String? = null,
        newIndex: Int = -1,
        replacedXfp: String? = null,
        walletId: String = "",
    )

    /**
     * @param passphrase only need for replacing primary key
     */
    fun openAddSoftwareSignerScreen(
        activityContext: Context,
        passphrase: String = "",
        @KeyFlow.PrimaryFlowInfo keyFlow: Int = KeyFlow.NONE,
        groupId: String? = null,
        replacedXfp: String? = null,
        walletId: String = "",
        index: Int = -1,
    )

    /**
     * @param passphrase only need for replacing primary key
     */
    fun openCreateNewSeedScreen(
        activityContext: Context,
        passphrase: String = "",
        @KeyFlow.PrimaryFlowInfo keyFlow: Int = KeyFlow.NONE,
        walletId: String = "",
        groupId: String? = null,
        replacedXfp: String? = null,
        numberOfWords: Int = 24,
        signerIndex: Int = -1,
    )

    fun openCreateNewSeedScreen(fragment: Fragment, isQuickWallet: Boolean = false)

    /**
     * @param passphrase only need for replacing primary key
     */
    fun openRecoverSeedScreen(
        activityContext: Context,
        passphrase: String = "",
        @KeyFlow.PrimaryFlowInfo keyFlow: Int = KeyFlow.NONE,
        isRecoverHotWallet: Boolean = false,
        walletId: String = "",
        groupId: String? = null,
        replacedXfp: String? = null,
        signerIndex: Int = -1,
    )

    /**
     * @param passphrase only need for replacing primary key
     */
    fun openSelectPhraseScreen(
        launcher: ActivityResultLauncher<Intent>,
        activityContext: Context,
        mnemonic: String,
        passphrase: String = "",
        @KeyFlow.PrimaryFlowInfo keyFlow: Int = KeyFlow.NONE,
        masterSignerId: String = "",
        walletId: String = "",
        groupId: String? = null,
        replacedXfp: String? = null,
        signerIndex: Int = -1,
    )

    /**
     * @param passphrase only need for replacing primary key
     * @param username only need for sign in (import) primary key
     * @param address only need for sign in (import) primary key
     */
    fun openAddSoftwareSignerNameScreen(
        activityContext: Context,
        mnemonic: String = "",
        @KeyFlow.PrimaryFlowInfo keyFlow: Int = KeyFlow.NONE,
        username: String? = null,
        passphrase: String = "",
        address: String? = null,
        walletId: String? = null,
        xprv: String? = null,
    )

    /**
     * @param passphrase only need for replacing primary key
     */
    fun openSetPassphraseScreen(
        activityContext: Context,
        mnemonic: String = "",
        signerName: String,
        passphrase: String = "",
        @KeyFlow.PrimaryFlowInfo keyFlow: Int = KeyFlow.NONE,
        groupId: String? = null,
        replacedXfp: String? = null,
        walletId: String = "",
        signerIndex: Int = -1,
    )

    fun openPrimaryKeyIntroScreen(activityContext: Context)

    fun openAddPrimaryKeyScreen(
        activityContext: Context,
        passphrase: String = "",
        @KeyFlow.PrimaryFlowInfo keyFlow: Int = KeyFlow.NONE,
    )

    fun openPrimaryKeyChooseUserNameScreen(
        activityContext: Context,
        mnemonic: String,
        passphrase: String,
        signerName: String,
    )

    fun openPrimaryKeySignInIntroScreen(activityContext: Context)

    fun openPrimaryKeyAccountScreen(activityContext: Context, accounts: ArrayList<PrimaryKey>)

    fun openPrimaryKeySignInScreen(
        activityContext: Context,
        primaryKey: PrimaryKey,
    )

    fun openPrimaryKeyEnterPassphraseScreen(
        activityContext: Context,
        mnemonic: String = "",
        @KeyFlow.PrimaryFlowInfo keyFlow: Int = KeyFlow.NONE,
        xprv: String? = null,
    )

    fun openPrimaryKeyManuallyUsernameScreen(activityContext: Context)

    fun openPrimaryKeyManuallySignatureScreen(activityContext: Context, username: String)

    fun openPrimaryKeyReplaceIntroScreen(
        activityContext: Context,
        @KeyFlow.PrimaryFlowInfo keyFlow: Int = KeyFlow.NONE,
    )

    fun openScanQrCodeScreen(activityContext: Context, isGroupWalletFlow: Boolean = false)
}