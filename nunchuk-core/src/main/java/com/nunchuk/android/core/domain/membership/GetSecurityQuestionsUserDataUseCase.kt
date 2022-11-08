package com.nunchuk.android.core.domain.membership

import com.google.gson.Gson
import com.nunchuk.android.core.data.model.QuestionsAndAnswerRequest
import com.nunchuk.android.core.data.model.QuestionsAndAnswerRequestBody
import com.nunchuk.android.core.data.model.SecurityQuestionsUpdateRequest
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.QuestionsAndAnswer
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import retrofit2.http.Body
import java.util.*
import javax.inject.Inject

class GetSecurityQuestionsUserDataUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val gson: Gson,
    private val userWalletRepository: PremiumWalletRepository,
) : UseCase<GetSecurityQuestionsUserDataUseCase.Param, String>(
    dispatcher
) {
    override suspend fun execute(parameters: Param): String {
        val currentServerTime = userWalletRepository.getCurrentServerTime()
        val questionsAndAnswerRequests = parameters.questions.map {
            QuestionsAndAnswerRequest(
                questionId = it.questionId,
                answer = it.answer
            )
        }
        val body = QuestionsAndAnswerRequestBody(questionsAndAnswerRequests, walletId = parameters.walletId)
        val nonce = UUID.randomUUID().toString()
        val request = SecurityQuestionsUpdateRequest(
            nonce = nonce,
            iat = currentServerTime / 1000,
            exp = currentServerTime / 1000 + 30 * 60,
            body = body
        )
        return gson.toJson(request)
    }

    class Param(val walletId: String, val questions: List<QuestionsAndAnswer>)
}