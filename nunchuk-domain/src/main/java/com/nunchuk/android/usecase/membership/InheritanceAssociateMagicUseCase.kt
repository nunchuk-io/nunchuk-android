package com.nunchuk.android.usecase.membership

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.inheritance.InheritancePlanBeneficiary
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class InheritanceAssociateMagicUseCase @Inject constructor(
    private val repository: PremiumWalletRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : UseCase<InheritanceAssociateMagicUseCase.Param, List<InheritancePlanBeneficiary>>(ioDispatcher) {

    override suspend fun execute(parameters: Param): List<InheritancePlanBeneficiary> {
        return repository.inheritanceAssociateMagic(
            walletId = parameters.walletId,
            groupId = parameters.groupId,
            beneficiaries = parameters.beneficiaries,
        )
    }

    data class Param(
        val walletId: String,
        val groupId: String?,
        val beneficiaries: List<InheritancePlanBeneficiary>,
    )
}
