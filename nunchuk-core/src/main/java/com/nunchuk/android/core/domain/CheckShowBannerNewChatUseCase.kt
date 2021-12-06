package com.nunchuk.android.core.domain

import com.nunchuk.android.core.persistence.NCSharePreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface CheckShowBannerNewChatUseCase {
    fun execute(): Flow<Boolean>
}

internal class CheckShowBannerNewChatUseCaseImpl @Inject constructor(
    private val ncSharedPreferences: NCSharePreferences
) : CheckShowBannerNewChatUseCase {

    override fun execute() = flow {
        emit(
            ncSharedPreferences.dontShowBannerNewChat
        )
    }

}