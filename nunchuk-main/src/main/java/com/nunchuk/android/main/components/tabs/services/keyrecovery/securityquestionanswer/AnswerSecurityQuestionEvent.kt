package com.nunchuk.android.main.components.tabs.services.keyrecovery.securityquestionanswer

import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.model.BackupKey
import com.nunchuk.android.model.SecurityQuestion

sealed class AnswerSecurityQuestionEvent {
    data class Loading(val isLoading: Boolean) : AnswerSecurityQuestionEvent()
    data class DownloadBackupKeySuccess(val signer: SignerModel, val backupKey: BackupKey) : AnswerSecurityQuestionEvent()
    data class ProcessFailure(val message: String) : AnswerSecurityQuestionEvent()
    data class OnVerifySuccess(val token: String) : AnswerSecurityQuestionEvent()
}

data class AnswerSecurityQuestionState(
    val question: SecurityQuestion? = null,
    val answer: String = "",
    val error: String = "",
)