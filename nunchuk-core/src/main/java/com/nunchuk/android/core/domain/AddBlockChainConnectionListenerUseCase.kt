package com.nunchuk.android.core.domain

import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface AddBlockChainConnectionListenerUseCase {
    fun execute(): Flow<Unit>
}

internal class AddBlockChainConnectionListenerUseCaseImpl @Inject constructor(
    private val nunchukNativeSdk: NunchukNativeSdk
) : AddBlockChainConnectionListenerUseCase {

    override fun execute() = flow {
        nunchukNativeSdk.addBlockchainConnectionListener()
        emit(Unit)
    }
}