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

package com.nunchuk.android.core.repository

import com.google.gson.Gson
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.data.model.membership.SignerServerDto
import com.nunchuk.android.core.data.model.membership.TapSignerDto
import com.nunchuk.android.core.domain.utils.NfcFileManager
import com.nunchuk.android.core.manager.UserWalletApiManager
import com.nunchuk.android.core.mapper.ServerSignerMapper
import com.nunchuk.android.core.persistence.NcDataStore
import com.nunchuk.android.core.signer.toSignerTag
import com.nunchuk.android.core.util.COLDCARD_DEFAULT_KEY_NAME
import com.nunchuk.android.core.util.formattedName
import com.nunchuk.android.core.util.toSignerType
import com.nunchuk.android.model.KeyUpload
import com.nunchuk.android.model.KeyVerifiedRequest
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.model.MembershipStep
import com.nunchuk.android.model.SignerExtra
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.VerifyType
import com.nunchuk.android.model.isAddInheritanceKey
import com.nunchuk.android.model.signer.SupportedSigner
import com.nunchuk.android.model.toIndex
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.persistence.dao.AssistedWalletDao
import com.nunchuk.android.persistence.dao.MembershipStepDao
import com.nunchuk.android.persistence.entity.MembershipStepEntity
import com.nunchuk.android.repository.KeyRepository
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.Chain
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.type.WalletType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import javax.inject.Inject


internal class KeyRepositoryImpl @Inject constructor(
    private val userWalletApiManager: UserWalletApiManager,
    private val accountManager: AccountManager,
    private val membershipDao: MembershipStepDao,
    private val nfcFileManager: NfcFileManager,
    private val gson: Gson,
    private val ncDataStore: NcDataStore,
    private val nativeSdk: NunchukNativeSdk,
    private val serverSignerMapper: ServerSignerMapper,
    private val assistedWalletDao: AssistedWalletDao,
    applicationScope: CoroutineScope,
) : KeyRepository {
    private val chain =
        ncDataStore.chain.stateIn(applicationScope, SharingStarted.Eagerly, Chain.MAIN)

    override fun uploadBackupKey(
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
        existingColdCard: SingleSigner?
    ): Flow<KeyUpload> {
        return callbackFlow {
            val file = File(filePath)
            val requestFile: RequestBody =
                asRequestBody(file, "multipart/form-data".toMediaTypeOrNull()) {
                    trySend(KeyUpload.Progress(it.coerceAtMost(99)))
                }

            val body: MultipartBody.Part =
                MultipartBody.Part.createFormData("file", file.name, requestFile)

            val keyNameBody: RequestBody =
                keyName.toRequestBody("multipart/form-data".toMediaTypeOrNull())
            val keyTypeBody: RequestBody =
                keyType.toRequestBody("multipart/form-data".toMediaTypeOrNull())
            val keyXfp: RequestBody = xfp.toRequestBody("multipart/form-data".toMediaTypeOrNull())
            val keyCardId: RequestBody =
                cardId.toRequestBody("multipart/form-data".toMediaTypeOrNull())
            val result = if (groupId.isEmpty()) {
                userWalletApiManager.walletApi.uploadBackupKey(
                    keyName = keyNameBody,
                    keyType = keyTypeBody,
                    keyXfp = keyXfp,
                    cardId = keyCardId,
                    image = body
                )
            } else {
                userWalletApiManager.groupWalletApi.uploadBackupKey(
                    groupId = groupId,
                    keyName = keyNameBody,
                    keyType = keyTypeBody,
                    keyXfp = keyXfp,
                    cardId = keyCardId,
                    image = body
                )
            }
            if (result.isSuccess || result.error.code == ALREADY_VERIFIED_CODE) {
                val response = if (result.isSuccess) result.data else null
                val chatId = accountManager.getAccount().chatId
                val verifyType =
                    if (result.error.code == ALREADY_VERIFIED_CODE) VerifyType.SELF_VERIFIED else VerifyType.NONE
                val signer = existingColdCard
                    ?: (nativeSdk.getSignerByIndex(
                        xfp,
                        WalletType.MULTI_SIG.ordinal,
                        AddressType.NATIVE_SEGWIT.ordinal,
                        newIndex
                    ) ?: throw NullPointerException("Can not get signer by index $newIndex"))
                val info = MembershipStepEntity(
                    chatId = chatId,
                    step = step,
                    masterSignerId = xfp,
                    keyIdInServer = response?.keyId.orEmpty(),
                    checkSum = response?.keyCheckSum.orEmpty(),
                    extraJson = gson.toJson(
                        SignerExtra(
                            derivationPath = signer.derivationPath,
                            isAddNew = isAddNewKey,
                            signerType = keyType.toSignerType(),
                            userKeyFileName = if (result.error.code != ALREADY_VERIFIED_CODE) result.data.fileName.orEmpty() else "",
                        )
                    ),
                    verifyType = verifyType,
                    plan = plan,
                    chain = chain.value,
                    groupId = groupId
                )
                val isInheritance = step.isAddInheritanceKey
                val status =
                    if (keyType == SignerType.NFC.name) nativeSdk.getTapSignerStatusFromMasterSigner(
                        xfp
                    ) else null
                val tags = if (isInheritance) {
                    when (keyType) {
                        SignerType.NFC.name -> {
                            listOf(SignerTag.INHERITANCE.name)
                        }

                        SignerType.COLDCARD_NFC.name, SignerType.AIRGAP.name, SignerType.HARDWARE.name -> {
                            listOf(SignerTag.INHERITANCE.name, SignerTag.COLDCARD.name)
                        }

                        else -> {
                            listOf(SignerTag.INHERITANCE.name)
                        }
                    }
                } else null
                val payload = SignerServerDto(
                    name = signer.name,
                    xfp = signer.masterFingerprint,
                    derivationPath = signer.derivationPath,
                    xpub = signer.xpub,
                    pubkey = signer.publicKey,
                    type = keyType,
                    tapsigner = if (keyType == SignerType.NFC.name) {
                        TapSignerDto(
                            cardId = status!!.ident.toString(),
                            version = status.version.orEmpty(),
                            birthHeight = status.birthHeight,
                            isTestnet = status.isTestNet,
                            isInheritance = isInheritance
                        )
                    } else null,
                    tags = tags,
                    index = step.toIndex()
                )
                if (isRequestAddKey) {
                    Timber.tag("inheritance-add").e("uploadBackupKey - $groupId, $xfp, $payload")
                    val keyResponse = if (groupId.isNotEmpty()) {
                        userWalletApiManager.groupWalletApi.addKeyToServer(
                            groupId = groupId,
                            payload = payload,
                        )
                    } else {
                        userWalletApiManager.walletApi.addKeyToServer(
                            payload = payload,
                        )
                    }
                    if (keyResponse.isSuccess.not()) {
                        throw keyResponse.error
                    }
                }
                val existingInfo = membershipDao.getStep(chatId, chain.value, step, groupId)
                if (existingInfo != null && tags.orEmpty().contains(SignerTag.COLDCARD.name)) {
                    membershipDao.delete(existingInfo)
                }
                membershipDao.updateOrInsert(info)
                send(KeyUpload.Progress(100))
                if (result.isSuccess) {
                    val serverKeyFilePath = nfcFileManager.storeServerBackupKeyToFile(
                        result.data.keyId,
                        result.data.keyBackUpBase64
                    )
                    send(
                        KeyUpload.Data(
                            filePath = serverKeyFilePath,
                            backUpFileName = result.data.fileName.orEmpty(),
                            keyId = result.data.keyId
                        )
                    )
                } else {
                    send(KeyUpload.KeyVerified(result.error.message))
                }
                file.delete()
            } else {
                throw result.error
            }
            awaitClose { }
        }
    }

    override fun uploadReplaceBackupKey(
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
        exisingColdCard: SingleSigner?
    ): Flow<KeyUpload> {
        return callbackFlow {
            val file = File(filePath)
            val requestFile: RequestBody =
                asRequestBody(file, "multipart/form-data".toMediaTypeOrNull()) {
                    trySend(KeyUpload.Progress(it.coerceAtMost(99)))
                }

            val body: MultipartBody.Part =
                MultipartBody.Part.createFormData("file", file.name, requestFile)

            val keyNameBody: RequestBody =
                keyName.toRequestBody("multipart/form-data".toMediaTypeOrNull())
            val keyTypeBody: RequestBody =
                keyType.toRequestBody("multipart/form-data".toMediaTypeOrNull())
            val keyXfp: RequestBody = xfp.toRequestBody("multipart/form-data".toMediaTypeOrNull())
            val keyCardId: RequestBody =
                cardId.toRequestBody("multipart/form-data".toMediaTypeOrNull())
            val verifyToken = ncDataStore.passwordToken.first()
            val result = if (groupId.isEmpty()) {
                userWalletApiManager.walletApi.uploadBackupKeyReplacement(
                    walletId = walletId,
                    verifyToken = verifyToken,
                    keyName = keyNameBody,
                    keyType = keyTypeBody,
                    keyXfp = keyXfp,
                    cardId = keyCardId,
                    image = body
                )
            } else {
                userWalletApiManager.groupWalletApi.uploadBackupKeyReplacement(
                    groupId = groupId,
                    walletId = walletId,
                    verifyToken = verifyToken,
                    keyName = keyNameBody,
                    keyType = keyTypeBody,
                    keyXfp = keyXfp,
                    cardId = keyCardId,
                    image = body
                )
            }
            if (result.isSuccess.not() && result.error.code != ALREADY_VERIFIED_CODE) {
                throw result.error
            }
            val signer = exisingColdCard ?: nativeSdk.getSignerByIndex(
                xfp,
                WalletType.MULTI_SIG.ordinal,
                AddressType.NATIVE_SEGWIT.ordinal,
                0
            ) ?: throw NullPointerException("Can not get signer by index 0")
            val status =
                if (keyType == SignerType.NFC.name) nativeSdk.getTapSignerStatusFromMasterSigner(xfp) else null
            val wallet = if (groupId.isNotEmpty()) {
                userWalletApiManager.groupWalletApi.getGroupWallet(groupId)
            } else {
                userWalletApiManager.walletApi.getWallet(walletId)
            }.data.wallet ?: throw NullPointerException("Can not get wallet $walletId")
            val isInheritance =
                wallet.signerServerDtos.find { it.xfp == replacedXfp }?.tags?.contains(
                    SignerTag.INHERITANCE.name
                ) == true
            val tags = if (isInheritance) {
                when (keyType) {
                    SignerType.NFC.name -> {
                        listOf(SignerTag.INHERITANCE.name)
                    }

                    SignerType.COLDCARD_NFC.name, SignerType.AIRGAP.name, SignerType.HARDWARE.name -> {
                        listOf(SignerTag.INHERITANCE.name, SignerTag.COLDCARD.name)
                    }

                    else -> {
                        listOf(SignerTag.INHERITANCE.name)
                    }
                }
            } else null
            val payload = SignerServerDto(
                name = signer.name,
                xfp = signer.masterFingerprint,
                derivationPath = signer.derivationPath,
                xpub = signer.xpub,
                pubkey = signer.publicKey,
                type = keyType,
                tapsigner = if (keyType == SignerType.NFC.name) {
                    TapSignerDto(
                        cardId = status!!.ident.toString(),
                        version = status.version.orEmpty(),
                        birthHeight = status.birthHeight,
                        isTestnet = status.isTestNet,
                        isInheritance = isInheritance
                    )
                } else null,
                tags = tags,
            )
            if (isRequestReplaceKey) {
                val replaceResponse = if (groupId.isNotEmpty()) {
                    userWalletApiManager.groupWalletApi.replaceKey(
                        verifyToken = verifyToken,
                        groupId = groupId,
                        walletId = walletId,
                        xfp = replacedXfp,
                        payload = payload
                    )
                } else {
                    userWalletApiManager.walletApi.replaceKey(
                        verifyToken = verifyToken,
                        walletId = walletId,
                        xfp = replacedXfp,
                        payload = payload
                    )
                }

                if (replaceResponse.isSuccess.not()) {
                    throw replaceResponse.error
                }
            }
            send(KeyUpload.Progress(100))

            if (result.isSuccess) {
                val serverKeyFilePath = nfcFileManager.storeServerBackupKeyToFile(
                    result.data.keyId,
                    result.data.keyBackUpBase64
                )
                send(
                    KeyUpload.Data(
                        filePath = serverKeyFilePath,
                        backUpFileName = result.data.fileName.orEmpty(),
                        keyId = result.data.keyId
                    )
                )

            } else {
                send(KeyUpload.KeyVerified(result.error.message))
            }
            file.delete()

            awaitClose { }
        }
    }

    override suspend fun setKeyVerified(
        groupId: String,
        masterSignerId: String,
        isAppVerify: Boolean
    ) {
        val stepInfo =
            membershipDao.getStepByMasterSignerId(
                email = accountManager.getAccount().chatId,
                chain = chain.value,
                masterSignerId = masterSignerId,
                groupId = groupId
            )
                ?: throw NullPointerException("Can not mark key verified $masterSignerId")
        val response = if (groupId.isEmpty()) {
            userWalletApiManager.walletApi.setKeyVerified(
                stepInfo.keyIdInServer.ifEmpty { stepInfo.masterSignerId },
                KeyVerifiedRequest(
                    stepInfo.checkSum,
                    if (isAppVerify) "APP_VERIFIED" else "SELF_VERIFIED"
                )
            )
        } else {
            userWalletApiManager.groupWalletApi.setKeyVerified(
                groupId,
                stepInfo.keyIdInServer.ifEmpty { stepInfo.masterSignerId },
                KeyVerifiedRequest(
                    stepInfo.checkSum,
                    if (isAppVerify) "APP_VERIFIED" else "SELF_VERIFIED"
                )
            )
        }
        if (response.isSuccess) {
            membershipDao.updateOrInsert(stepInfo.copy(verifyType = if (isAppVerify) VerifyType.APP_VERIFIED else VerifyType.SELF_VERIFIED))
        } else {
            throw response.error
        }
    }

    override suspend fun setReplaceKeyVerified(
        checkSum: String,
        keyId: String,
        isAppVerify: Boolean,
        groupId: String,
        walletId: String
    ) {
        val verifyToken = ncDataStore.passwordToken.first()
        val response = if (groupId.isEmpty()) {
            userWalletApiManager.walletApi.setKeyVerifiedReplacement(
                verifyToken = verifyToken,
                keyId = keyId,
                walletId = walletId,
                KeyVerifiedRequest(
                    checkSum,
                    if (isAppVerify) "APP_VERIFIED" else "SELF_VERIFIED"
                )
            )
        } else {
            userWalletApiManager.groupWalletApi.setKeyVerifiedReplacement(
                verifyToken = verifyToken,
                groupId = groupId,
                keyId = keyId,
                walletId = walletId,
                payload = KeyVerifiedRequest(
                    checkSum,
                    if (isAppVerify) "APP_VERIFIED" else "SELF_VERIFIED"
                )
            )
        }

        if (response.isSuccess.not()) {
            throw response.error
        }
    }

    private fun asRequestBody(
        file: File,
        contentType: MediaType? = null,
        onProgressChange: (progress: Int) -> Unit
    ): RequestBody {
        return object : RequestBody() {
            override fun contentType() = contentType

            override fun contentLength() = file.length()

            override fun writeTo(sink: BufferedSink) {
                val fileLength = contentLength()
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                val inSt = FileInputStream(file)
                var uploaded = 0L
                inSt.use {
                    var read: Int = inSt.read(buffer)
                    while (read != -1) {
                        uploaded += read
                        val progress = (uploaded * 100L / fileLength).toInt()
                        Timber.d("Upload progress: $progress")
                        onProgressChange(progress)

                        sink.write(buffer, 0, read)
                        read = inSt.read(buffer)
                    }
                }
            }
        }
    }

    override suspend fun initReplaceKey(
        groupId: String?,
        walletId: String,
        xfp: String,
    ) {
        val verifyToken = ncDataStore.passwordToken.first()
        val response = if (groupId.isNullOrEmpty()) {
            userWalletApiManager.walletApi.initReplaceKey(
                verifyToken = verifyToken,
                xfp = xfp,
                walletId = walletId,
            )
        } else {
            userWalletApiManager.groupWalletApi.initReplaceKey(
                verifyToken = verifyToken,
                groupId = groupId,
                xfp = xfp,
                walletId = walletId,
            )
        }

        if (response.isSuccess.not()) {
            throw response.error
        }
    }

    override suspend fun cancelReplaceKey(groupId: String?, walletId: String, xfp: String) {
        val response = if (groupId.isNullOrEmpty()) {
            userWalletApiManager.walletApi.cancelReplaceKey(
                walletId = walletId,
                xfp = xfp,
            )
        } else {
            userWalletApiManager.groupWalletApi.cancelReplaceKey(
                groupId = groupId,
                walletId = walletId,
                xfp = xfp,
            )
        }

        if (response.isSuccess.not()) {
            throw response.error
        }
    }

    override suspend fun replaceKey(
        groupId: String?,
        walletId: String,
        signer: SingleSigner,
        xfp: String
    ) {
        val verifyToken = ncDataStore.passwordToken.first()
        val wallet = nativeSdk.getWallet(walletId)
        val isInheritance = wallet.signers.find { it.masterFingerprint == xfp }?.tags.orEmpty()
            .contains(SignerTag.INHERITANCE)
        val serverSigner = serverSignerMapper(signer, isInheritance)
        val response = if (groupId.isNullOrEmpty()) {
            userWalletApiManager.walletApi.replaceKey(
                verifyToken = verifyToken,
                walletId = walletId,
                xfp = xfp,
                payload = serverSigner,
            )
        } else {
            userWalletApiManager.groupWalletApi.replaceKey(
                verifyToken = verifyToken,
                groupId = groupId,
                walletId = walletId,
                xfp = xfp,
                payload = serverSigner
            )
        }

        if (response.isSuccess.not()) {
            throw response.error
        }
    }

    override suspend fun resetReplaceKey(groupId: String?, walletId: String) {
        val verifyToken = ncDataStore.passwordToken.first()
        val response = if (groupId.isNullOrEmpty()) {
            userWalletApiManager.walletApi.resetReplaceWallet(
                verifyToken = verifyToken,
                walletId = walletId,
            )
        } else {
            userWalletApiManager.groupWalletApi.resetReplaceWallet(
                verifyToken = verifyToken,
                groupId = groupId,
                walletId = walletId,
            )
        }

        if (response.isSuccess.not()) {
            throw response.error
        }
    }


    override suspend fun getReplaceSignerName(
        walletId: String,
        type: SignerType,
        tag: SignerTag?
    ): String {
        val wallet = nativeSdk.getWallet(walletId)
        val replaceSignerTypes = assistedWalletDao.getById(walletId)?.replaceSignerTypes.orEmpty()
        val numberOfSigners =
            wallet.signers.count { it.type == type } + replaceSignerTypes.count { it == type } + 1
        val prefix = when (type) {
            SignerType.SOFTWARE -> "Key"
            SignerType.HARDWARE -> "Hardware Key"
            SignerType.AIRGAP -> tag.formattedName
            SignerType.NFC -> "TAPSIGNER"
            SignerType.COLDCARD_NFC -> COLDCARD_DEFAULT_KEY_NAME
            else -> "Key"
        }

        val suffix = if (numberOfSigners > 1) {
            " #${numberOfSigners}"
        } else {
            ""
        }
        return "$prefix$suffix"
    }

    override suspend fun updateWalletReplaceConfig(
        walletId: String,
        groupId: String,
        isRemoveKey: Boolean
    ) {
        val response = if (groupId.isNotEmpty()) {
            userWalletApiManager.groupWalletApi.updateReplaceWalletConfigs(
                groupId = groupId,
                walletId = walletId,
                payload = mapOf(
                    "remove_unused_keys" to isRemoveKey
                )
            )
        } else {
            userWalletApiManager.walletApi.updateReplaceWalletConfigs(
                walletId = walletId,
                payload = mapOf(
                    "remove_unused_keys" to isRemoveKey
                )
            )
        }

        if (response.isSuccess.not()) {
            throw response.error
        }
    }

    override suspend fun getBackUpKey(xfp: String, groupId: String): String {
        val response = if (groupId.isEmpty()) {
            userWalletApiManager.walletApi.downloadBackup(xfp = xfp)
        } else {
            userWalletApiManager.groupWalletApi.downloadBackup(groupId = groupId, xfp = xfp)
        }
        if (response.isSuccess.not()) {
            throw response.error
        }
        val serverKeyFilePath = nfcFileManager.storeServerBackupKeyToFile(
            response.data.keyId,
            response.data.keyBackUpBase64
        )
        return serverKeyFilePath
    }

    override suspend fun getBackUpKeyReplacement(
        xfp: String,
        groupId: String,
        walletId: String
    ): String {
        val verifyToken = ncDataStore.passwordToken.first()
        val response = if (groupId.isEmpty()) {
            userWalletApiManager.walletApi.downloadBackupReplacement(
                walletId = walletId,
                verifyToken = verifyToken,
                xfp = xfp
            )
        } else {
            userWalletApiManager.groupWalletApi.downloadBackupReplacement(
                groupId = groupId,
                walletId = walletId,
                verifyToken = verifyToken,
                xfp = xfp
            )
        }
        if (response.isSuccess.not()) {
            throw response.error
        }
        val serverKeyFilePath = nfcFileManager.storeServerBackupKeyToFile(
            response.data.keyId,
            response.data.keyBackUpBase64
        )
        return serverKeyFilePath
    }

    override suspend fun getSupportedSigners(): List<SupportedSigner> {
        val response = userWalletApiManager.walletApi.getSupportedSigners()
        if (response.isSuccess.not()) {
            throw response.error
        }
        return response.data.supportedSigners.map {
            SupportedSigner(
                type = it.signerType.toSignerType(),
                tag = it.signerTag.toSignerTag()
            )
        }
    }

    companion object {
        private const val ALREADY_VERIFIED_CODE = 409
    }
}