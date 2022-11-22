package com.nunchuk.android.core.qr

import androidx.lifecycle.ViewModel
import com.nunchuk.android.usecase.GetWalletUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class DynamicQRCodeViewModel @Inject constructor(
    private val getWalletUseCase: GetWalletUseCase,
) : ViewModel() {
    fun getWalletName(walletId: String) = getWalletUseCase.execute(walletId).map { it.wallet.name }
}