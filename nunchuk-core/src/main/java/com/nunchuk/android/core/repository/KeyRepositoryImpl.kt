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
import com.nunchuk.android.model.KeyUpload
import com.nunchuk.android.model.KeyVerifiedRequest
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.model.MembershipStep
import com.nunchuk.android.model.SignerExtra
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.VerifyType
import com.nunchuk.android.model.isAddInheritanceKey
import com.nunchuk.android.model.toIndex
import com.nunchuk.android.nativelib.NunchukNativeSdk
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
        newIndex: Int
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
            val result = userWalletApiManager.walletApi.uploadBackupKey(
                keyName = keyNameBody,
                keyType = keyTypeBody,
                keyXfp = keyXfp,
                cardId = keyCardId,
                image = body
            )
            if (result.isSuccess || result.error.code == ALREADY_VERIFIED_CODE) {
                val response = if (result.isSuccess) result.data else null
                val chatId = accountManager.getAccount().chatId
                val verifyType =
                    if (result.error.code == ALREADY_VERIFIED_CODE) VerifyType.SELF_VERIFIED else VerifyType.NONE
                var signer: SingleSigner? = null
                if (groupId.isNotEmpty()) {
                    signer =
                        nativeSdk.getSignerByIndex(
                            xfp,
                            WalletType.MULTI_SIG.ordinal,
                            AddressType.NATIVE_SEGWIT.ordinal,
                            newIndex
                        )
                }
                val info = MembershipStepEntity(
                    chatId = chatId,
                    step = step,
                    masterSignerId = xfp,
                    keyIdInServer = response?.keyId.orEmpty(),
                    checkSum = response?.keyCheckSum.orEmpty(),
                    extraJson = gson.toJson(
                        SignerExtra(
                            derivationPath = signer?.derivationPath.orEmpty(),
                            isAddNew = isAddNewKey,
                            signerType = SignerType.NFC
                        )
                    ),
                    verifyType = verifyType,
                    plan = plan,
                    chain = chain.value,
                    groupId = groupId
                )
                if (groupId.isNotEmpty()) {
                    if (signer == null) throw NullPointerException("Can not get signer by index $newIndex")
                    val isInheritance = step.isAddInheritanceKey
                    val status = nativeSdk.getTapSignerStatusFromMasterSigner(xfp)
                    val keyResponse = userWalletApiManager.groupWalletApi.addKeyToServer(
                        groupId = groupId,
                        payload = SignerServerDto(
                            name = signer.name,
                            xfp = signer.masterFingerprint,
                            derivationPath = signer.derivationPath,
                            xpub = signer.xpub,
                            pubkey = signer.publicKey,
                            type = SignerType.NFC.name,
                            tapsigner = TapSignerDto(
                                cardId = status.ident.toString(),
                                version = status.version.orEmpty(),
                                birthHeight = status.birthHeight,
                                isTestnet = status.isTestNet,
                                isInheritance = isInheritance
                            ),
                            tags = if (isInheritance) listOf(
                                SignerTag.INHERITANCE.name
                            ) else null,
                            index = step.toIndex()
                        ),
                    )

                    if (keyResponse.isSuccess.not()) {
                        throw keyResponse.error
                    }
                }
                membershipDao.updateOrInsert(info)
                send(KeyUpload.Progress(100))
                if (result.isSuccess) {
                    val serverKeyFilePath = nfcFileManager.storeServerBackupKeyToFile(
                        result.data.keyId,
                        result.data.keyBackUpBase64
                    )
                    send(KeyUpload.Data(serverKeyFilePath))
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
        val response =
            userWalletApiManager.walletApi.setKeyVerified(
                stepInfo.keyIdInServer,
                KeyVerifiedRequest(
                    stepInfo.checkSum,
                    if (isAppVerify) "APP_VERIFIED" else "SELF_VERIFIED"
                )
            )
        if (response.isSuccess) {
            membershipDao.updateOrInsert(stepInfo.copy(verifyType = if (isAppVerify) VerifyType.APP_VERIFIED else VerifyType.SELF_VERIFIED))
        } else {
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
        val response = if (groupId.isNullOrEmpty()) {
            userWalletApiManager.walletApi.initReplaceKey(
                xfp,
                walletId,
            )
        } else {
            userWalletApiManager.groupWalletApi.initReplaceKey(
                groupId,
                xfp,
                walletId,
            )
        }

        if (response.isSuccess.not()) {
            throw response.error
        }
    }

    override suspend fun cancelReplaceKey(groupId: String?, walletId: String, xfp: String) {
        val response = if (groupId.isNullOrEmpty()) {
            userWalletApiManager.walletApi.cancelReplaceKey(
                xfp,
                walletId,
            )
        } else {
            userWalletApiManager.groupWalletApi.cancelReplaceKey(
                groupId,
                xfp,
                walletId,
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
        // TODO: Hai condition for isInheritanceKey
        val serverSigner = serverSignerMapper(signer, false)
        val response = if (groupId.isNullOrEmpty()) {
            userWalletApiManager.walletApi.replaceKey(
                walletId = walletId,
                xfp = xfp,
                payload = serverSigner
            )
        } else {
            userWalletApiManager.groupWalletApi.replaceKey(
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

    companion object {
        private const val ALREADY_VERIFIED_CODE = 409
    }
}