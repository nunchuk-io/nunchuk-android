package com.nunchuk.android.messages.components.freegroup

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.domain.GetGroupDeviceUIDUseCase
import com.nunchuk.android.core.domain.GetListMessageFreeGroupWalletUseCase
import com.nunchuk.android.listener.GroupMessageListener
import com.nunchuk.android.messages.util.GroupChatManager
import com.nunchuk.android.model.FreeGroupMessage
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.usecase.wallet.GetWalletDetail2UseCase
import com.nunchuk.android.utils.simpleDateFormat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class FreeGroupWalletChatViewModel @Inject constructor(
    private val getListMessageFreeGroupWalletUseCase: GetListMessageFreeGroupWalletUseCase,
    private val getWalletDetail2UseCase: GetWalletDetail2UseCase,
    private val groupChatManager: GroupChatManager,
    private val getGroupDeviceUIDUseCase: GetGroupDeviceUIDUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val walletId =
        savedStateHandle.get<String>(FreeGroupWalletChatActivity.EXTRA_WALLET_ID).orEmpty()

    private val _uiState = MutableStateFlow(FreeGroupWalletChatUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            GroupMessageListener.getMessageFlow().collect { message ->
                Log.e("group-wallet", "message: $message")
                if (message.walletId == walletId) {
                    val newMessages = _uiState.value.messages + message
                    _uiState.update {
                        it.copy(
                            messages = newMessages,
                            messageUis = newMessages.groupByDate(_uiState.value.uid)
                        )
                    }
                }
            }
        }

        viewModelScope.launch {
            groupChatManager.init(walletId)
        }

        viewModelScope.launch {
            val uid = getGroupDeviceUIDUseCase(Unit).getOrNull()
            _uiState.update {
                it.copy(uid = uid.orEmpty())
            }
            getListMessage()
            getWalletDetail()
        }
    }

    private fun getWalletDetail() {
        if (walletId.isEmpty()) return
        viewModelScope.launch {
            getWalletDetail2UseCase(walletId).onSuccess { wallet ->
                _uiState.update {
                    it.copy(wallet = wallet)
                }
            }.onFailure {
                // Handle failure
            }
        }
    }

    private fun getListMessage() {
        if (walletId.isEmpty()) return
        viewModelScope.launch {
            getListMessageFreeGroupWalletUseCase(
                GetListMessageFreeGroupWalletUseCase.Param(
                    walletId = walletId,
                )
            ).onSuccess { result ->
                _uiState.update {
                    it.copy(
                        messages = result,
                        messageUis = result.groupByDate(_uiState.value.uid)
                    )
                }
            }.onFailure {
                // Handle failure
            }
        }
    }

    fun sendMessage(message: String) = viewModelScope.launch {
        groupChatManager.sendMessage(message, walletId)
    }
}

fun generateSampleFreeGroupMessages(): List<FreeGroupMessage> {
    val messages = mutableListOf<FreeGroupMessage>()
    for (i in 1..5) {
        messages.add(
            FreeGroupMessage(
                id = i.toString(),
                sender = if (i % 2 == 0) "senderId" else "receiverId",
                content = "Sample message $i",
                signer = "AAA",
                walletId = "walletId",
                timestamp = Date().time - 86400000
            )
        )
    }
    for (i in 6..10) {
        messages.add(
            FreeGroupMessage(
                id = i.toString(),
                sender = if (i % 2 == 0) "senderId" else "receiverId",
                content = "Sample message $i",
                signer = "AAA",
                walletId = "walletId",
                timestamp = Date().time
            )
        )
    }
    return messages
}

data class FreeGroupWalletChatUiState(
    val messages: List<FreeGroupMessage> = emptyList(),
    val messageUis: List<MessageUI> = emptyList(),
    val wallet: Wallet? = null,
    val uid: String = ""
)

sealed class MessageUI {
    data class TimeMessage(val date: String) : MessageUI()
    data class SenderMessage(val data: FreeGroupMessage) : MessageUI()
    data class ReceiverMessage(val data: FreeGroupMessage) : MessageUI()
}

internal fun List<FreeGroupMessage>.groupByDate(curUserId: String): List<MessageUI> {
    val grouping: LinkedHashMap<String, Set<FreeGroupMessage>> = LinkedHashMap()
    var messages: MutableSet<FreeGroupMessage>
    for (model in this) {
        val hashMapKey: String = Date(model.timestamp * 1000).simpleDateFormat()
        if (grouping.containsKey(hashMapKey)) {
            val set = grouping[hashMapKey]!!
            (set as MutableSet).add(model)
        } else {
            messages = LinkedHashSet()
            messages.add(model)
            grouping[hashMapKey] = messages
        }
    }
    return grouping.groupByDate(curUserId)
}

internal fun LinkedHashMap<String, Set<FreeGroupMessage>>.groupByDate(curUserId: String): List<MessageUI> {
    val models = ArrayList<MessageUI>()
    for (date in keys) {
        val dateItem = MessageUI.TimeMessage(date)
        this[date]!!.mapTo(models) {
            if (it.sender == curUserId) {
                MessageUI.SenderMessage(it)
            } else {
                MessageUI.ReceiverMessage(it)
            }
        }
        models.add(dateItem)
    }
    return models
}