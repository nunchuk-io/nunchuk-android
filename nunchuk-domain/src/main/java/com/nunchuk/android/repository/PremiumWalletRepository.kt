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

package com.nunchuk.android.repository

import com.nunchuk.android.model.BackupKey
import com.nunchuk.android.model.CalculateRequiredSignatures
import com.nunchuk.android.model.Inheritance
import com.nunchuk.android.model.InheritanceAdditional
import com.nunchuk.android.model.InheritanceCheck
import com.nunchuk.android.model.KeyPolicy
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.model.Period
import com.nunchuk.android.model.QuestionsAndAnswer
import com.nunchuk.android.model.SecurityQuestion
import com.nunchuk.android.model.SeverWallet
import com.nunchuk.android.model.TransactionAdditional
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.model.WalletServerSync
import com.nunchuk.android.model.membership.AssistedWalletBrief
import com.nunchuk.android.model.membership.AssistedWalletConfig
import com.nunchuk.android.model.transaction.ExtendedTransaction
import com.nunchuk.android.model.transaction.ServerTransaction
import kotlinx.coroutines.flow.Flow

interface PremiumWalletRepository {
    suspend fun getSecurityQuestions(verifyToken: String?): List<SecurityQuestion>
    suspend fun verifySecurityQuestions(questions: List<QuestionsAndAnswer>): String
    suspend fun configSecurityQuestions(questions: List<QuestionsAndAnswer>, plan: MembershipPlan)
    suspend fun createServerKeys(
        name: String, keyPolicy: KeyPolicy, plan: MembershipPlan
    ): KeyPolicy

    suspend fun getServerKey(xfp: String): KeyPolicy
    suspend fun updateServerKeys(
        signatures: Map<String, String>,
        keyIdOrXfp: String,
        token: String,
        securityQuestionToken: String,
        body: String,
    ): KeyPolicy

    suspend fun createSecurityQuestion(question: String): SecurityQuestion
    suspend fun createServerWallet(
        wallet: Wallet, serverKeyId: String, plan: MembershipPlan
    ): SeverWallet

    suspend fun getServerWallet(): WalletServerSync
    suspend fun updateServerWallet(walletLocalId: String, name: String): SeverWallet
    suspend fun updateServerKey(xfp: String, name: String): Boolean
    suspend fun createServerTransaction(walletId: String, psbt: String, note: String?)
    suspend fun updateServerTransaction(walletId: String, txId: String, note: String?)
    suspend fun signServerTransaction(
        walletId: String,
        txId: String,
        psbt: String
    ): ExtendedTransaction

    suspend fun getServerTransaction(walletId: String, transactionId: String): ExtendedTransaction
    suspend fun deleteServerTransaction(walletId: String, transactionId: String)
    suspend fun getInheritance(walletId: String): Inheritance
    suspend fun downloadBackup(
        id: String,
        questions: List<QuestionsAndAnswer>,
        verifyToken: String
    ): BackupKey

    suspend fun verifiedPasswordToken(targetAction: String, password: String): String?
    suspend fun verifiedPKeyToken(targetAction: String, address: String, signature: String): String?
    suspend fun calculateRequiredSignaturesSecurityQuestions(
        walletId: String,
        questions: List<QuestionsAndAnswer>
    ): CalculateRequiredSignatures

    suspend fun calculateRequiredSignaturesUpdateKeyPolicy(
        xfp: String,
        walletId: String,
        keyPolicy: KeyPolicy
    ): CalculateRequiredSignatures

    suspend fun securityQuestionsUpdate(
        authorizations: List<String>,
        verifyToken: String,
        userData: String,
        securityQuestionToken: String
    )

    suspend fun getNonce(): String
    suspend fun generateSecurityQuestionUserData(
        walletId: String,
        questions: List<QuestionsAndAnswer>
    ): String

    suspend fun generateUpdateServerKey(walletId: String, keyPolicy: KeyPolicy): String

    suspend fun scheduleTransaction(
        walletId: String,
        transactionId: String,
        scheduleTime: Long
    ): ServerTransaction

    suspend fun deleteScheduleTransaction(
        walletId: String,
        transactionId: String,
    ): ServerTransaction

    suspend fun getLockdownPeriod(): List<Period>
    suspend fun lockdownUpdate(
        authorizations: List<String>,
        verifyToken: String,
        userData: String,
        securityQuestionToken: String
    )

    suspend fun generateLockdownUserData(
        walletId: String,
        periodId: String
    ): String

    suspend fun calculateRequiredSignaturesLockdown(
        walletId: String,
        periodId: String
    ): CalculateRequiredSignatures

    suspend fun generateInheritanceUserData(
        note: String,
        notificationEmails: List<String>,
        notifyToday: Boolean,
        activationTimeMilis: Long,
        bufferPeriodId: String?,
        walletId: String
    ): String

    suspend fun generateInheritanceClaimStatusUserData(
        magic: String
    ): String

    suspend fun generateInheritanceClaimCreateTransactionUserData(
        magic: String,
        address: String,
        feeRate: String
    ): String

    suspend fun inheritanceClaimStatus(
        userData: String,
        masterFingerprint: String,
        signature: String
    ): InheritanceAdditional

    suspend fun inheritanceClaimCreateTransaction(
        userData: String,
        masterFingerprint: String,
        signature: String
    ): TransactionAdditional

    suspend fun generateCancelInheritanceUserData(
        walletId: String
    ): String

    suspend fun calculateRequiredSignaturesInheritance(
        note: String,
        notificationEmails: List<String>,
        notifyToday: Boolean,
        activationTimeMilis: Long,
        walletId: String,
        bufferPeriodId: String?,
        isCancelInheritance: Boolean
    ): CalculateRequiredSignatures

    suspend fun createOrUpdateInheritance(
        authorizations: List<String>,
        verifyToken: String,
        userData: String,
        securityQuestionToken: String,
        isUpdate: Boolean,
        plan: MembershipPlan
    ): Inheritance

    suspend fun cancelInheritance(
        authorizations: List<String>,
        verifyToken: String,
        userData: String,
        securityQuestionToken: String,
        walletId: String
    )

    suspend fun inheritanceClaimDownloadBackup(magic: String): BackupKey

    suspend fun inheritanceClaimingClaim(magic: String, psbt: String): TransactionAdditional

    suspend fun inheritanceCheck(): InheritanceCheck

    suspend fun syncTransaction(walletId: String)

    suspend fun getInheritanceBufferPeriod(): List<Period>

    fun getAssistedWalletsLocal(): Flow<List<AssistedWalletBrief>>

    suspend fun clearLocalData()

    suspend fun reuseKeyWallet(walletId: String, plan: MembershipPlan)

    suspend fun calculateRequiredSignaturesDeleteAssistedWallet(
        walletId: String,
    ): CalculateRequiredSignatures

    suspend fun deleteAssistedWallet(
        authorizations: List<String>,
        verifyToken: String,
        securityQuestionToken: String,
        walletId: String
    )

    suspend fun updateServerKeyName(xfp: String, name: String)

    suspend fun getAssistedWalletConfig() : AssistedWalletConfig

    fun assistedKeys() : Flow<Set<String>>

    suspend fun getCoinControlData(walletId: String): String

    suspend fun uploadCoinControlData(walletId: String, data: String)

    suspend fun clearTransactionEmergencyLockdown(walletId: String)
}