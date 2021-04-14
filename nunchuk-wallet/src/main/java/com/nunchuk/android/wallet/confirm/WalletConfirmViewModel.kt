package com.nunchuk.android.wallet.confirm

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.Result
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.usecase.CreateWalletUseCase
import com.nunchuk.android.usecase.DraftWalletUseCase
import com.nunchuk.android.wallet.confirm.WalletConfirmEvent.*
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class WalletConfirmViewModel @Inject constructor(
    private val draftWalletUseCase: DraftWalletUseCase,
    private val createWalletUseCase: CreateWalletUseCase
) : NunchukViewModel<Unit, WalletConfirmEvent>() {

    override val initialState = Unit
    private var descriptor = ""

    fun handleContinueEvent(walletName: String, walletType: WalletType, addressType: AddressType, totalRequireSigns: Int, signers: List<SingleSigner>) {
        event(SetLoadingEvent(true))
        viewModelScope.launch {
            draftWallet(walletName, totalRequireSigns, signers, addressType, walletType)
        }
    }

    private suspend fun draftWallet(
        walletName: String,
        totalRequireSigns: Int,
        signers: List<SingleSigner>,
        addressType: AddressType,
        walletType: WalletType
    ) {
        val result = draftWalletUseCase.execute(
            name = walletName,
            totalRequireSigns = totalRequireSigns,
            signers = signers,
            addressType = addressType,
            isEscrow = walletType == WalletType.ESCROW
        )
        when (result) {
            is Result.Success -> {
                descriptor = result.data
                createWallet(walletName, totalRequireSigns, signers, addressType, walletType)
            }
            is Result.Error -> {
                event(CreateWalletErrorEvent(result.exception.message.orUnknownError()))
                event(SetLoadingEvent(false))
            }
        }
    }

    private suspend fun createWallet(
        walletName: String,
        totalRequireSigns: Int,
        signers: List<SingleSigner>,
        addressType: AddressType,
        walletType: WalletType
    ) {
        val result = createWalletUseCase.execute(
            name = walletName,
            totalRequireSigns = totalRequireSigns,
            signers = signers,
            addressType = addressType,
            isEscrow = walletType == WalletType.ESCROW
        )
        when (result) {
            is Result.Success -> event(CreateWalletSuccessEvent(descriptor))
            is Result.Error -> {
                event(CreateWalletErrorEvent(result.exception.message.orUnknownError()))
                event(SetLoadingEvent(false))
            }
        }
    }

}