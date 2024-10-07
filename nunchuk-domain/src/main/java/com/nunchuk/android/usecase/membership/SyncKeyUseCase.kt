package com.nunchuk.android.usecase.membership

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.MembershipStep
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class SyncKeyUseCase @Inject constructor(
    private val repository: PremiumWalletRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : UseCase<SyncKeyUseCase.Param, Unit>(ioDispatcher) {

    override suspend fun execute(parameters: Param) {
        return repository.syncKey(
            parameters.groupId,
            parameters.step,
            parameters.signer,
        )
    }

    data class Param(
        val groupId: String = "",
        val step: MembershipStep,
        val signer: SingleSigner,
    )
}