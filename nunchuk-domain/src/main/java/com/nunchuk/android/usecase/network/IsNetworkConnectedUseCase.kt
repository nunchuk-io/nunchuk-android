package com.nunchuk.android.usecase.network

import com.nunchuk.android.repository.NetworkRepository
import javax.inject.Inject

class IsNetworkConnectedUseCase @Inject constructor(
    private val networkRepository: NetworkRepository
) {
    operator fun invoke(): Boolean {
        return networkRepository.isConnected()
    }
}