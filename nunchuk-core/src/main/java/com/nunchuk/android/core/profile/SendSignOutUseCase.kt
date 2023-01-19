package com.nunchuk.android.core.profile

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

data class SendSignOutUseCase @Inject constructor(
    private val userProfileRepository: UserProfileRepository,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : UseCase<Unit, Unit>(dispatcher){
    override suspend fun execute(parameters: Unit) {
        userProfileRepository.sendSignOut()
    }
}