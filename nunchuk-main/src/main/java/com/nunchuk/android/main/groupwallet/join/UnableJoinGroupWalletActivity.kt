package com.nunchuk.android.main.groupwallet.join

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.base.BaseComposeActivity
import com.nunchuk.android.signer.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class UnableJoinGroupWalletActivity : BaseComposeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                UnableJoinGroupWalletScreen(
                    link = intent.getStringExtra(EXTRA_LINK) ?: ""
                ) {
                    finish()
                }
            }
        })
    }

    companion object {
        const val TAG = "UnableJoinGroupWalletActivity"
        private const val EXTRA_LINK = "extra_link"
        fun start(context: Context, link: String) {
            context.startActivity(Intent(context, UnableJoinGroupWalletActivity::class.java)
                .apply {
                    putExtra(EXTRA_LINK, link)
                })
        }
    }
}

@Composable
fun UnableJoinGroupWalletScreen(
    link: String = "",
    onGotItClicked: () -> Unit = {}
) {
    NunchukTheme {
        Scaffold(modifier = Modifier.navigationBarsPadding(),
            topBar = {
                NcTopAppBar(
                    title = "",
                    isBack = false
                )
            }, bottomBar = {
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    onClick = {
                        onGotItClicked()
                    },
                ) {
                    Text(text = stringResource(id = R.string.nc_text_got_it))
                }
            }) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding()
                    .fillMaxHeight()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_unable_access_link),
                        contentDescription = "Help Icon",
                        modifier = Modifier.size(96.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(id = R.string.nc_unable_access_link),
                    style = NunchukTheme.typography.heading,
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(id = R.string.nc_unable_access_link_desc),
                    style = NunchukTheme.typography.body,
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = colorResource(R.color.nc_bg_mid_gray),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(16.dp)
                        .clickable {
                            onGotItClicked()
                        }
                ) {
                    Text(
                        text = link,
                        style = NunchukTheme.typography.body,
                        textAlign = TextAlign.Start,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
fun PreviewUnableJoinGroupWalletScreen() {
    UnableJoinGroupWalletScreen(
        link = "https://nunchuk.io/dw/id/groupwallet2"
    )
}