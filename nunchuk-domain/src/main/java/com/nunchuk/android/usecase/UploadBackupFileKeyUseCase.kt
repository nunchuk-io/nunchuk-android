package com.nunchuk.android.usecase

import com.nunchuk.android.FlowUseCase
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.KeyUpload
import com.nunchuk.android.model.MembershipStep
import com.nunchuk.android.repository.KeyRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class UploadBackupFileKeyUseCase @Inject constructor(
    @IoDispatcher private val dispatcher: CoroutineDispatcher, private val repository: KeyRepository
) : FlowUseCase<UploadBackupFileKeyUseCase.Param, KeyUpload>(dispatcher) {
    override fun execute(parameters: Param): Flow<KeyUpload> =
        repository.uploadBackupKey(
            step = parameters.step,
            keyName = parameters.keyName,
            keyType = parameters.keyType,
            xfp = parameters.xfp,
            filePath = parameters.filePath
        )

    data class Param(val step: MembershipStep, val keyName: String, val keyType: String, val xfp: String, val filePath: String)
}