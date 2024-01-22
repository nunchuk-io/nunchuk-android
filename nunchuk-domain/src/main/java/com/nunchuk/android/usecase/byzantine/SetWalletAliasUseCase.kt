package com.nunchuk.android.usecase.byzantine

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.repository.GroupWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class SetWalletAliasUseCase @Inject constructor(
    private val repository: GroupWalletRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : UseCase<SetWalletAliasUseCase.Params, Unit>(ioDispatcher) {

    override suspend fun execute(parameters: Params) {
        return repository.setWalletAlias(
            groupId = parameters.groupId,
            walletId = parameters.walletId,
            alias = parameters.alias
        )
    }

    data class Params(
        val groupId: String,
        val walletId: String,
        val alias: String,
    )
}