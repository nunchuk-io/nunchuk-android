package com.nunchuk.android.repository

import com.nunchuk.android.model.*

interface PremiumWalletRepository {
    suspend fun getSecurityQuestions(verifyToken: String?): List<SecurityQuestion>
    suspend fun configSecurityQuestions(questions: List<QuestionsAndAnswer>, plan: MembershipPlan)
    suspend fun createServerKeys(
        name: String, keyPolicy: KeyPolicy, plan: MembershipPlan
    ): KeyPolicy

    suspend fun getServerKey(xfp: String): KeyPolicy
    suspend fun updateServerKeys(keyIdOrXfp: String, name: String, policy: KeyPolicy): KeyPolicy
    suspend fun createSecurityQuestion(question: String): SecurityQuestion
    suspend fun createServerWallet(
        wallet: Wallet, serverKeyId: String, plan: MembershipPlan
    ): SeverWallet

    suspend fun getServerWallet(): WalletServerSync
    suspend fun updateServerWallet(walletLocalId: String, name: String): SeverWallet
    suspend fun createServerTransaction(walletId: String, psbt: String, note: String?, txId: String)
    suspend fun signServerTransaction(walletId: String, txId: String, psbt: String): Transaction?
    suspend fun getServerTransaction(walletId: String, transactionId: String): Transaction?
    suspend fun deleteServerTransaction(walletId: String, transactionId: String)
    suspend fun getInheritance(walletId: String): Inheritance
    suspend fun downloadBackup(id: String, questions: List<QuestionsAndAnswer>, verifyToken: String) : BackupKey
    suspend fun verifiedPasswordToken(targetAction: String, password: String): String?
    suspend fun calculateRequiredSignaturesSecurityQuestions(walletId: String, questions: List<QuestionsAndAnswer>): CalculateRequiredSignatures
    suspend fun securityQuestionsUpdate(authorizations: List<String>, verifyToken: String, userData: String)
    suspend fun getCurrentServerTime(): Long
}