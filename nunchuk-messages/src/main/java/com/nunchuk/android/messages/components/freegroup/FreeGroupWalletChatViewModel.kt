package com.nunchuk.android.messages.components.freegroup

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.domain.GetListMessageFreeGroupWalletUseCase
import com.nunchuk.android.core.domain.SendMessageFreeGroupWalletUseCase
import com.nunchuk.android.listener.GroupMessageListener
import com.nunchuk.android.model.FreeGroupMessage
import com.nunchuk.android.model.GroupSandbox
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.usecase.free.groupwallet.GetGroupWalletsUseCase
import com.nunchuk.android.usecase.wallet.GetWalletDetail2UseCase
import com.nunchuk.android.utils.simpleDateFormat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class FreeGroupWalletChatViewModel @Inject constructor(
    private val getListMessageFreeGroupWalletUseCase: GetListMessageFreeGroupWalletUseCase,
    private val accountManager: AccountManager,
    private val sendMessageFreeGroupWalletUseCase: SendMessageFreeGroupWalletUseCase,
    private val getGroupWalletsUseCase: GetGroupWalletsUseCase,
    private val getWalletDetail2UseCase: GetWalletDetail2UseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val walletId =
        savedStateHandle.get<String>(FreeGroupWalletChatActivity.EXTRA_WALLET_ID).orEmpty()

    private val _uiState = MutableStateFlow(FreeGroupWalletChatUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getGroupWalletsUseCase(Unit).onSuccess { wallets ->
                val wallet = wallets.firstOrNull { it.id == walletId } ?: return@onSuccess
                val signer = wallet.signers.map {
                    it.takeIf { it.masterFingerprint.isNotEmpty() }
                }.firstOrNull()

                _uiState.update {
                    it.copy(
                        wallet = wallet,
                        singleSigner = signer,
                    )
                }
                getListMessage()
                getWalletDetail()
            }
        }
        viewModelScope.launch {
            GroupMessageListener.getMessageFlow().collect { message ->
                if (message.walletId == walletId) {
                    val newMessages = _uiState.value.messages + message
                    _uiState.update {
                        it.copy(
                            messages = newMessages,
                            messageUis = newMessages.groupByDate(accountManager.getAccount().id)
                        )
                    }
                }
            }
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
            ).onSuccess {
//                _uiState.update {
//                    it.copy(
//                        messages = it.messages,
//                        messageUis = it.messages.groupByDate(accountManager.getAccount().id)
//                    )
//                }
            }.onFailure {
                // Handle failure
            }
        }
    }

    fun sendMessage(message: String) {
        if (_uiState.value.singleSigner == null || message.isEmpty()) return
        viewModelScope.launch {
            sendMessageFreeGroupWalletUseCase(
                SendMessageFreeGroupWalletUseCase.Param(
                    message = message,
                    walletId = walletId,
                    singleSigner = _uiState.value.singleSigner!!
                )
            ).onSuccess {

            }.onFailure {
            }
        }
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
    val singleSigner: SingleSigner? = null,
    val wallet: Wallet? = null,
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
        val hashMapKey: String = Date(model.timestamp).simpleDateFormat()
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
        models.add(dateItem)
        this[date]!!.mapTo(models) {
            if (it.sender == curUserId) {
                MessageUI.SenderMessage(it)
            } else {
                MessageUI.ReceiverMessage(it)
            }
        }
    }
    return models.reversed()
}