package com.nunchuk.android.main.membership.honey.distribution

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.signer.SignerCard
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.main.R

/** Confirms the inheritance key is on the plan; the distribution choice comes next. */
@Composable
fun InheritanceKeyAddedContent(
    signer: SignerModel? = null,
    remainTime: Int = 0,
    onContinueClicked: () -> Unit = {},
) {
    NunchukTheme {
        Scaffold(
            modifier = Modifier.navigationBarsPadding(),
            topBar = {
                NcTopAppBar(
                    title = if (remainTime <= 0) "" else stringResource(
                        R.string.nc_estimate_remain_time, remainTime
                    ),
                    isBack = false,
                )
            },
            bottomBar = {
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    onClick = onContinueClicked,
                ) {
                    Text(text = stringResource(R.string.nc_text_continue))
                }
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // Filled green disc with a white tick, per the design; nc_circle_checked draws its
                // own outline so it is tinted white and sits on top of the disc.
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .background(
                            color = colorResource(id = R.color.nc_slime_dark),
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    NcIcon(
                        modifier = Modifier.size(56.dp),
                        painter = painterResource(id = R.drawable.ic_check),
                        contentDescription = "Success icon",
                        tint = Color.White,
                    )
                }
                Text(
                    modifier = Modifier.padding(top = 24.dp),
                    text = stringResource(R.string.nc_inheritance_key_added),
                    style = NunchukTheme.typography.heading,
                    textAlign = TextAlign.Center,
                )
                if (signer != null) {
                    SignerCard(
                        item = signer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp),
                    )
                }
                Spacer(modifier = Modifier.padding(bottom = 24.dp))
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun InheritanceKeyAddedContentPreview() {
    InheritanceKeyAddedContent()
}
