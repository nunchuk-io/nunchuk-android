package com.nunchuk.android.signer.repository

import com.nunchuk.android.api.key.KeyApi
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.domain.utils.NfcFileManager
import com.nunchuk.android.model.KeyUpload
import com.nunchuk.android.model.KeyVerifiedRequest
import com.nunchuk.android.model.MembershipStep
import com.nunchuk.android.persistence.dao.MembershipStepDao
import com.nunchuk.android.persistence.entity.MembershipStepEntity
import com.nunchuk.android.persistence.updateOrInsert
import com.nunchuk.android.repository.KeyRepository
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


class KeyRepositoryImpl @Inject constructor(
    private val keyApi: KeyApi,
    private val accountManager: AccountManager,
    private val membershipDao: MembershipStepDao,
    private val nfcFileManager: NfcFileManager,
) : KeyRepository {
    override fun uploadBackupKey(
        step: MembershipStep,
        keyName: String,
        keyType: String,
        xfp: String,
        filePath: String
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
            val result = keyApi.uploadBackupKey(
                keyName = keyNameBody,
                keyType = keyTypeBody,
                keyXfp = keyXfp,
                image = body
            )
            if (result.isSuccess) {
                val email = accountManager.getAccount().email
                val info = MembershipStepEntity(
                    email = email,
                    step = step,
                    masterSignerId = xfp,
                    keyIdInServer = result.data.keyId,
                    checkSum = result.data.keyCheckSum
                )
                membershipDao.updateOrInsert(info)
                val serverKeyFilePath = nfcFileManager.storeServerBackupKeyToFile(
                    result.data.keyId,
                    result.data.keyBackUpBase64
                )
                send(KeyUpload.Progress(100))
                send(KeyUpload.Data(serverKeyFilePath))
                file.delete()
            } else {
                throw result.error
            }
            awaitClose { }
        }
    }

    override suspend fun setKeyVerified(masterSignerId: String) {
        val stepInfo =
            membershipDao.getStepByMasterSignerId(accountManager.getAccount().email, masterSignerId)
                ?: throw NullPointerException("Can not mark key verified $masterSignerId")
        val response =
            keyApi.setKeyVerified(stepInfo.keyIdInServer, KeyVerifiedRequest(stepInfo.checkSum))
        if (response.isSuccess) {
            membershipDao.updateOrInsert(stepInfo.copy(isVerify = true))
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
}