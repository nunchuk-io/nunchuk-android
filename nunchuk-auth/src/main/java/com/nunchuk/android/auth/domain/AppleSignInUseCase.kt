package com.nunchuk.android.auth.domain

import com.google.gson.Gson
import com.nunchuk.android.auth.api.UserTokenResponse
import com.nunchuk.android.core.account.AccountInfo
import com.nunchuk.android.core.guestmode.SignInMode
import com.nunchuk.android.core.network.ErrorResponse
import com.nunchuk.android.core.network.NunchukApiException
import com.nunchuk.android.core.network.UNKNOWN_ERROR
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import org.json.JSONObject
import javax.inject.Inject

class AppleSignInUseCase @Inject constructor(
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
    private val storeAccountUseCase: StoreAccountUseCase
) : UseCase<String, AccountInfo>(ioDispatcher) {

    override suspend fun execute(parameters: String): AccountInfo {
        val jsonObject = JSONObject(parameters)
        val data = jsonObject.getJSONObject("data").toString()
        val errorJson = jsonObject.getJSONObject("error").toString()
        val gson = Gson()
        if (errorJson.isNotEmpty()) {
            val error = gson.fromJson(
                errorJson,
                ErrorResponse::class.java
            )
            if (error.code != 0) {
                throw NunchukApiException(
                    code = error.code,
                    message = error.message ?: UNKNOWN_ERROR,
                    errorDetail = error.details
                )
            }
        }

        val userResponse = gson.fromJson(
            data,
            UserTokenResponse::class.java
        )
        return storeAccountUseCase(
            StoreAccountUseCase.Param(
                "",
                userResponse,
                staySignedIn = true,
                fetchUserInfo = true,
                loginType = SignInMode.EMAIL
            )
        ).getOrThrow()
    }
}