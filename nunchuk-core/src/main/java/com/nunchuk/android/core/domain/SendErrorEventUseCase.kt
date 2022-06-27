package com.nunchuk.android.core.domain

import com.nunchuk.android.core.provider.AppInfoProvider
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

interface SendErrorEventUseCase {
    fun execute(roomId: String, throwable: Throwable): Flow<Unit>
}

internal class SendErrorEventUseCaseImpl @Inject constructor(
    private val appInfoProvider: AppInfoProvider,
    private val nativeSdk: NunchukNativeSdk
) : SendErrorEventUseCase {

    override fun execute(roomId: String, throwable: Throwable) = flow {
        val sdkInt = android.os.Build.VERSION.SDK_INT
        val appName = appInfoProvider.getAppName()
        val appVersionName = appInfoProvider.getAppVersionName()
        val platform = "Android $sdkInt - $appName $appVersionName"
        val reason = throwable.message.orEmpty()
        val (code, message) = getCodeFromMessage(reason)
        emit(
            if (code !in IGNORED_EXCEPTIONS) {
                nativeSdk.sendErrorEvent(roomId = roomId, platform = platform, code = code, message = message)
            } else Unit
        )
    }.flowOn(Dispatchers.IO)

    private fun getCodeFromMessage(message: String) = when {
        message.isEmpty() -> UNKNOWN_ERROR to message
        !message.contains(":") -> UNKNOWN_ERROR to message
        else -> {
            val strings = message.split(":")
            strings[0].trim() to strings[1].trim()
        }
    }

    companion object {
        const val UNKNOWN_ERROR = (-1).toString()
        private const val TX_NOT_FOUND_EXCEPTION = (-2003).toString()
        private const val SHARED_WALLET_NOT_FOUND_EXCEPTION = (-5002).toString()
        private val IGNORED_EXCEPTIONS = listOf(TX_NOT_FOUND_EXCEPTION, SHARED_WALLET_NOT_FOUND_EXCEPTION)
    }
}