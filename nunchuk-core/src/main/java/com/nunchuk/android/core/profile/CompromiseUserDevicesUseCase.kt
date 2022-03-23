package com.nunchuk.android.core.profile

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface CompromiseUserDevicesUseCase {
    fun execute(devices: List<String>): Flow<Unit>
}

internal class CompromiseUserDevicesUseCaseImpl @Inject constructor(
    private val userProfileRepository: UserProfileRepository
) : CompromiseUserDevicesUseCase {

    override fun execute(devices: List<String>) = userProfileRepository.compromiseDevices(devices)

}