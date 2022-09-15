package com.nunchuk.android.core.domain.membership

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.QuestionsAndAnswer
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class ConfigSecurityQuestionUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val userWalletsRepository: PremiumWalletRepository,
) : UseCase<List<QuestionsAndAnswer>, Unit>(dispatcher) {
    override suspend fun execute(parameters: List<QuestionsAndAnswer>) {
        userWalletsRepository.configSecurityQuestions(parameters)
    }
}