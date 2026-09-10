package com.nunchuk.android.usecase.membership

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.inheritance.ClaimOption
import com.nunchuk.android.repository.KeyRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

/**
 * Records how the owner will pass an off-chain inheritance key to their Beneficiary — the
 * distribution choice. Sending both options is "do both"; there is no separate value for it.
 *
 * Dropping [ClaimOption.ENCRYPTED_BACKUP] from an existing selection deletes the uploaded backup and
 * its verification on the server and cannot be undone, so the UI must confirm first.
 */
class SetInheritanceClaimOptionsUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val repository: KeyRepository,
) : UseCase<SetInheritanceClaimOptionsUseCase.Param, Unit>(dispatcher) {

    override suspend fun execute(parameters: Param) {
        repository.setInheritanceClaimOptions(
            groupId = parameters.groupId,
            walletId = parameters.walletId,
            xfp = parameters.xfp,
            claimOptions = parameters.claimOptions,
        )
    }

    /** [walletId] empty targets the draft wallet, otherwise the replacement on that wallet. */
    data class Param(
        val xfp: String,
        val claimOptions: List<ClaimOption>,
        val groupId: String = "",
        val walletId: String = "",
    )
}
