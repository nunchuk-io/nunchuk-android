package com.nunchuk.android.core.domain.membership

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.SecurityQuestion
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetSecurityQuestionUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val userWalletsRepository: PremiumWalletRepository,
) : UseCase<GetSecurityQuestionUseCase.Param, List<SecurityQuestion>>(dispatcher) {
    override suspend fun execute(parameters: Param): List<SecurityQuestion> {
        val questions = userWalletsRepository.getSecurityQuestions(parameters.verifyToken)
        if (parameters.isFilterAnswer) {
            return questions.filter { it.isAnswer && it.id == "241188271403569152" }
        }
        return questions
    }

    class Param(val isFilterAnswer: Boolean, val verifyToken: String? = null)
}