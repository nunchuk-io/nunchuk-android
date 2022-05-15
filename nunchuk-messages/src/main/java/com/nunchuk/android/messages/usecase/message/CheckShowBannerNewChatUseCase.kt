package com.nunchuk.android.messages.usecase.message

import com.nunchuk.android.core.persistence.NCSharePreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

interface CheckShowBannerNewChatUseCase {
    fun execute(): Flow<Boolean>
}

internal class CheckShowBannerNewChatUseCaseImpl @Inject constructor(
    private val ncSharedPreferences: NCSharePreferences
) : CheckShowBannerNewChatUseCase {

    override fun execute() = flow {
        emit(ncSharedPreferences.showBannerNewChat)
    }.flowOn(Dispatchers.IO)

}