package com.nunchuk.android.usecase.byzantine

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.byzantine.DraftWallet
import com.nunchuk.android.repository.GroupWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class ReuseGroupWalletUseCase @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val groupWalletRepository: GroupWalletRepository
) : UseCase<ReuseGroupWalletUseCase.Params, DraftWallet>(ioDispatcher) {

    override suspend fun execute(parameters: Params): DraftWallet =
        groupWalletRepository.reuseGroupWallet(
            groupId = parameters.groupId,
            fromGroupId = parameters.fromGroupId
        )

    data class Params(
        val groupId: String,
        val fromGroupId: String,
    )
}