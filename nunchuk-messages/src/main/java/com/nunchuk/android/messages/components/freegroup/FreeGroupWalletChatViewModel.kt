package com.nunchuk.android.messages.components.freegroup

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.domain.GetListMessageFreeGroupWalletUseCase
import com.nunchuk.android.core.domain.SendMessageFreeGroupWalletUseCase
import com.nunchuk.android.model.FreeGroupMessage
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.usecase.free.groupwallet.GetGroupSandboxUseCase
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
    private val accountManager: AccountManager,
    private val sendMessageFreeGroupWalletUseCase: SendMessageFreeGroupWalletUseCase,
    private val getGroupSandboxUseCase: GetGroupSandboxUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val groupId =
        savedStateHandle.get<String>(FreeGroupWalletChatActivity.EXTRA_GROUP_ID).orEmpty()

    private val _uiState = MutableStateFlow(FreeGroupWalletChatUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getGroupSandboxUseCase(groupId).onSuccess { group ->
                val signer = group.signers.map {
                    it.takeIf { it.masterFingerprint.isNotEmpty() }
                }.firstOrNull()
                _uiState.update {
                    it.copy(
                        walletId = group.walletId,
                        singleSigner = signer,
                    )
                }
                getListMessage(group.walletId)
            }
        }
        getListMessage("group.walletId") // remove this line
    }

    private fun getListMessage(walletId: String) {
//        if (walletId.isEmpty()) return
        viewModelScope.launch {
            val messages = generateSampleFreeGroupMessages()
            _uiState.update {
                it.copy(
                    messages = messages,
                    messageUis = messages.groupByDate(accountManager.getAccount().id)
                )
            }
//            getListMessageFreeGroupWalletUseCase(
//                GetListMessageFreeGroupWalletUseCase.Param(
//                    walletId = walletId,
//                )
//            ).onSuccess {
//                _uiState.update {
//                    it.copy(
//                        messages = it.messages,
//                        messageUis = it.messages.groupByDate(accountManager.getAccount().id)
//                    )
//                }
//            }.onFailure {
//                // Handle failure
//            }
        }
    }

    fun sendMessage(message: String) {
        if (_uiState.value.singleSigner == null || message.isEmpty()) return
        viewModelScope.launch {
            sendMessageFreeGroupWalletUseCase(
                SendMessageFreeGroupWalletUseCase.Param(
                    message = message,
                    walletId = _uiState.value.walletId,
                    singleSigner = _uiState.value.singleSigner!!
                )
            ).onSuccess {
                // Handle success
            }.onFailure {
                // Handle failure
            }
        }
    }
}

fun generateSampleFreeGroupMessages(): List<FreeGroupMessage> {
    val messages = mutableListOf<FreeGroupMessage>()
    for (i in 1..30) {
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
    val walletId: String = ""
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