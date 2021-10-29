package com.nunchuk.android.wallet.shared.components.config

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.usecase.CreateSharedWalletUseCase
import com.nunchuk.android.usecase.GetRoomWalletUseCase
import com.nunchuk.android.utils.onException
import com.nunchuk.android.wallet.shared.components.config.SharedWalletConfigEvent.CreateSharedWalletSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.room.members.RoomMemberQueryParams
import org.matrix.android.sdk.api.session.room.model.RoomMemberSummary
import javax.inject.Inject

internal class SharedWalletConfigViewModel @Inject constructor(
    private val createSharedWalletUseCase: CreateSharedWalletUseCase,
    private val getRoomWalletUseCase: GetRoomWalletUseCase
) : NunchukViewModel<SharedWalletConfigState, SharedWalletConfigEvent>() {

    override val initialState = SharedWalletConfigState()

    init {
        if (SessionHolder.hasActiveRoom()) {
            val currentRoom = SessionHolder.currentRoom!!
            val roomMembers = currentRoom.getRoomMembers(RoomMemberQueryParams.Builder().build())
            updateState { copy(signerModels = roomMembers.toSignerModels()) }
            getRoomWallet(currentRoom.roomId)
        }
    }

    private fun getRoomWallet(roomId: String) {
        viewModelScope.launch {
            getRoomWalletUseCase.execute(roomId)
                .flowOn(Dispatchers.IO)
                .onException { }
                .flowOn(Dispatchers.Main)
                .collect { updateState { copy(roomWallet = it) } }
        }
    }

    fun finalizeWallet() {
        viewModelScope.launch {
            val roomId = SessionHolder.currentRoom!!.roomId
            createSharedWalletUseCase.execute(roomId)
                .flowOn(Dispatchers.IO)
                .onException { }
                .collect {
                    getRoomWallet(roomId)
                    event(CreateSharedWalletSuccess)
                }
        }
    }

}

private fun List<RoomMemberSummary>.toSignerModels() = map(RoomMemberSummary::toSignerModel)

private fun RoomMemberSummary.toSignerModel() = SignerModel(
    id = userId,
    name = displayName ?: userId,
    fingerPrint = "",
    derivationPath = ""
)