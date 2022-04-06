package com.nunchuk.android.core.profile

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface DeleteUserDevicesUseCase {
    fun execute(devices: List<String>): Flow<Unit>
}

internal class DeleteUserDevicesUseCaseImpl @Inject constructor(
    private val userProfileRepository: UserProfileRepository
) : DeleteUserDevicesUseCase {

    override fun execute(devices: List<String>) = userProfileRepository.deleteDevices(devices)

}