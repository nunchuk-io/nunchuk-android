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

import com.nunchuk.android.model.Alert
import com.nunchuk.android.model.BackupKey
import com.nunchuk.android.model.ByzantineGroup
import com.nunchuk.android.model.CalculateRequiredSignatures
import com.nunchuk.android.model.CalculateRequiredSignaturesAction
import com.nunchuk.android.model.CalculateRequiredSignaturesExt
import com.nunchuk.android.model.DefaultPermissions
import com.nunchuk.android.model.GroupChat
import com.nunchuk.android.model.GroupKeyPolicy
import com.nunchuk.android.model.HealthCheckHistory
import com.nunchuk.android.model.HealthReminder
import com.nunchuk.android.model.HistoryPeriod
import com.nunchuk.android.model.Inheritance
import com.nunchuk.android.model.InheritanceAdditional
import com.nunchuk.android.model.InheritanceCheck
import com.nunchuk.android.model.KeyPolicy
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.model.MembershipStep
import com.nunchuk.android.model.Period
import com.nunchuk.android.model.QuestionsAndAnswer
import com.nunchuk.android.model.SavedAddress
import com.nunchuk.android.model.SecurityQuestion
import com.nunchuk.android.model.SeverWallet
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.TransactionAdditional
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.model.WalletConfig
import com.nunchuk.android.model.WalletConstraints
import com.nunchuk.android.model.WalletServer
import com.nunchuk.android.model.WalletServerSync
import com.nunchuk.android.model.byzantine.AssistedMember
import com.nunchuk.android.model.byzantine.DummyTransactionPayload
import com.nunchuk.android.model.byzantine.GroupWalletType
import com.nunchuk.android.model.membership.AssistedWalletBrief
import com.nunchuk.android.model.membership.GroupConfig
import com.nunchuk.android.model.signer.SignerServer
import com.nunchuk.android.model.transaction.ExtendedTransaction
import com.nunchuk.android.model.transaction.ServerTransaction
import com.nunchuk.android.model.wallet.ReplaceWalletStatus
import com.nunchuk.android.type.SignerTag
import kotlinx.coroutines.flow.Flow

interface PremiumWalletRepository {
    suspend fun getSecurityQuestions(): List<SecurityQuestion>
    suspend fun verifySecurityQuestions(questions: List<QuestionsAndAnswer>): String
    suspend fun configSecurityQuestions(questions: List<QuestionsAndAnswer>)
    suspend fun createServerKeys(
        name: String, keyPolicy: KeyPolicy, plan: MembershipPlan
    ): KeyPolicy

    suspend fun getServerKey(xfp: String, derivationPath: String): KeyPolicy
    suspend fun getGroupServerKey(
        groupId: String,
        xfp: String,
        derivationPath: String
    ): GroupKeyPolicy

    suspend fun updateServerKeys(
        signatures: Map<String, String>,
        keyIdOrXfp: String,
        derivationPath: String,
        token: String,
        securityQuestionToken: String,
        body: String,
    ): String

    suspend fun updateGroupServerKeys(
        signatures: Map<String, String>,
        groupId: String,
        keyIdOrXfp: String,
        derivationPath: String,
        token: String,
        securityQuestionToken: String,
        body: String,
        draft: Boolean
    ): String

    suspend fun createSecurityQuestion(question: String): SecurityQuestion

    suspend fun getServerWallet(): WalletServerSync
    suspend fun createServerTransaction(
        groupId: String?,
        walletId: String,
        psbt: String,
        note: String?
    )

    suspend fun updateServerTransaction(
        groupId: String?,
        walletId: String,
        txId: String,
        note: String?
    )

    suspend fun signServerTransaction(
        groupId: String?,
        walletId: String,
        txId: String,
        psbt: String
    ): ExtendedTransaction

    suspend fun getServerTransaction(
        groupId: String?,
        walletId: String,
        transactionId: String
    ): ExtendedTransaction

    suspend fun deleteServerTransaction(groupId: String?, walletId: String, transactionId: String)
    suspend fun getInheritance(walletId: String, groupId: String?): Inheritance
    suspend fun markSetupInheritance(walletId: String, isSetupInheritance: Boolean)
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
        derivationPath: String,
        walletId: String,
        keyPolicy: KeyPolicy
    ): CalculateRequiredSignatures

    suspend fun calculateRequiredSignaturesUpdateGroupKeyPolicy(
        xfp: String,
        derivationPath: String,
        walletId: String,
        groupId: String,
        keyPolicy: GroupKeyPolicy
    ): CalculateRequiredSignatures

    suspend fun securityQuestionsUpdate(
        authorizations: List<String>,
        verifyToken: String,
        userData: String,
        securityQuestionToken: String,
        confirmCodeToken: String,
        confirmCodeNonce: String,
        draft: Boolean
    ): String

    suspend fun getNonce(): String
    suspend fun generateSecurityQuestionUserData(
        walletId: String,
        questions: List<QuestionsAndAnswer>
    ): String

    suspend fun generateUpdateServerKey(walletId: String, keyPolicy: KeyPolicy): String
    suspend fun generateUpdateGroupServerKey(walletId: String, keyPolicy: GroupKeyPolicy): String

    suspend fun scheduleTransaction(
        groupId: String?,
        walletId: String,
        transactionId: String,
        scheduleTime: Long
    ): ServerTransaction

    suspend fun deleteScheduleTransaction(
        groupId: String?,
        walletId: String,
        transactionId: String,
    ): ServerTransaction

    suspend fun getLockdownPeriod(groupId: String?): List<Period>
    suspend fun lockdownUpdate(
        authorizations: List<String>,
        verifyToken: String,
        userData: String,
        securityQuestionToken: String,
        confirmCodeToken: String,
        confirmCodeNonce: String
    )

    suspend fun generateLockdownUserData(
        walletId: String,
        periodId: String,
        groupId: String?
    ): String

    suspend fun calculateRequiredSignaturesLockdown(
        walletId: String,
        periodId: String,
        groupId: String?
    ): CalculateRequiredSignatures

    suspend fun calculateRequiredSignaturesChangeEmail(
        newEmail: String,
    ): CalculateRequiredSignatures

    suspend fun changeEmail(
        newEmail: String,
        verifyToken: String,
        securityQuestionToken: String,
        confirmCodeToken: String,
        confirmCodeNonce: String,
        draft: Boolean
    ): DummyTransactionPayload?

    suspend fun generateInheritanceUserData(
        note: String,
        notificationEmails: List<String>,
        notifyToday: Boolean,
        activationTimeMilis: Long,
        bufferPeriodId: String?,
        walletId: String,
        groupId: String?
    ): String

    suspend fun generateInheritanceClaimStatusUserData(magic: String): String

    suspend fun generateInheritanceClaimCreateTransactionUserData(
        magic: String,
        address: String,
        feeRate: String
    ): String

    suspend fun inheritanceClaimStatus(
        userData: String,
        masterFingerprints: List<String>,
        signatures: List<String>
    ): InheritanceAdditional

    suspend fun inheritanceClaimCreateTransaction(
        userData: String,
        masterFingerprints: List<String>,
        signatures: List<String>
    ): TransactionAdditional

    suspend fun generateCancelInheritanceUserData(
        walletId: String,
        groupId: String?
    ): String

    suspend fun generateRequestPlanningInheritanceUserData(
        walletId: String,
        groupId: String
    ): String

    suspend fun calculateRequiredSignaturesInheritance(
        note: String,
        notificationEmails: List<String>,
        notifyToday: Boolean,
        activationTimeMilis: Long,
        walletId: String,
        bufferPeriodId: String?,
        action: CalculateRequiredSignaturesAction,
        groupId: String?
    ): CalculateRequiredSignatures

    suspend fun createOrUpdateInheritance(
        authorizations: List<String>,
        verifyToken: String,
        userData: String,
        securityQuestionToken: String,
        isUpdate: Boolean,
        draft: Boolean
    ): String

    suspend fun cancelInheritance(
        authorizations: List<String>,
        verifyToken: String,
        userData: String,
        securityQuestionToken: String,
        walletId: String,
        draft: Boolean
    ): String

    suspend fun requestPlanningInheritance(
        authorizations: List<String>,
        userData: String,
        walletId: String,
        groupId: String
    ): String

    suspend fun inheritanceClaimDownloadBackup(
        magic: String,
        hashedBps: List<String>
    ): List<BackupKey>

    suspend fun inheritanceClaimingClaim(magic: String, psbt: String): TransactionAdditional

    suspend fun inheritanceCheck(): InheritanceCheck

    suspend fun syncTransaction(groupId: String?, walletId: String)

    suspend fun getInheritanceBufferPeriod(): List<Period>

    fun getAssistedWalletsLocal(): Flow<List<AssistedWalletBrief>>

    suspend fun clearLocalData()

    suspend fun calculateRequiredSignaturesDeleteAssistedWallet(
        walletId: String,
        groupId: String?
    ): CalculateRequiredSignatures

    suspend fun deleteAssistedWallet(
        authorizations: List<String>,
        verifyToken: String,
        securityQuestionToken: String,
        walletId: String,
        groupId: String?,
    )

    suspend fun updateServerKeyName(xfp: String, name: String, path: String)
    suspend fun getGroupAssistedWalletConfig(): GroupConfig

    fun assistedKeys(): Flow<Set<String>>

    suspend fun getCoinControlData(groupId: String?, walletId: String): String

    suspend fun uploadCoinControlData(groupId: String?, walletId: String, data: String)

    suspend fun clearTransactionEmergencyLockdown(groupId: String?, walletId: String)

    suspend fun requestAddKey(groupId: String, step: MembershipStep, tags: List<SignerTag>): String

    suspend fun checkKeyAdded(plan: MembershipPlan, groupId: String, requestId: String?): Boolean
    suspend fun deleteDraftWallet()
    suspend fun cancelRequestIdIfNeed(groupId: String, step: MembershipStep)
    suspend fun getPermissionGroupWallet(type: GroupWalletType): DefaultPermissions
    suspend fun createGroupServerKey(groupId: String, name: String, groupKeyPolicy: GroupKeyPolicy)
    suspend fun syncKey(groupId: String, step: MembershipStep, signer: SingleSigner)
    suspend fun createGroup(
        m: Int,
        n: Int,
        requiredServerKey: Boolean,
        allowInheritance: Boolean,
        setupPreference: String,
        members: List<AssistedMember>
    ): ByzantineGroup

    suspend fun getWalletConstraints(): List<WalletConstraints>
    fun getGroups(): Flow<List<ByzantineGroup>>
    suspend fun getGroupsRemote(): List<ByzantineGroup>
    suspend fun syncGroupWallets(): Boolean
    suspend fun hasGroupWallets(): Boolean
    fun getGroup(groupId: String): Flow<ByzantineGroup>
    suspend fun getLocalGroup(groupId: String): ByzantineGroup?
    suspend fun getGroupRemote(groupId: String): ByzantineGroup
    suspend fun deleteGroupWallet(groupId: String)
    suspend fun deleteGroup(groupId: String)
    suspend fun updateGroupStatus(groupId: String, status: String)
    suspend fun generateEditGroupMemberUserData(
        members: List<AssistedMember>
    ): String

    suspend fun calculateRequiredSignaturesEditGroupMember(
        groupId: String,
        members: List<AssistedMember>
    ): CalculateRequiredSignatures

    suspend fun editGroupMember(
        groupId: String,
        authorizations: List<String>,
        verifyToken: String,
        members: List<AssistedMember>,
        securityQuestionToken: String,
        confirmCodeToken: String,
        confirmCodeNonce: String
    ): ByzantineGroup

    suspend fun createGroupWallet(
        groupId: String,
        name: String,
        primaryMembershipId: String?
    ): Wallet

    suspend fun createPersonalWallet(
        name: String
    ): Wallet

    suspend fun groupMemberAcceptRequest(groupId: String)
    suspend fun groupMemberDenyRequest(groupId: String)
    suspend fun syncGroupWallet(
        groupId: String,
        groupAssistedKeys: MutableSet<String> = mutableSetOf()
    ): WalletServer

    fun getAlerts(groupId: String?, walletId: String?): Flow<List<Alert>>
    suspend fun getAlertsRemote(groupId: String?, walletId: String?): List<Alert>
    suspend fun markAlertAsRead(groupId: String?, walletId: String?, alertId: String)
    suspend fun dismissAlert(groupId: String?, walletId: String?, alertId: String)
    suspend fun getAlertTotal(groupId: String? = null, walletId: String? = null): Int
    suspend fun createOrUpdateGroupChat(
        roomId: String,
        groupId: String,
        historyPeriodId: String?
    ): GroupChat

    suspend fun getGroupChatByGroupId(groupId: String): GroupChat
    suspend fun deleteGroupChat(groupId: String)
    suspend fun getHistoryPeriod(): List<HistoryPeriod>
    suspend fun requestConfirmationCode(action: String, userData: String): Pair<String, String>
    suspend fun verifyConfirmationCode(codeId: String, code: String): String
    suspend fun updateServerWallet(
        walletLocalId: String,
        name: String,
        groupId: String?
    ): SeverWallet

    suspend fun getWallet(walletId: String): WalletServer

    suspend fun syncDeletedWallet(): Boolean
    suspend fun deleteKeyForReplacedWallet(groupId: String, walletId: String)
    suspend fun deleteKey(xfp: String)
    suspend fun healthCheckHistory(xfp: String): List<HealthCheckHistory>

    suspend fun syncConfirmedTransactionNotes(groupId: String?, walletId: String)

    suspend fun calculateRequiredSignaturesRecoverKey(
        xfp: String,
    ): CalculateRequiredSignaturesExt

    suspend fun requestRecoverKey(
        authorizations: List<String>,
        verifyToken: String,
        securityQuestionToken: String,
        confirmCodeToken: String,
        confirmCodeNonce: String,
        xfp: String,
    )

    suspend fun recoverKey(
        xfp: String,
        verifyToken: String,
        securityQuestionToken: String
    ): BackupKey

    suspend fun markKeyAsRecovered(xfp: String, status: String)
    suspend fun denyInheritanceRequestPlanning(requestId: String, groupId: String, walletId: String)
    suspend fun approveInheritanceRequestPlanning(
        requestId: String,
        groupId: String,
        walletId: String
    )

    suspend fun requestSignatureTransaction(
        groupId: String,
        walletId: String,
        transactionId: String,
        membershipId: String
    )

    suspend fun updatePrimaryOwner(groupId: String, walletId: String, primaryMembershipId: String)

    suspend fun replaceTransaction(
        groupId: String?,
        walletId: String,
        transactionId: String,
        newTxPsbt: String
    )

    suspend fun getUserWalletsServer(): List<WalletServer>
    suspend fun getGroupWalletsServer(): List<WalletServer>
    suspend fun updateKeyType(localSigner: SingleSigner, serverSigner: SignerServer)
    suspend fun getSavedAddressesRemote(): List<SavedAddress>
    fun getSavedAddressesLocal(): Flow<List<SavedAddress>>
    suspend fun addOrUpdateSavedAddress(address: String, label: String, isPremiumUser: Boolean)
    suspend fun deleteSavedAddress(address: String, isPremiumUser: Boolean)
    suspend fun getHealthReminders(groupId: String?, walletId: String): List<HealthReminder>
    suspend fun addOrUpdateHealthReminder(groupId: String?, walletId: String, xfps: List<String>, frequency: String, startDateMillis: Long)
    suspend fun deleteHealthReminder(groupId: String?, walletId: String, xfps: List<String>)
    suspend fun skipHealthReminder(groupId: String?, walletId: String, xfp: String)
    suspend fun replaceKeyStatus(
        groupId: String?,
        walletId: String,
    ): ReplaceWalletStatus

    suspend fun finalizeReplaceWallet(
        groupId: String?,
        walletId: String,
    ): Wallet

    suspend fun initPersonalWallet(
       walletConfig: WalletConfig
    )
}