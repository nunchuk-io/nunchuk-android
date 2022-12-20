package com.nunchuk.android.repository

import com.nunchuk.android.model.*
import com.nunchuk.android.model.transaction.ExtendedTransaction
import com.nunchuk.android.model.transaction.ServerTransaction

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
    suspend fun createServerTransaction(walletId: String, psbt: String, note: String?, txId: String)
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

    suspend fun getLockdownPeriod(): List<LockdownPeriod>
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
        walletId: String
    ): CalculateRequiredSignatures

    suspend fun createOrUpdateInheritance(
        authorizations: List<String>,
        verifyToken: String,
        userData: String,
        securityQuestionToken: String,
        isUpdate: Boolean
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

    suspend fun inheritanceCheck(magic: String): InheritanceCheck
}