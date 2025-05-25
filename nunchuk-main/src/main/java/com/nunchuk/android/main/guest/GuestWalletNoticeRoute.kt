package com.nunchuk.android.main.guest

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nunchuk.android.compose.NcCircleImage
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.fillBeewax
import com.nunchuk.android.signer.R

@Composable
fun GuestWalletNoticeRoute(
    onGotIt: () -> Unit = {},
    viewModel: GuestWalletNoticeViewModel = hiltViewModel()
) {

    LaunchedEffect(Unit) {
        viewModel.handledFirstCreateEmail()
    }

    GuestWalletNoticeScreen(onGotIt = onGotIt)
}

@Composable
fun GuestWalletNoticeScreen(
    onGotIt: () -> Unit
) {
    NcScaffold(
        modifier = Modifier.fillMaxSize().systemBarsPadding(),
        topBar = {
            Spacer(Modifier.height(56.dp))
        }, bottomBar = {
            NcPrimaryDarkButton(
                onClick = onGotIt,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Got it",
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.Top
        ) {
            NcCircleImage(
                size = 96.dp,
                iconSize = 60.dp,
                resId = R.drawable.ic_account,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally),
            )
            Text(
                text = "Guest wallets notice",
                style = NunchukTheme.typography.heading,
                modifier = Modifier.padding(top = 24.dp)
            )
            Text(
                text = "The wallets created in guest mode will still work normally after you create a new account. To access them again, switch back to guest mode:",
                style = NunchukTheme.typography.body,
                modifier = Modifier.padding(top = 12.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .background(
                        MaterialTheme.colorScheme.fillBeewax,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(vertical = 16.dp, horizontal = 12.dp)
            ) {
                Text(
                    text = "Profile → Sign out → Continue as guest",
                    style = NunchukTheme.typography.body,
                    color = NunchukTheme.typography.body.color
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewGuestWalletNoticeScreen() {
    NunchukTheme {
        GuestWalletNoticeScreen(onGotIt = {})
    }
}