package com.nunchuk.android.core.domain

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.GroupPlatformKeyPolicies
import com.nunchuk.android.model.GroupPlatformKeyPolicyUpdateRequirement
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class PreviewGroupPlatformKeyPolicyUpdateUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val nativeSdk: NunchukNativeSdk
) : UseCase<PreviewGroupPlatformKeyPolicyUpdateUseCase.Params, GroupPlatformKeyPolicyUpdateRequirement>(dispatcher) {
    override suspend fun execute(parameters: Params): GroupPlatformKeyPolicyUpdateRequirement {
        return nativeSdk.previewGroupPlatformKeyPolicyUpdate(
            walletId = parameters.walletId,
            policies = parameters.policies,
        )
    }

    data class Params(
        val walletId: String,
        val policies: GroupPlatformKeyPolicies,
    )
}
