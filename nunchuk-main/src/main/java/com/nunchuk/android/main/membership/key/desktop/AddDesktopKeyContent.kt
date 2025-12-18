package com.nunchuk.android.main.membership.key.desktop

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcSpannedClickableText
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.SpanIndicator
import com.nunchuk.android.core.util.openExternalLink
import com.nunchuk.android.main.R

@Composable
fun AddDesktopKeyContent(
    onContinueClicked: () -> Unit = {},
    onMoreClicked: () -> Unit = {},
    remainTime: Int = 0,
    @DrawableRes backgroundId: Int = R.drawable.bg_add_ledger,
    title: String = "",
    desc: String = "",
    button: String = "",
    isMembershipFlow: Boolean = true,
) {
    val context = LocalContext.current
    NunchukTheme {
        Scaffold(
            modifier = Modifier.navigationBarsPadding(),
            bottomBar = {
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    onClick = onContinueClicked
                ) {
                    Text(
                        text = button
                    )
                }
            },
            topBar = {
                NcImageAppBar(
                    backgroundRes = backgroundId,
                    title = stringResource(
                        id = R.string.nc_estimate_remain_time,
                        remainTime
                    ).takeIf { isMembershipFlow },
                    actions = {
                        if (isMembershipFlow) {
                            IconButton(onClick = onMoreClicked) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_more),
                                    contentDescription = "More icon"
                                )
                            }
                        }
                    },
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    modifier = Modifier.padding(16.dp),
                    text = title,
                    style = NunchukTheme.typography.heading
                )
                NcSpannedClickableText(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = desc,
                    baseStyle = NunchukTheme.typography.body,
                    styles = mapOf(SpanIndicator('A') to SpanStyle(textDecoration = TextDecoration.Underline))
                ) {
                    context.openExternalLink("https://github.com/nunchuk-io/nunchuk-desktop/releases")
                }
            }
        }
    }
}

@Preview
@Composable
private fun AddLedgerScreenPreview() {
    AddDesktopKeyContent(
        title = "Add Ledger",
        desc = stringResource(id = R.string.nc_main_add_ledger_desc),
        button = "Continue to add Ledger",
    )
}