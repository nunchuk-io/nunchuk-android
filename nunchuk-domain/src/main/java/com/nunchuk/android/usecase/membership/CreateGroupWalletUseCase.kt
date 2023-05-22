package com.nunchuk.android.usecase.membership

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.ByzantineGroup
import com.nunchuk.android.model.byzantine.AssistedMember
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class CreateGroupWalletUseCase @Inject constructor(
    private val repository: PremiumWalletRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : UseCase<CreateGroupWalletUseCase.Param, ByzantineGroup>(ioDispatcher) {

    override suspend fun execute(parameters: Param): ByzantineGroup {
        return repository.createGroupWallet(
            parameters.m,
            parameters.n,
            parameters.requiredServerKey,
            parameters.allowInheritance,
            parameters.setupPreference,
            parameters.members)
    }

    class Param(
        val m: Int,
        val n: Int,
        val requiredServerKey: Boolean,
        val allowInheritance: Boolean,
        val setupPreference: String,
        val members: List<AssistedMember>
    )
}