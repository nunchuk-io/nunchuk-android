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
import com.nunchuk.android.core.signer.PrimaryKeyFlow
import com.nunchuk.android.model.PrimaryKey
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.SignerType

interface SignerNavigator {
    fun openSignerIntroScreen(activityContext: Context)

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
        isInWallet: Boolean = false,
        isInAssistedWallet: Boolean = false,
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
        newIndex : Int = -1,
        replacedXfp: String? = null,
        walletId: String = "",
    )

    fun openAddAirSignerScreenForResult(
        launcher: ActivityResultLauncher<Intent>,
        activityContext: Context,
        isMembershipFlow: Boolean,
        tag: SignerTag? = null,
        groupId: String = "",
        xfp: String? = null,
        newIndex : Int = -1,
        replacedXfp: String? = null,
        walletId: String = "",
    )

    /**
     * @param passphrase only need for replacing primary key
     */
    fun openAddSoftwareSignerScreen(
        activityContext: Context,
        passphrase: String = "",
        @PrimaryKeyFlow.PrimaryFlowInfo primaryKeyFlow: Int = PrimaryKeyFlow.NONE,
        groupId: String? = null,
        replacedXfp: String? = null,
        walletId: String = "",
    )

    /**
     * @param passphrase only need for replacing primary key
     */
    fun openCreateNewSeedScreen(
        activityContext: Context,
        passphrase: String = "",
        @PrimaryKeyFlow.PrimaryFlowInfo primaryKeyFlow: Int = PrimaryKeyFlow.NONE,
        walletId: String = "",
        groupId: String? = null,
        replacedXfp: String? = null,
    )

    fun openCreateNewSeedScreen(fragment: Fragment, isQuickWallet: Boolean = false)

    /**
     * @param passphrase only need for replacing primary key
     */
    fun openRecoverSeedScreen(
        activityContext: Context,
        passphrase: String = "",
        @PrimaryKeyFlow.PrimaryFlowInfo primaryKeyFlow: Int = PrimaryKeyFlow.NONE,
        isRecoverHotWallet: Boolean = false,
        walletId: String = "",
        groupId: String? = null,
        replacedXfp: String? = null,
    )

    /**
     * @param passphrase only need for replacing primary key
     */
    fun openSelectPhraseScreen(
        activityContext: Context,
        mnemonic: String,
        passphrase: String = "",
        @PrimaryKeyFlow.PrimaryFlowInfo primaryKeyFlow: Int = PrimaryKeyFlow.NONE,
        masterSignerId: String = "",
        walletId: String = "",
        groupId: String? = null,
        replacedXfp: String? = null,
    )

    /**
     * @param passphrase only need for replacing primary key
     * @param username only need for sign in (import) primary key
     * @param address only need for sign in (import) primary key
     */
    fun openAddSoftwareSignerNameScreen(
        activityContext: Context,
        mnemonic: String,
        @PrimaryKeyFlow.PrimaryFlowInfo primaryKeyFlow: Int = PrimaryKeyFlow.NONE,
        username: String? = null,
        passphrase: String = "",
        address: String? = null,
    )

    /**
     * @param passphrase only need for replacing primary key
     */
    fun openSetPassphraseScreen(
        activityContext: Context,
        mnemonic: String,
        signerName: String,
        passphrase: String = "",
        @PrimaryKeyFlow.PrimaryFlowInfo primaryKeyFlow: Int = PrimaryKeyFlow.NONE,
        groupId: String? = null,
        replacedXfp: String? = null,
        walletId: String = "",
    )

    fun openPrimaryKeyIntroScreen(activityContext: Context)

    fun openAddPrimaryKeyScreen(
        activityContext: Context,
        passphrase: String = "",
        @PrimaryKeyFlow.PrimaryFlowInfo primaryKeyFlow: Int = PrimaryKeyFlow.NONE,
    )

    fun openPrimaryKeyChooseUserNameScreen(
        activityContext: Context, mnemonic: String,
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
        mnemonic: String,
        @PrimaryKeyFlow.PrimaryFlowInfo primaryKeyFlow: Int = PrimaryKeyFlow.NONE,
    )

    fun openPrimaryKeyManuallyUsernameScreen(activityContext: Context)

    fun openPrimaryKeyManuallySignatureScreen(activityContext: Context, username: String)

    fun openPrimaryKeyReplaceIntroScreen(
        activityContext: Context,
        @PrimaryKeyFlow.PrimaryFlowInfo primaryKeyFlow: Int = PrimaryKeyFlow.NONE,
    )
}