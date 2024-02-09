package com.nunchuk.android.main.membership.byzantine.payment.cosign

import androidx.lifecycle.ViewModel
import com.nunchuk.android.main.membership.model.toGroupWalletType
import com.nunchuk.android.model.byzantine.GroupWalletType
import com.nunchuk.android.usecase.byzantine.GetGroupUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class PaymentCosignViewModel @Inject constructor(
    private val getGroupUseCase: GetGroupUseCase
) : ViewModel() {
    suspend fun getGroupConfig(groupId: String): GroupWalletType? {
        return getGroupUseCase(GetGroupUseCase.Params(groupId))
            .map { it.getOrNull() }
            .firstOrNull()?.walletConfig?.toGroupWalletType()
    }
}