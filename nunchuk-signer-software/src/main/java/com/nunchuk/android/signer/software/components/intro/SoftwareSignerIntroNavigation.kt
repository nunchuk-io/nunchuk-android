package com.nunchuk.android.signer.software.components.intro

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.nunchuk.android.compose.NcCircleImage
import com.nunchuk.android.compose.NcHintMessage
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcSelectableBottomSheet
import com.nunchuk.android.compose.NcSpannedText
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.SpanIndicator
import com.nunchuk.android.core.util.ClickAbleText
import com.nunchuk.android.signer.software.R

const val softwareSignerIntroRoute = "software_signer_intro"

fun NavGraphBuilder.softwareSignerIntro(
    isSupportXprv: Boolean,
    onCreateNewSeedClicked: () -> Unit = {},
    onRecoverSeedClicked: () -> Unit = {},
    onRecoverXprvClicked: () -> Unit = {},
) {
    composable(softwareSignerIntroRoute) {
        SoftwareSignerIntroScreen(
            isSupportXprv = isSupportXprv,
            onCreateNewSeedClicked = onCreateNewSeedClicked,
            onRecoverSeedClicked = onRecoverSeedClicked,
            onRecoverXprvClicked = onRecoverXprvClicked,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoftwareSignerIntroScreen(
    isSupportXprv: Boolean = false,
    onCreateNewSeedClicked: () -> Unit = {},
    onRecoverSeedClicked: () -> Unit = {},
    onRecoverXprvClicked: () -> Unit = {},
) {
    var showRecoverSheet by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        modifier = Modifier.systemBarsPadding(),
        topBar = {
            NcTopAppBar(
                title = stringResource(R.string.nc_before_you_start),
                textStyle = NunchukTheme.typography.titleLarge,
                actions = {
                    Spacer(modifier = Modifier.size(40.dp))
                }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                NcHintMessage(
                    messages = listOf(ClickAbleText("Upgrade software keys to hardware keys for improved security."))
                )

                NcPrimaryDarkButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onCreateNewSeedClicked
                ) {
                    Text(text = stringResource(id = R.string.nc_ssigner_new_seed))
                }

                NcOutlineButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        if (isSupportXprv) {
                            showRecoverSheet = true
                        } else {
                            onRecoverSeedClicked()
                        }
                    }
                ) {
                    Text(text = stringResource(id = R.string.nc_ssigner_recover_seed))
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .fillMaxSize(),
        ) {
            NcCircleImage(
                resId = R.drawable.ic_warning_outline,
                modifier = Modifier
                    .padding(top = 24.dp)
                    .align(Alignment.CenterHorizontally),
                size = 96.dp,
                iconSize = 60.dp
            )

            NcSpannedText(
                modifier = Modifier.padding(top = 24.dp),
                text = "A software key will be generated locally on this device. [B]Deleting the app will also delete the software key.[/B]\n\nPlease make sure to:",
                baseStyle = NunchukTheme.typography.body,
                styles = mapOf(SpanIndicator('B') to SpanStyle(fontWeight = FontWeight.Bold)),
            )

            Section(
                modifier = Modifier.padding(top = 24.dp),
                iconResId = R.drawable.ic_replace_primary_key,
                title = "Back up the key ",
                content = "The backup will allow you to recover the key in worst case scenarios.",
            )

            Section(
                modifier = Modifier.padding(top = 24.dp),
                iconResId = R.drawable.ic_emergency_lockdown_dark,
                title = "Keep your device secure",
                content = "Since the software key resides on this device, keeping the device safe will prevent the software key from being compromised.",
            )
        }
    }

    if (showRecoverSheet) {
        NcSelectableBottomSheet(
            options = listOf(
                stringResource(R.string.nc_recover_key_via_seed),
                stringResource(R.string.nc_recover_key_via_xprv),
            ),
            onSelected = {
                if (it == 0) {
                    onRecoverSeedClicked()
                } else {
                    onRecoverXprvClicked()
                }
                showRecoverSheet = false
            },
            onDismiss = {
                showRecoverSheet = false
            },
        )
    }
}

@Composable
private fun Section(
    @DrawableRes iconResId: Int,
    title: String,
    content: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        NcIcon(painter = painterResource(id = iconResId), contentDescription = "Icon")
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = NunchukTheme.typography.title,
            )
            Text(
                text = content,
                style = NunchukTheme.typography.body,
            )
        }
    }
}

@Preview
@Composable
private fun SoftwareSignerIntroPreview() {
    NunchukTheme {
        SoftwareSignerIntroScreen()
    }
}
