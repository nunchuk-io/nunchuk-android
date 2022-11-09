package com.nunchuk.android.repository

import com.nunchuk.android.model.*

interface PremiumWalletRepository {
    suspend fun getSecurityQuestions(): List<SecurityQuestion>
    suspend fun configSecurityQuestions(questions: List<QuestionsAndAnswer>)
    suspend fun createServerKeys(name: String, keyPolicy: KeyPolicy): KeyPolicy
    suspend fun getServerKey(xfp: String): KeyPolicy
    suspend fun updateServerKeys(keyIdOrXfp: String, name: String, policy: KeyPolicy) : KeyPolicy
    suspend fun createSecurityQuestion(question: String): SecurityQuestion
    suspend fun createServerWallet(wallet: Wallet, serverKeyId: String): SeverWallet
    suspend fun getServerWallet(): WalletServerSync
    suspend fun updateServerWallet(walletLocalId: String, name: String): SeverWallet
    suspend fun createServerTransaction(walletId: String, psbt: String, note: String?)
    suspend fun signServerTransaction(walletId: String, txId: String, psbt: String): Transaction?
    suspend fun getServerTransaction(walletId: String, transactionId: String): Transaction?
    suspend fun deleteServerTransaction(walletId: String, transactionId: String)
}