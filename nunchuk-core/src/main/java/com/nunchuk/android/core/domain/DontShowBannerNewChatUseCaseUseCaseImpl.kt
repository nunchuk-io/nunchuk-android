package com.nunchuk.android.core.domain

import com.nunchuk.android.core.persistence.NCSharePreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface DontShowBannerNewChatUseCase {
    fun execute(): Flow<Boolean>
}

internal class DontShowBannerNewChatUseCaseImpl @Inject constructor(
    private val ncSharedPreferences: NCSharePreferences
) : DontShowBannerNewChatUseCase {

    override fun execute() = flow {
        ncSharedPreferences.showBannerNewChat = false
        emit(
            ncSharedPreferences.showBannerNewChat
        )
    }

}