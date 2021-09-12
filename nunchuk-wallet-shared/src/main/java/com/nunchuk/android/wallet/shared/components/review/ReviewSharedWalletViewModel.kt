package com.nunchuk.android.wallet.shared.components.review

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.usecase.InitWalletUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class ReviewSharedWalletViewModel @Inject constructor(
    private val initWalletUseCase: InitWalletUseCase
) : NunchukViewModel<ReviewSharedWalletState, ReviewSharedWalletEvent>() {

    override val initialState = ReviewSharedWalletState()

    fun init() {
        updateState { initialState }
    }

    fun handleContinueEvent(
        walletName: String,
        walletType: WalletType,
        addressType: AddressType,
        totalSigns: Int,
        requireSigns: Int
    ) {
        SessionHolder.currentRoom?.roomId?.let {
            initWallet(
                roomId = it,
                walletName = walletName,
                walletType = walletType,
                addressType = addressType,
                totalSigns = totalSigns,
                requireSigns = requireSigns
            )
        }
    }

    private fun initWallet(
        roomId: String,
        walletName: String,
        walletType: WalletType,
        addressType: AddressType,
        totalSigns: Int,
        requireSigns: Int
    ) {
        // FIXME swap requireSigns, totalSigns when nativesdk updated
        viewModelScope.launch {
            initWalletUseCase.execute(
                roomId = roomId,
                name = walletName,
                totalSigns = requireSigns,
                requireSigns = totalSigns,
                addressType = addressType,
                isEscrow = walletType == WalletType.ESCROW
            )
                .flowOn(Dispatchers.IO)
                .catch { event(ReviewSharedWalletEvent.InitWalletErrorEvent(it.message.orUnknownError())) }
                .flowOn(Dispatchers.Main)
                .collect { event(ReviewSharedWalletEvent.InitWalletCompletedEvent) }
        }
    }

}