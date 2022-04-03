package com.nunchuk.android.core.domain

import com.nunchuk.android.core.data.model.AppUpdateResponse
import com.nunchuk.android.core.repository.NCAppRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface CheckUpdateRecommendUseCase {
    fun execute(): Flow<AppUpdateResponse>
}

internal class CheckUpdateRecommendUseCaseImpl @Inject constructor(
    private val ncAppRepository: NCAppRepository
) : CheckUpdateRecommendUseCase {

    override fun execute() = ncAppRepository.checkAppUpdate()

}