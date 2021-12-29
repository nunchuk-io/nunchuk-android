package com.nunchuk.android.core.domain

import com.nunchuk.android.core.persistence.NCSharePreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface HideBannerNewChatUseCase {
    fun execute(): Flow<Boolean>
}

internal class HideBannerNewChatUseCaseImpl @Inject constructor(
    private val ncSharedPreferences: NCSharePreferences
) : HideBannerNewChatUseCase {

    override fun execute() = flow {
        ncSharedPreferences.showBannerNewChat = false
        emit(
            ncSharedPreferences.showBannerNewChat
        )
    }

}