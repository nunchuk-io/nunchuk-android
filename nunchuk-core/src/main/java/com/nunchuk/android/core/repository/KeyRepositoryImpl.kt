package com.nunchuk.android.core.repository

import com.google.gson.Gson
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.domain.utils.NfcFileManager
import com.nunchuk.android.core.manager.UserWalletApiManager
import com.nunchuk.android.model.*
import com.nunchuk.android.persistence.dao.MembershipStepDao
import com.nunchuk.android.persistence.entity.MembershipStepEntity
import com.nunchuk.android.persistence.updateOrInsert
import com.nunchuk.android.repository.KeyRepository
import com.nunchuk.android.type.SignerType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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
) : KeyRepository {
    override fun uploadBackupKey(
        step: MembershipStep,
        keyName: String,
        keyType: String,
        xfp: String,
        filePath: String,
        isAddNewKey: Boolean,
        plan: MembershipPlan
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
            val result = userWalletApiManager.walletApi.uploadBackupKey(
                keyName = keyNameBody,
                keyType = keyTypeBody,
                keyXfp = keyXfp,
                image = body
            )
            if (result.isSuccess || result.error.code == ALREADY_VERIFIED_CODE) {
                val response = if (result.isSuccess) result.data else null
                val chatId = accountManager.getAccount().chatId
                val verifyType =
                    if (result.error.code == ALREADY_VERIFIED_CODE) VerifyType.APP_VERIFIED else VerifyType.NONE
                val info = MembershipStepEntity(
                    chatId = chatId,
                    step = step,
                    masterSignerId = xfp,
                    keyIdInServer = response?.keyId.orEmpty(),
                    checkSum = response?.keyCheckSum.orEmpty(),
                    extraJson = gson.toJson(
                        SignerExtra(
                            derivationPath = "",
                            isAddNew = isAddNewKey,
                            signerType = SignerType.NFC
                        )
                    ),
                    verifyType = verifyType,
                    plan = plan
                )
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

    override suspend fun setKeyVerified(masterSignerId: String, isAppVerify: Boolean) {
        val stepInfo =
            membershipDao.getStepByMasterSignerId(accountManager.getAccount().chatId, masterSignerId)
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

    companion object {
        private const val ALREADY_VERIFIED_CODE = 409
    }
}