package com.nunchuk.android.messages.components.freegroup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.GetListMessageFreeGroupWalletUseCase
import com.nunchuk.android.messages.components.detail.AbsChatModel
import com.nunchuk.android.messages.components.detail.DateModel
import com.nunchuk.android.messages.components.detail.Message
import com.nunchuk.android.model.FreeGroupMessage
import com.nunchuk.android.utils.simpleDateFormat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.ArrayList
import java.util.Date
import java.util.LinkedHashMap
import java.util.LinkedHashSet
import javax.inject.Inject

@HiltViewModel
class FreeGroupWalletChatViewModel @Inject constructor(
    private val getListMessageFreeGroupWalletUseCase: GetListMessageFreeGroupWalletUseCase
) : ViewModel() {

    private val _uiState= MutableStateFlow(FreeGroupWalletChatUiState())
    val uiState = _uiState.asStateFlow()

    init {
        getListMessage()
    }

    private fun getListMessage() {
        viewModelScope.launch {
            val messages = generateSampleFreeGroupMessages()
            _uiState.update {
                it.copy(messages = messages, messageUis = messages.groupByDate())
            }
//            getListMessageFreeGroupWalletUseCase(
//                GetListMessageFreeGroupWalletUseCase.Param(
//                    walletId = "walletId",
//                )
//            ).onSuccess {
//                _uiState.update {
//                    it.copy(messages = it.messages, messageUis = it.messages.groupByDate())
//                }
//            }.onFailure {
//                // Handle failure
//            }
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
    val messageUis: List<MessageUI> = emptyList()
)

sealed class MessageUI {
    data class TimeMessage(val date: String) : MessageUI()
    data class SenderMessage(val data: FreeGroupMessage) : MessageUI()
    data class ReceiverMessage(val data: FreeGroupMessage) : MessageUI()
}

internal fun List<FreeGroupMessage>.groupByDate(): List<MessageUI> {
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
    return grouping.groupByDate()
}

internal fun LinkedHashMap<String, Set<FreeGroupMessage>>.groupByDate(): List<MessageUI> {
    val models = ArrayList<MessageUI>()
    for (date in keys) {
        val dateItem = MessageUI.TimeMessage(date)
        models.add(dateItem)
        this[date]!!.mapTo(models) {
            if (it.sender == "senderId") {
                MessageUI.SenderMessage(it)
            } else {
                MessageUI.ReceiverMessage(it)
            }
        }
    }
    return models.reversed()
}