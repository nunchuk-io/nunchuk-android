package com.nunchuk.android.wallet.components.config

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.guestmode.SignInMode
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.messages.usecase.message.LeaveRoomUseCase
import com.nunchuk.android.model.Result
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.WalletExtended
import com.nunchuk.android.usecase.DeleteWalletUseCase
import com.nunchuk.android.usecase.GetUnusedSignerFromMasterSignerUseCase
import com.nunchuk.android.usecase.GetWalletUseCase
import com.nunchuk.android.usecase.UpdateWalletUseCase
import com.nunchuk.android.utils.onException
import com.nunchuk.android.wallet.components.config.WalletConfigEvent.UpdateNameErrorEvent
import com.nunchuk.android.wallet.components.config.WalletConfigEvent.UpdateNameSuccessEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class WalletConfigViewModel @Inject constructor(
    private val getWalletUseCase: GetWalletUseCase,
    private val updateWalletUseCase: UpdateWalletUseCase,
    private val deleteWalletUseCase: DeleteWalletUseCase,
    private val leaveRoomUseCase: LeaveRoomUseCase,
    private val accountManager: AccountManager,
    private val getUnusedSignerFromMasterSignerUseCase: GetUnusedSignerFromMasterSignerUseCase,
) : NunchukViewModel<WalletExtended, WalletConfigEvent>() {

    override val initialState = WalletExtended()

    lateinit var walletId: String

    fun init(walletId: String) {
        this.walletId = walletId
        getWalletDetails()
    }

    private fun getWalletDetails() {
        viewModelScope.launch {
            getWalletUseCase.execute(walletId)
                .flowOn(Dispatchers.IO)
                .onException { event(UpdateNameErrorEvent(it.message.orUnknownError())) }
                .flowOn(Dispatchers.Main)
                .collect { updateState { it } }
        }
    }

    fun handleEditCompleteEvent(walletName: String) {
        viewModelScope.launch {
            updateWalletUseCase.execute(getState().wallet.copy(name = walletName))
                .flowOn(Dispatchers.IO)
                .onException { event(UpdateNameErrorEvent(it.message.orUnknownError())) }
                .flowOn(Dispatchers.Main)
                .collect {
                    updateState { copy(wallet = wallet.copy(name = walletName)) }
                    event(UpdateNameSuccessEvent)
                }
        }
    }

    private fun showError(t: Throwable) {
        event(WalletConfigEvent.WalletDetailsError(t.message.orUnknownError()))
    }

    private suspend fun leaveRoom(onDone: suspend () -> Unit) {
        val roomId = getState().roomWallet?.roomId
        if (roomId == null) {
            onDone()
            return
        }
        leaveRoomUseCase.execute(roomId)
            .flowOn(Dispatchers.IO)
            .onException { e -> showError(e) }
            .collect {
                onDone()
            }
    }

    fun handleDeleteWallet() {
        viewModelScope.launch {
            leaveRoom {
                when (val event = deleteWalletUseCase.execute(walletId)) {
                    is Result.Success -> event(WalletConfigEvent.DeleteWalletSuccess)
                    is Result.Error -> showError(event.exception)
                }
            }
        }
    }

    fun isSharedWallet() = getState().isShared

    fun mapSigners(singleSigners: List<SingleSigner>): List<SignerModel> {
        return singleSigners.map { it.toModel(isPrimaryKey = isPrimaryKey(it.masterSignerId)) }
    }

    private fun isPrimaryKey(id: String) = accountManager.loginType() == SignInMode.PRIMARY_KEY.value && accountManager.getPrimaryKeyInfo()?.xfp == id
}