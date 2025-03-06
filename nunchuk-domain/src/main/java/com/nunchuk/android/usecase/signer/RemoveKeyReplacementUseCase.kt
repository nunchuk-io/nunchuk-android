package com.nunchuk.android.usecase.signer

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class RemoveKeyReplacementUseCase @Inject constructor(
    private val repository: PremiumWalletRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : UseCase<RemoveKeyReplacementUseCase.Params, Unit>(ioDispatcher) {

    override suspend fun execute(parameters: Params) {
        repository.removeKeyReplacement(parameters.groupId, parameters.walletId, parameters.xfp)
    }

    class Params(val groupId: String?, val walletId: String, val xfp: String)
}
