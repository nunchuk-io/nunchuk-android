package com.nunchuk.android.wallet.confirm

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.Result
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.usecase.CreateWalletUseCase
import com.nunchuk.android.wallet.confirm.WalletConfirmEvent.CreateWalletErrorEvent
import com.nunchuk.android.wallet.confirm.WalletConfirmEvent.CreateWalletSuccessEvent
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class WalletConfirmViewModel @Inject constructor(
    private val createWalletUseCase: CreateWalletUseCase
) : NunchukViewModel<Unit, WalletConfirmEvent>() {

    override val initialState = Unit

    fun handleContinueEvent(walletName: String, walletType: WalletType, addressType: AddressType, totalRequireSigns: Int, signers: List<SingleSigner>) {
        viewModelScope.launch {
            val result = createWalletUseCase.execute(
                name = walletName,
                totalRequireSigns = totalRequireSigns,
                signers = signers,
                addressType = addressType,
                isEscrow = walletType == WalletType.ESCROW
            )
            if (result is Result.Success) {
                event(CreateWalletSuccessEvent)
            } else if (result is Result.Error) {
                event(CreateWalletErrorEvent(result.exception.message.orUnknownError()))
            }
        }
    }

}