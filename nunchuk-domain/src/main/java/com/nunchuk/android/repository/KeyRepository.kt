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

package com.nunchuk.android.repository

import com.nunchuk.android.model.KeyUpload
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.model.MembershipStep
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.VerifyType
import com.nunchuk.android.model.inheritance.ClaimOption
import com.nunchuk.android.model.signer.SupportedSigner
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.type.WalletType
import kotlinx.coroutines.flow.Flow

interface KeyRepository {
    fun uploadBackupKey(
        step: MembershipStep,
        keyName: String,
        keyType: String,
        xfp: String,
        cardId: String,
        filePath: String,
        isAddNewKey: Boolean,
        plan: MembershipPlan,
        groupId: String,
        newIndex: Int,
        isRequestAddKey: Boolean,
        walletType: WalletType,
        existingColdCard: SingleSigner? = null,
        isOnChainFlow: Boolean = false
    ): Flow<KeyUpload>

    fun uploadReplaceBackupKey(
        replacedXfp: String,
        keyName: String,
        keyType: String,
        xfp: String,
        cardId: String,
        filePath: String,
        isAddNewKey: Boolean,
        signerIndex: Int,
        walletId: String,
        groupId: String,
        isRequestReplaceKey: Boolean,
        existingColdCard: SingleSigner? = null,
        isOnChainFlow: Boolean = false
    ): Flow<KeyUpload>

    /**
     * [verificationMethod] names which claim option of an off-chain inheritance key was just
     * verified. Null on every other flow, where the server keeps one verification per key.
     */
    suspend fun setKeyVerified(
        groupId: String,
        masterSignerId: String,
        verifyType: VerifyType,
        verificationMethod: ClaimOption? = null,
    )

    /**
     * Records how the owner will pass an off-chain inheritance key to their Beneficiary.
     *
     * [walletId] empty targets the draft wallet, otherwise the replacement on that wallet. Dropping
     * [ClaimOption.ENCRYPTED_BACKUP] deletes the pending backup and its verification server-side and
     * cannot be undone, so confirm before calling.
     */
    suspend fun setInheritanceClaimOptions(
        groupId: String,
        walletId: String,
        xfp: String,
        claimOptions: List<ClaimOption>
    )

    /**
     * [verificationMethod] names which claim option of an off-chain inheritance key was just
     * verified, exactly as [setKeyVerified] does for the draft. Null on every other flow, where the
     * server keeps one verification per key.
     */
    suspend fun setReplaceKeyVerified(
        checkSum: String,
        keyId: String,
        verifyType: VerifyType,
        groupId: String,
        walletId: String,
        verificationMethod: ClaimOption? = null,
    )

    suspend fun initReplaceKey(
        groupId: String?,
        walletId: String,
        xfp: String,
    )

    suspend fun cancelReplaceKey(
        groupId: String?,
        walletId: String,
        xfp: String,
    )

    suspend fun replaceKey(
        groupId: String?,
        walletId: String,
        signer: SingleSigner,
        xfp: String,
        keyIndex: Int? = null,
    )

    suspend fun resetReplaceKey(
        groupId: String?,
        walletId: String,
    )

    suspend fun getReplaceSignerName(walletId: String, type: SignerType, tag: SignerTag?) : String

    suspend fun updateWalletReplaceConfig(
        walletId: String,
        groupId: String,
        isRemoveKey: Boolean,
    )

    suspend fun getBackUpKey(xfp: String, groupId: String): String

    suspend fun getBackUpKeyReplacement(xfp: String, groupId: String, walletId: String): String

    suspend fun getSupportedSigners(): List<SupportedSigner>
}