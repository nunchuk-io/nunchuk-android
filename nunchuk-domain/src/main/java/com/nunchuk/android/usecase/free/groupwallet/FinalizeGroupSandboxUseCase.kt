package com.nunchuk.android.usecase.free.groupwallet

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.GroupSandbox
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class FinalizeGroupSandboxUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val nativeSdk: NunchukNativeSdk
) : UseCase<FinalizeGroupSandboxUseCase.Params, GroupSandbox>(dispatcher) {
    override suspend fun execute(parameters: Params): GroupSandbox {
        return nativeSdk.finalizeGroup(parameters.groupId, parameters.signerIndexes)
    }

    /**
     * Use case parameters
     *
     * @param groupId Group ID
     * @param signerIndexes Signer indexes for taproot
     */
    data class Params(
        val groupId: String,
        val signerIndexes: Set<Int> = emptySet()
    )
}