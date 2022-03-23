package com.nunchuk.android.core.profile

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface GetUserDevicesUseCase {
    fun execute(): Flow<List<UserDeviceResponse>>
}

internal class GetUserDevicesUseCaseImpl @Inject constructor(
    private val userProfileRepository: UserProfileRepository
) : GetUserDevicesUseCase {

    override fun execute() = userProfileRepository.getUserDevices()

}