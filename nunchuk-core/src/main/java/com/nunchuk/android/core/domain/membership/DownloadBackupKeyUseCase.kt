package com.nunchuk.android.core.domain.membership

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.BackupKey
import com.nunchuk.android.model.QuestionsAndAnswer
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class DownloadBackupKeyUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val userWalletsRepository: PremiumWalletRepository,
) : UseCase<DownloadBackupKeyUseCase.Param, BackupKey>(dispatcher) {
    override suspend fun execute(parameters: Param): BackupKey {
        val questions = listOf(
            QuestionsAndAnswer(
                questionId = parameters.questionId,
                answer = parameters.answer
            )
        )
        return userWalletsRepository.downloadBackup(
            id = parameters.id,
            questions = questions,
            verifyToken = parameters.verifyToken
        )
    }

    class Param(val id: String, val questionId: String, val answer: String, val verifyToken: String)
}