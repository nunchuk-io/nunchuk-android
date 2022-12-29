package com.nunchuk.android.core.domain.membership

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.QuestionsAndAnswer
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetSecurityQuestionsUserDataUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val userWalletRepository: PremiumWalletRepository,
) : UseCase<GetSecurityQuestionsUserDataUseCase.Param, String>(
    dispatcher
) {
    override suspend fun execute(parameters: Param): String {
        return userWalletRepository.generateSecurityQuestionUserData(
            parameters.walletId,
            parameters.questions
        )
    }

    class Param(val walletId: String, val questions: List<QuestionsAndAnswer>)
}