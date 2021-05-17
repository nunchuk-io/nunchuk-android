package com.nunchuk.android.wallet.details

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.Result.Error
import com.nunchuk.android.model.Result.Success
import com.nunchuk.android.usecase.GetWalletUseCase
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class WalletDetailsViewModel @Inject constructor(
    private val getWalletUseCase: GetWalletUseCase
) : NunchukViewModel<WalletDetailsState, WalletDetailsEvent>() {

    override val initialState = WalletDetailsState()

    lateinit var walletId: String

    fun init(walletId: String) {
        this.walletId = walletId
        getWalletDetails()
    }

    private fun getWalletDetails() {
        viewModelScope.launch {
            when (val result = getWalletUseCase.execute(walletId)) {
                is Success -> updateState { copy(wallet = result.data) }
                is Error -> event(WalletDetailsEvent.GetWalletDetailsError(result.exception.message.orUnknownError()))
            }
        }
    }

}