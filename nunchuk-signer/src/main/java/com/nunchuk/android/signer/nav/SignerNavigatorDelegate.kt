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

package com.nunchuk.android.signer.nav

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import com.nunchuk.android.core.domain.membership.WalletsExistingKey
import com.nunchuk.android.core.signer.KeyFlow
import com.nunchuk.android.model.MembershipStep
import com.nunchuk.android.model.PrimaryKey
import com.nunchuk.android.model.signer.SupportedSigner
import com.nunchuk.android.nav.SignerNavigator
import com.nunchuk.android.signer.SignerIntroActivity
import com.nunchuk.android.signer.components.add.AddAirgapSignerActivity
import com.nunchuk.android.signer.components.details.SignerInfoActivity
import com.nunchuk.android.signer.software.SoftwareSignerIntroActivity
import com.nunchuk.android.signer.software.components.confirm.ConfirmSeedActivity
import com.nunchuk.android.signer.software.components.create.CreateNewSeedActivity
import com.nunchuk.android.signer.software.components.name.AddSoftwareSignerNameActivity
import com.nunchuk.android.signer.software.components.passphrase.SetPassphraseActivity
import com.nunchuk.android.signer.software.components.primarykey.PKeyAddSignerActivity
import com.nunchuk.android.signer.software.components.primarykey.account.PKeyAccountActivity
import com.nunchuk.android.signer.software.components.primarykey.chooseusername.PKeyChooseUsernameActivity
import com.nunchuk.android.signer.software.components.primarykey.intro.PKeySignInIntroActivity
import com.nunchuk.android.signer.software.components.primarykey.intro.PKeySignUpIntroActivity
import com.nunchuk.android.signer.software.components.primarykey.intro.replace.PKeyReplaceKeyIntroActivity
import com.nunchuk.android.signer.software.components.primarykey.manuallysignature.PKeyManuallySignatureActivity
import com.nunchuk.android.signer.software.components.primarykey.manuallyusername.PKeyManuallyUsernameActivity
import com.nunchuk.android.signer.software.components.primarykey.passphrase.PKeyEnterPassphraseActivity
import com.nunchuk.android.signer.software.components.primarykey.signin.PKeySignInActivity
import com.nunchuk.android.signer.software.components.recover.RecoverSeedActivity
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.SignerType

interface SignerNavigatorDelegate : SignerNavigator {

    override fun openSignerIntroScreen(
        activityContext: Context,
        walletId: String,
        groupId: String?,
        index: Int,
        supportedSigners: List<SupportedSigner>?
    ) {
        SignerIntroActivity.start(
            activityContext = activityContext,
            walletId = walletId,
            groupId = groupId,
            index = index,
            supportedSigners = supportedSigners
        )
    }

    override fun openSignerInfoScreen(
        activityContext: Context,
        isMasterSigner: Boolean,
        id: String,
        masterFingerprint: String,
        name: String,
        type: SignerType,
        derivationPath: String,
        justAdded: Boolean,
        setPassphrase: Boolean,
        isReplacePrimaryKey: Boolean,
        customMessage: String,
        existingKey: WalletsExistingKey?
    ) {
        SignerInfoActivity.start(
            activityContext = activityContext,
            isMasterSigner = isMasterSigner,
            id = id,
            name = name,
            justAdded = justAdded,
            type = type,
            setPassphrase = setPassphrase,
            derivationPath = derivationPath,
            masterFingerprint = masterFingerprint,
            isReplacePrimaryKey = isReplacePrimaryKey,
            customMessage = customMessage,
            existingKey = existingKey
        )
    }

    override fun openAddAirSignerScreen(
        activityContext: Context,
        isMembershipFlow: Boolean,
        tag: SignerTag?,
        groupId: String,
        xfp: String?,
        newIndex: Int,
        replacedXfp: String?,
        walletId: String,
        step: MembershipStep?,
        requestedSignerIndex: Int,
    ) {
        activityContext.startActivity(
            AddAirgapSignerActivity.buildIntent(
                activityContext = activityContext,
                isMembershipFlow = isMembershipFlow,
                tag = tag,
                groupId = groupId,
                xfp = xfp,
                newIndex = newIndex,
                replacedXfp = replacedXfp,
                walletId = walletId,
                step = step,
                requestedSignerIndex = requestedSignerIndex
            )
        )
    }

    override fun openAddAirSignerScreenForResult(
        launcher: ActivityResultLauncher<Intent>,
        activityContext: Context,
        isMembershipFlow: Boolean,
        tag: SignerTag?,
        groupId: String,
        xfp: String?,
        newIndex: Int,
        replacedXfp: String?,
        walletId: String,
    ) {
        launcher.launch(
            AddAirgapSignerActivity.buildIntent(
                activityContext = activityContext,
                isMembershipFlow = isMembershipFlow,
                tag = tag,
                groupId = groupId,
                xfp = xfp,
                newIndex = newIndex,
                replacedXfp = replacedXfp,
                walletId = walletId
            )
        )
    }

    override fun openAddSoftwareSignerScreen(
        activityContext: Context,
        passphrase: String,
        keyFlow: Int,
        groupId: String?,
        replacedXfp: String?,
        walletId: String,
        index: Int,
    ) {
        SoftwareSignerIntroActivity.start(
            activityContext = activityContext,
            passphrase = passphrase,
            primaryKeyFlow = keyFlow,
            groupId = groupId,
            replacedXfp = replacedXfp,
            walletId = walletId,
            index = index
        )
    }

    override fun openCreateNewSeedScreen(
        activityContext: Context,
        passphrase: String,
        keyFlow: Int,
        walletId: String,
        groupId: String?,
        replacedXfp: String?,
        numberOfWords: Int,
        signerIndex: Int,
    ) {
        CreateNewSeedActivity.start(
            activityContext = activityContext,
            keyFlow = keyFlow,
            passphrase = passphrase,
            walletId = walletId,
            groupId = groupId,
            replacedXfp = replacedXfp,
            numberOfWords = numberOfWords,
            signerIndex = signerIndex
        )
    }

    override fun openRecoverSeedScreen(
        activityContext: Context,
        passphrase: String,
        keyFlow: Int,
        isRecoverHotWallet: Boolean,
        walletId: String,
        groupId: String?,
        replacedXfp: String?,
        signerIndex: Int,
    ) {
        RecoverSeedActivity.start(
            activityContext = activityContext,
            passphrase = passphrase,
            primaryKeyFlow = keyFlow,
            isRecoverHotWallet = isRecoverHotWallet,
            groupId = groupId,
            replacedXfp = replacedXfp,
            walletId = walletId
        )
    }

    override fun openSelectPhraseScreen(
        launcher: ActivityResultLauncher<Intent>,
        activityContext: Context,
        mnemonic: String,
        passphrase: String,
        keyFlow: Int,
        masterSignerId: String,
        walletId: String,
        groupId: String?,
        replacedXfp: String?,
        signerIndex: Int,
    ) {
        launcher.launch(
            ConfirmSeedActivity.buildIntent(
                activityContext = activityContext,
                mnemonic = mnemonic,
                passphrase = passphrase,
                primaryKeyFlow = keyFlow,
                masterSignerId = masterSignerId,
                walletId = walletId,
                groupId = groupId,
                replacedXfp = replacedXfp,
                signerIndex = signerIndex
            )
        )
    }

    override fun openAddSoftwareSignerNameScreen(
        activityContext: Context,
        mnemonic: String,
        keyFlow: Int,
        username: String?,
        passphrase: String,
        address: String?,
        walletId: String?,
        xprv: String?,
    ) {
        AddSoftwareSignerNameActivity.start(
            activityContext = activityContext,
            mnemonic = mnemonic,
            primaryKeyFlow = keyFlow,
            username = username,
            passphrase = passphrase,
            address = address,
            walletId = walletId,
            xprv = xprv
        )
    }

    override fun openSetPassphraseScreen(
        activityContext: Context,
        mnemonic: String,
        signerName: String,
        passphrase: String,
        @KeyFlow.PrimaryFlowInfo keyFlow: Int,
        groupId: String?,
        replacedXfp: String?,
        walletId: String,
        signerIndex: Int
    ) {
        SetPassphraseActivity.start(
            activityContext = activityContext,
            mnemonic = mnemonic,
            signerName = signerName,
            primaryKeyFlow = keyFlow,
            passphrase = passphrase,
            groupId = groupId,
            replacedXfp = replacedXfp,
            walletId = walletId,
            signerIndex = signerIndex
        )
    }

    override fun openPrimaryKeyIntroScreen(activityContext: Context) {
        PKeySignUpIntroActivity.start(activityContext)
    }

    override fun openAddPrimaryKeyScreen(
        activityContext: Context,
        passphrase: String,
        keyFlow: Int,
    ) {
        PKeyAddSignerActivity.start(activityContext, keyFlow, passphrase)
    }

    override fun openPrimaryKeyChooseUserNameScreen(
        activityContext: Context,
        mnemonic: String,
        passphrase: String,
        signerName: String,
    ) {
        PKeyChooseUsernameActivity.start(activityContext, mnemonic, passphrase, signerName)
    }

    override fun openPrimaryKeySignInIntroScreen(activityContext: Context) {
        PKeySignInIntroActivity.start(activityContext)
    }

    override fun openPrimaryKeyAccountScreen(
        activityContext: Context,
        accounts: ArrayList<PrimaryKey>,
    ) {
        PKeyAccountActivity.start(activityContext, accounts)
    }

    override fun openPrimaryKeySignInScreen(activityContext: Context, primaryKey: PrimaryKey) {
        PKeySignInActivity.start(activityContext, primaryKey)
    }

    override fun openPrimaryKeyEnterPassphraseScreen(
        activityContext: Context,
        mnemonic: String,
        keyFlow: Int,
        xprv: String?,
    ) {
        PKeyEnterPassphraseActivity.start(activityContext, keyFlow, mnemonic)
    }

    override fun openPrimaryKeyManuallyUsernameScreen(activityContext: Context) {
        PKeyManuallyUsernameActivity.start(activityContext)
    }

    override fun openPrimaryKeyManuallySignatureScreen(activityContext: Context, username: String) {
        PKeyManuallySignatureActivity.start(activityContext, username)
    }

    override fun openPrimaryKeyReplaceIntroScreen(activityContext: Context, keyFlow: Int) {
        PKeyReplaceKeyIntroActivity.start(activityContext)
    }
}