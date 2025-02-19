package com.nunchuk.android.main.groupwallet.join

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.base.BaseComposeActivity
import com.nunchuk.android.core.qr.convertToQRCode
import com.nunchuk.android.core.share.IntentSharingController
import com.nunchuk.android.main.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CommonQRCodeActivity : BaseComposeActivity() {

    private val qrCode by lazy { intent.getStringExtra(EXTRA_QR_CODE) ?: "" }

    private val viewModel: CommonQRCodeViewModel by viewModels()

    private val controller: IntentSharingController by lazy(LazyThreadSafetyMode.NONE) {
        IntentSharingController.from(
            this
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                val uiState = viewModel.uiState.collectAsStateWithLifecycle()
                CommonQRCodeScreen(
                    qrCodeBitmap = uiState.value.qrCodeBitmap,
                    onShareClicked = {
                        viewModel.shareQRCode(it)
                    }
                )
            }
        })

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.event.collect { event ->
                    when (event) {
                        is CommonQRCodeEvent.ShareQRCodeSuccess -> {
                            shareConfigurationFile(event.filePath)
                        }

                        is CommonQRCodeEvent.Error -> {
                        }
                    }
                }
            }
        }
        viewModel.generateQRCode(qrCode)
    }

    private fun shareConfigurationFile(filePath: String) {
        controller.shareFile(filePath)
    }

    companion object {
        const val EXTRA_QR_CODE = "qr_code"
        fun start(context: Context, qrCode: String) {
            context.startActivity(Intent(context, CommonQRCodeActivity::class.java).apply {
                putExtra(EXTRA_QR_CODE, qrCode)
            })
        }
    }
}

@Composable
fun CommonQRCodeScreen(
    qrCodeBitmap: Bitmap?,
    onShareClicked: (Bitmap) -> Unit = {}
) {
    NunchukTheme {
        Scaffold(topBar = {
            NcTopAppBar(
                title = "Share QR code",
                textStyle = NunchukTheme.typography.titleLarge,
                isBack = false
            )
        }) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding()
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (qrCodeBitmap == null) {
                    return@Column
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(340.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, Color.Gray, RoundedCornerShape(16.dp))
                ) {
                    Image(
                        bitmap = qrCodeBitmap.asImageBitmap(),
                        contentDescription = "QR Code",
                        modifier = Modifier.fillMaxSize()
                    )
//                    Image(
//                        painter = painterResource(id = R.drawable.ic_nunchuk_logo_circle),
//                        contentDescription = "Logo",
//                        modifier = Modifier.size(78.dp)
//                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Scan the QR code to join the group wallet.",
                    style = NunchukTheme.typography.body,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                IconButton(
                    onClick = {
                        onShareClicked(qrCodeBitmap)
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            color = Color(0xFFF5F5F5),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_share),
                        contentDescription = "Share",
                        tint = Color.Black
                    )
                }

            }
        }
    }
}

@Preview
@Composable
fun PreviewCommonQRCodeScreen() {
    CommonQRCodeScreen(qrCodeBitmap = "url".convertToQRCode(),
        onShareClicked = {})
}