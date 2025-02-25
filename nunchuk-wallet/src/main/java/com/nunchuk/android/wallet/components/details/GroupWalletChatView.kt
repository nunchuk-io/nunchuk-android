package com.nunchuk.android.wallet.components.details

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcExpandableTextInline
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NcTextField
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.border
import com.nunchuk.android.compose.controlFillPrimary
import com.nunchuk.android.compose.lightGray
import com.nunchuk.android.compose.strokePrimary
import com.nunchuk.android.compose.textSecondary
import com.nunchuk.android.model.FreeGroupMessage
import com.nunchuk.android.wallet.R

@Composable
fun GroupWalletChatView(
    messages: List<FreeGroupMessage> = emptyList(),
    unreadCount: Int = 0,
    onSendMessage: (String) -> Unit = {},
    onOpenChat: () -> Unit = {}
) {

    var isChatExpanded by remember { mutableStateOf(true) }

    NunchukTheme {
        Column(
            modifier = Modifier
                .wrapContentHeight()
                .clickable {
                    isChatExpanded = !isChatExpanded
                }
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.strokePrimary,
                    RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                )
                .background(
                    MaterialTheme.colorScheme.lightGray,
                    RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                )
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp)

        ) {
            ChatHeader(
                unreadCount = unreadCount,
                isChatExpanded = isChatExpanded,
                onCollapseExpand = {
                    isChatExpanded = !isChatExpanded
                },
                onOpenChat = onOpenChat
            )

            if (isChatExpanded) {
                ChatMessages(messages = messages.take(2))

                ChatInput(onSendMessage = { message ->
                   onSendMessage(message)
                })
            }
        }
    }
}

@Composable
fun ChatHeader(
    unreadCount: Int = 0,
    isChatExpanded: Boolean = true,
    onCollapseExpand: () -> Unit = {},
    onOpenChat: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Chat",
            style = NunchukTheme.typography.titleSmall,
        )
        Spacer(modifier = Modifier.width(8.dp))
        if ( unreadCount > 0) {
            Box(
                modifier = Modifier
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.border,
                        shape = RoundedCornerShape(20.dp),
                    )
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = unreadCount.toString(),
                    style = NunchukTheme.typography.bodySmall
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))

        NcIcon(
            painter = painterResource(id = R.drawable.ic_expand_chat),
            contentDescription = "Expand",
            tint = MaterialTheme.colorScheme.controlFillPrimary,
            modifier = Modifier.size(20.dp)
                .clickable {
                    onOpenChat()
                }
        )

        NcIcon(
            painter = if (isChatExpanded) painterResource(id = R.drawable.ic_arrow_chat_down) else painterResource(id = R.drawable.ic_arrow_chat_up),
            contentDescription = "Expand",
            tint = MaterialTheme.colorScheme.controlFillPrimary,
            modifier = Modifier
                .padding(start = 16.dp)
                .size(20.dp)
                .clickable { onCollapseExpand() }
        )
    }
}

@Composable
fun ChatMessages(messages: List<FreeGroupMessage>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth(),
        reverseLayout = true
    ) {
        items(messages) { message ->
            ChatBubble(message.content)
        }
    }
}

@Composable
fun ChatBubble(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(Color.White, CircleShape)
                .padding(7.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_user),
                contentDescription = "User",
                tint = Color.Black,
                modifier = Modifier.size(14.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))
        NcExpandableTextInline(
            text = message,
            style = NunchukTheme.typography.bodySmall,
            collapsedMaxLine = 2,
        )
    }
}

@Composable
fun ChatInput(onSendMessage: (String) -> Unit) {
    var inputText by remember { mutableStateOf("") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NcTextField(
            modifier = Modifier
                .weight(1f),
            title = "",
            value = inputText,
            onValueChange = { inputText = it },
            placeholder = {
                Text(
                    text = "Type a message",
                    style = NunchukTheme.typography.bodySmall,
                )
            },
            inputBoxHeight = 45.dp,
            roundBoxRadius = 8.dp
        )
        Spacer(modifier = Modifier.width(16.dp))

        Icon(
            painter = painterResource(id = R.drawable.ic_send_disabled),
            modifier = Modifier
                .size(24.dp)
                .clickable {
                    if (inputText.isNotBlank()) {
                        onSendMessage(inputText)
                        inputText = ""
                    }
                },
            contentDescription = "Send",
            tint = MaterialTheme.colorScheme.textSecondary
        )
    }
}

@PreviewLightDark
@Composable
fun GroupWalletChatViewPreview() {
    GroupWalletChatView()
}