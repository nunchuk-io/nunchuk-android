package com.nunchuk.android.main.groupwallet

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.GroupSandbox
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class FinalizeGroupSandboxUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val nativeSdk: NunchukNativeSdk
) : UseCase<String, GroupSandbox>(dispatcher) {
    override suspend fun execute(parameters: String): GroupSandbox {
        return nativeSdk.finalizeGroup(parameters)
    }
}