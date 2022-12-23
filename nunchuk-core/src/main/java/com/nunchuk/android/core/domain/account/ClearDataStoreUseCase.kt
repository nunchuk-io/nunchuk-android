package com.nunchuk.android.core.domain.account

import com.nunchuk.android.core.profile.UserProfileRepository
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class ClearDataStoreUseCase @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val userProfileRepository: UserProfileRepository
) : UseCase<Unit, Unit>(ioDispatcher) {
    override suspend fun execute(parameters: Unit) {
        userProfileRepository.clearDataStore()
    }
}