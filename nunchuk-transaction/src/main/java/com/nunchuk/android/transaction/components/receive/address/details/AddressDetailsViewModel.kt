package com.nunchuk.android.transaction.components.receive.address.details

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.usecase.GetAddressPathUseCase
import com.nunchuk.android.usecase.GetWalletUseCase
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddressDetailsViewModel @Inject constructor(
    private val getAddressPathUseCase: GetAddressPathUseCase,
    private val getWalletUseCase: GetWalletUseCase,
) : NunchukViewModel<AddressDetailsState, Unit>() {

    override val initialState = AddressDetailsState()

    private lateinit var args: AddressDetailsArgs

    fun init(args: AddressDetailsArgs) {
        this.args = args
        viewModelScope.launch {
            getWalletUseCase.execute(args.walletId)
                .onException { }
                .collect {
                    if (isSingleSignWallet(it.wallet)) {
                        getDerivationPath()
                    }
                }
        }
    }

    private fun getDerivationPath() {
        viewModelScope.launch {
            getAddressPathUseCase(
                GetAddressPathUseCase.Params(
                    address = args.address,
                    walletId = args.walletId
                )
            ).onSuccess {
                updateState { copy(derivationPath = it) }
            }
        }
    }

    private fun isSingleSignWallet(wallet: Wallet): Boolean {
        val requireSigns = wallet.totalRequireSigns
        val totalSigns = wallet.signers.size
        return requireSigns == 1 && totalSigns == 1
    }
}

data class AddressDetailsState(
    val derivationPath: String = "",
)
