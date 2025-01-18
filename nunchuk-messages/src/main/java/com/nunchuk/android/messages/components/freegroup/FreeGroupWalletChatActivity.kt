package com.nunchuk.android.messages.components.freegroup

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NcTextField
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.border
import com.nunchuk.android.compose.lightGray
import com.nunchuk.android.compose.textPrimary
import com.nunchuk.android.compose.textSecondary
import com.nunchuk.android.core.base.BaseComposeActivity
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.messages.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FreeGroupWalletChatActivity : BaseComposeActivity() {

    private val viewModel: FreeGroupWalletChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                FreeGroupWalletChatScreen(
                    state = state,
                    onSendMessage = { message ->
                        viewModel.sendMessage(message)
                    },
                    onWalletInfoClick = {
                        navigator.openWalletDetailsScreen(this@FreeGroupWalletChatActivity, it)
                    }
                )
            }
        })
    }

    companion object {
        const val TAG = "FreeGroupWalletChatActivity"
        const val EXTRA_WALLET_ID = "wallet_id"

        fun start(activity: Context, walletId: String) {
            activity.startActivity(Intent(activity, FreeGroupWalletChatActivity::class.java)
                .apply {
                    putExtra(EXTRA_WALLET_ID, walletId)
                })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FreeGroupWalletChatScreen(
    state: FreeGroupWalletChatUiState = FreeGroupWalletChatUiState(),
    onSendMessage: (String) -> Unit = {},
    onWalletInfoClick: (String) -> Unit = {}
) {
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
                                text = state.wallet?.name.orEmpty(),
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
                ChatInput(
                    modifier = Modifier.imePadding(),
                    onSendMessage = { message ->
                        onSendMessage(message)
                    })
            }) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .navigationBarsPadding()
                    .fillMaxHeight()
            ) {
                ChatHeader(
                    btc = state.wallet?.getBTCAmount().orEmpty(),
                    multisign = "${state.wallet?.totalRequireSigns ?: 0}/${state.wallet?.signers?.size ?: 0} ${
                        stringResource(
                            R.string.nc_wallet_multisig
                        )
                    }",
                    onClick = { onWalletInfoClick(state.wallet?.id.orEmpty()) }
                )

                ChatMessages(
                    messages = state.messageUis
                )
            }
        }
    }
}


@Composable
fun ChatHeader(btc: String, multisign: String, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.lightGray)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_wallet_info),
            contentDescription = null,
            tint = Color.Black,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = btc,
                style = NunchukTheme.typography.title,
            )
            Text(
                text = multisign,
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
fun ChatMessages(messages: List<MessageUI>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        reverseLayout = true,
    ) {
        items(messages) { message ->
            when (message) {
                is MessageUI.SenderMessage -> {
                    SentMessageBubble(message)
                }

                is MessageUI.ReceiverMessage -> {
                    ReceivedMessageBubble(message)
                }

                is MessageUI.TimeMessage -> {
                    Text(
                        text = message.date,
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
}

@Composable
fun SentMessageBubble(message: MessageUI.SenderMessage) {
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
                text = message.data.content,
                style = NunchukTheme.typography.body,
                color = Color.Black
            )
        }
        Text(
            text = "Delivered",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReceivedMessageBubble(message: MessageUI.ReceiverMessage) {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val maxWidth = (screenWidth * 0.7f)
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = message.data.signer,
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
                    .border(1.dp, MaterialTheme.colorScheme.border, CircleShape)
                    .background(Color.White, CircleShape)
                    .padding(7.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_user_2),
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
                    .combinedClickable(
                        onClick = { /* Handle click if needed */ },
                        onLongClick = {
                            clipboardManager.setText(AnnotatedString(message.data.content))
                            Toast
                                .makeText(
                                    context,
                                    "Message copied to clipboard",
                                    Toast.LENGTH_SHORT
                                )
                                .show()
                        }
                    )
            ) {
                Text(
                    text = message.data.content,
                    style = NunchukTheme.typography.body,
                )
            }
        }

    }
}

@Composable
fun ChatInput(
    modifier: Modifier = Modifier,
    onSendMessage: (String) -> Unit
) {
    var inputText by remember { mutableStateOf("") }

    Row(
        modifier = modifier
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
fun PreviewFreeGroupWalletChatScreen() {
    FreeGroupWalletChatScreen(
        state = FreeGroupWalletChatUiState(
            messages = generateSampleFreeGroupMessages(),
            messageUis = generateSampleFreeGroupMessages().groupByDate("senderId")
        )
    )
}