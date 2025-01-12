package com.nunchuk.android.messages.components.freegroup

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NcTextField
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.border
import com.nunchuk.android.compose.lightGray
import com.nunchuk.android.compose.textPrimary
import com.nunchuk.android.compose.textSecondary
import com.nunchuk.android.core.base.BaseComposeActivity
import com.nunchuk.android.messages.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FreeGroupWalletChatActivity : BaseComposeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                FreeGroupWalletChatScreen()
            }
        })
    }

    companion object {
        const val TAG = "FreeGroupWalletChatActivity"
        fun start(activity: Context) {
            activity.startActivity(Intent(activity, FreeGroupWalletChatActivity::class.java))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FreeGroupWalletChatScreen() {
    NunchukTheme {
        Scaffold(
            modifier = Modifier.navigationBarsPadding(),
            topBar = {
                val onBackPressOwner = LocalOnBackPressedDispatcherOwner.current
                CenterAlignedTopAppBar(
                    modifier = Modifier,
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                    navigationIcon = {
                        IconButton(onClick = { onBackPressOwner?.onBackPressedDispatcher?.onBackPressed() }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_back),
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.textPrimary
                            )
                        }
                    },

                    title = {
                        Column {
                            Text(
                                text = "Group wallet",
                                style = NunchukTheme.typography.titleLarge,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth(),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                NcIcon(
                                    painter = painterResource(id = R.drawable.ic_encrypted),
                                    contentDescription = "Encrypted icon",
                                    tint = colorResource(id = R.color.nc_text_secondary)
                                )
                                Text(
                                    text = stringResource(id = R.string.nc_encrypted),
                                    style = NunchukTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.textSecondary),
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }

                    },
                    actions = {
                        Spacer(modifier = Modifier.size(LocalViewConfiguration.current.minimumTouchTargetSize))
                    }
                )
            }, bottomBar = {
                ChatInput(onSendMessage = { message ->
                    println("Message sent: $message") // Replace with real logic
                })
            }) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .navigationBarsPadding()
                    .fillMaxHeight()
            ) {
                // Header Section
                ChatHeader(walletName = "1.00000001 BTC", subtext = "2/3 Multisig")

                // Message List Section
                ChatMessages(
                    messages = listOf(
                        Message(
                            text = "Hey everyone, I’m in! Who’s next to add their key?",
                            isSent = true,
                            time = "7:14 PM",
                            delivered = true
                        ),
                        Message(
                            text = "Sample Message from the sender",
                            isSent = false,
                            senderId = "352B3521"
                        ),
                        Message(
                            text = "Same here, just added my key. Let me know if it’s all set.",
                            isSent = false,
                            senderId = "F42B3121"
                        ),
                        Message(
                            text = "Same here, just added my key. Let me know if it’s all set.",
                            isSent = false,
                            senderId = "F42B3121"
                        ),
                        Message(
                            text = "Same here, just added my key. Let me know if it’s all set.",
                            isSent = false,
                            senderId = "F42B3121"
                        ),
                        Message(
                            text = "Same here, just added my key. Let me know if it’s all set.",
                            isSent = false,
                            senderId = "F42B3121"
                        ),
                        Message(
                            text = "Same here, just added my key. Let me know if it’s all set.",
                            isSent = false,
                            senderId = "F42B3121"
                        ),
                        Message(
                            text = "Same here, just added my key. Let me know if it’s all set.",
                            isSent = false,
                            senderId = "F42B3121"
                        ),
                        Message(
                            text = "Same here, just added my key. Let me know if it’s all set.",
                            isSent = false,
                            senderId = "F42B3121"
                        ),
                        Message(
                            text = "Same here, just added my key. Let me know if it’s all set.",
                            isSent = false,
                            senderId = "F42B3121"
                        ),
                        Message(
                            text = "Same here, just added my key. Let me know if it’s all set.",
                            isSent = false,
                            senderId = "F42B3121"
                        ),
                        Message(
                            text = "Same here, just added my key. Let me know if it’s all set.",
                            isSent = false,
                            senderId = "F42B3121"
                        )
                    )
                )
            }
        }
    }
}


@Composable
fun ChatHeader(walletName: String, subtext: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.lightGray)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_wallet_info), // Replace with wallet icon
            contentDescription = null,
            tint = Color.Black,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = walletName,
                style = NunchukTheme.typography.title,
            )
            Text(
                text = subtext,
                style = NunchukTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.textSecondary),
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            painter = painterResource(id = R.drawable.ic_arrow_right_new),
            contentDescription = "Forward",
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun ChatMessages(messages: List<Message>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        reverseLayout = true,
    ) {
        items(messages) { message ->
            if (message.isSent) {
                SentMessageBubble(message)
            } else {
                ReceivedMessageBubble(message)
            }

            message.time?.let {
                // Timestamp between messages
                Text(
                    text = it,
                    style = NunchukTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun SentMessageBubble(message: Message) {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val maxWidth = (screenWidth * 0.7f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.End
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = maxWidth)
                .background(
                    Color(0xFFD0E6FF),
                    RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
                )
                .padding(12.dp)
        ) {
            Text(
                text = message.text,
                style = NunchukTheme.typography.body,
                color = Color.Black
            )
        }
        if (message.delivered) {
            Text(
                text = "Delivered",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
fun ReceivedMessageBubble(message: Message) {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val maxWidth = (screenWidth * 0.7f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = message.senderId ?: "",
            style = NunchukTheme.typography.caption.copy(color = MaterialTheme.colorScheme.textSecondary),
            modifier = Modifier.padding(bottom = 2.dp, start = 50.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.Bottom
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

            Spacer(modifier = Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .widthIn(max = maxWidth)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.border,
                        shape = RoundedCornerShape(
                            topEnd = 24.dp,
                            bottomStart = 24.dp,
                            bottomEnd = 24.dp
                        )
                    )
                    .padding(12.dp)
            ) {
                Text(
                    text = message.text,
                    style = NunchukTheme.typography.body,
                )
            }
        }

    }
}

@Composable
fun ChatInput(onSendMessage: (String) -> Unit) {
    var inputText by remember { mutableStateOf("") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
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
            inputBoxHeight = 40.dp,
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

// Message data class
data class Message(
    val text: String,
    val isSent: Boolean,
    val senderId: String? = null,
    val time: String? = null,
    val delivered: Boolean = false
)

@Preview
@Composable
fun PreviewFreeGroupWalletChatScreen() {
    FreeGroupWalletChatScreen()
}