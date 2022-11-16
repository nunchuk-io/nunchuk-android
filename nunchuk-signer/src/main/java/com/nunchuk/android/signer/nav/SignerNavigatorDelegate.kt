/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *							          *
 * Copyright (C) 2022 Nunchuk								              *
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
import com.nunchuk.android.model.PrimaryKey
import com.nunchuk.android.nav.SignerNavigator
import com.nunchuk.android.signer.AirSignerIntroActivity
import com.nunchuk.android.signer.SignerIntroActivity
import com.nunchuk.android.signer.components.add.AddSignerActivity
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
import com.nunchuk.android.signer.software.components.primarykey.notification.PKeyNotificationActivity
import com.nunchuk.android.signer.software.components.primarykey.passphrase.PKeyEnterPassphraseActivity
import com.nunchuk.android.signer.software.components.primarykey.signin.PKeySignInActivity
import com.nunchuk.android.signer.software.components.recover.RecoverSeedActivity
import com.nunchuk.android.type.SignerType

interface SignerNavigatorDelegate : SignerNavigator {

    override fun openSignerIntroScreen(activityContext: Context) {
        SignerIntroActivity.start(activityContext)
    }

    override fun openSignerInfoScreen(
        activityContext: Context,
        id: String,
        masterFingerprint: String,
        name: String,
        type: SignerType,
        derivationPath: String,
        justAdded: Boolean,
        setPassphrase: Boolean,
        isInWallet: Boolean,
        isReplacePrimaryKey: Boolean
    ) {
        SignerInfoActivity.start(
            activityContext = activityContext,
            id = id,
            name = name,
            justAdded = justAdded,
            type = type,
            setPassphrase = setPassphrase,
            isInWallet = isInWallet,
            derivationPath = derivationPath,
            masterFingerprint = masterFingerprint,
            isReplacePrimaryKey = isReplacePrimaryKey
        )
    }

    override fun openAddAirSignerIntroScreen(activityContext: Context) {
        AirSignerIntroActivity.start(activityContext)
    }

    override fun openAddAirSignerScreen(activityContext: Context) {
        AddSignerActivity.start(activityContext)
    }

    override fun openAddSoftwareSignerScreen(
        activityContext: Context,
        passphrase: String,
        primaryKeyFlow: Int
    ) {
        SoftwareSignerIntroActivity.start(activityContext, passphrase, primaryKeyFlow)
    }

    override fun openCreateNewSeedScreen(
        activityContext: Context,
        passphrase: String,
        primaryKeyFlow: Int
    ) {
        CreateNewSeedActivity.start(activityContext, primaryKeyFlow, passphrase)
    }

    override fun openRecoverSeedScreen(
        activityContext: Context,
        passphrase: String,
        primaryKeyFlow: Int
    ) {
        RecoverSeedActivity.start(activityContext, passphrase, primaryKeyFlow)
    }

    override fun openSelectPhraseScreen(
        activityContext: Context,
        mnemonic: String,
        passphrase: String,
        primaryKeyFlow: Int
    ) {
        ConfirmSeedActivity.start(activityContext, mnemonic, passphrase, primaryKeyFlow)
    }

    override fun openAddSoftwareSignerNameScreen(
        activityContext: Context,
        mnemonic: String,
        primaryKeyFlow: Int,
        username: String?,
        passphrase: String,
        address: String?,
    ) {
        AddSoftwareSignerNameActivity.start(
            activityContext,
            mnemonic,
            primaryKeyFlow,
            username,
            passphrase,
            address
        )
    }

    override fun openSetPassphraseScreen(
        activityContext: Context,
        mnemonic: String,
        signerName: String,
        passphrase: String,
        primaryKeyFlow: Int
    ) {
        SetPassphraseActivity.start(
            activityContext,
            mnemonic,
            signerName,
            primaryKeyFlow,
            passphrase
        )
    }

    override fun openPrimaryKeyIntroScreen(activityContext: Context) {
        PKeySignUpIntroActivity.start(activityContext)
    }

    override fun openAddPrimaryKeyScreen(
        activityContext: Context,
        passphrase: String,
        primaryKeyFlow: Int
    ) {
        PKeyAddSignerActivity.start(activityContext, primaryKeyFlow, passphrase)
    }

    override fun openPrimaryKeyChooseUserNameScreen(
        activityContext: Context,
        mnemonic: String,
        passphrase: String,
        signerName: String
    ) {
        PKeyChooseUsernameActivity.start(activityContext, mnemonic, passphrase, signerName)
    }

    override fun openPrimaryKeySignInIntroScreen(activityContext: Context) {
        PKeySignInIntroActivity.start(activityContext)
    }

    override fun openPrimaryKeyAccountScreen(
        activityContext: Context,
        accounts: ArrayList<PrimaryKey>
    ) {
        PKeyAccountActivity.start(activityContext, accounts)
    }

    override fun openPrimaryKeySignInScreen(activityContext: Context, primaryKey: PrimaryKey) {
        PKeySignInActivity.start(activityContext, primaryKey)
    }

    override fun openPrimaryKeyEnterPassphraseScreen(
        activityContext: Context,
        mnemonic: String,
        primaryKeyFlow: Int,
    ) {
        PKeyEnterPassphraseActivity.start(activityContext, primaryKeyFlow, mnemonic)
    }

    override fun openPrimaryKeyManuallyUsernameScreen(activityContext: Context) {
        PKeyManuallyUsernameActivity.start(activityContext)
    }

    override fun openPrimaryKeyManuallySignatureScreen(activityContext: Context, username: String) {
        PKeyManuallySignatureActivity.start(activityContext, username)
    }

    override fun openPrimaryKeyNotificationScreen(
        activityContext: Context,
        messages: ArrayList<String>,
        primaryKeyFlow: Int
    ) {
        PKeyNotificationActivity.start(activityContext, messages, primaryKeyFlow)
    }

    override fun openPrimaryKeyReplaceIntroScreen(activityContext: Context, primaryKeyFlow: Int) {
        PKeyReplaceKeyIntroActivity.start(activityContext)
    }
}