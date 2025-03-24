package com.nunchuk.android.wallet.personal.components.taproot

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.nunchuk.android.compose.NcCircleImage
import com.nunchuk.android.compose.NcHighlightText
import com.nunchuk.android.compose.NcHintMessage
import com.nunchuk.android.compose.NcOptionItem
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.wallet.personal.R

const val enableValueKeyRoute = "enableValueKey"

fun NavGraphBuilder.enableValueKeyScreen(
    onContinue: (Boolean) -> Unit
) {
    composable(
        route = enableValueKeyRoute,
    ) {
        EnableValueKeysetScreen(
            onContinue = onContinue
        )
    }
}

fun NavController.navigateEnableValueKey() {
    navigate(enableValueKeyRoute)
}

@Composable
fun EnableValueKeysetScreen(
    onContinue: (Boolean) -> Unit
) {
    var isEnabled by remember { mutableStateOf(true) }
    NunchukTheme {
        NcScaffold(
            modifier = Modifier.systemBarsPadding(),
            topBar = {
                NcTopAppBar(title = "")
            },
            bottomBar = {
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    onClick = { onContinue(isEnabled) }
                ) {
                    Text(stringResource(R.string.nc_text_continue))
                }
            },
            content = { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    NcCircleImage(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        resId = R.drawable.ic_mulitsig_dark,
                        size = 96.dp,
                        iconSize = 60.dp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.nc_value_keyset),
                        style = NunchukTheme.typography.heading,
                    )

                    NcHighlightText(
                        modifier = Modifier.padding(top = 12.dp),
                        text = stringResource(R.string.nc_value_key_set_description),
                        style = NunchukTheme.typography.body,
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    NcOptionItem(
                        label = stringResource(R.string.nc_enable_value_keyset),
                        isSelected = isEnabled,
                        onClick = { isEnabled = true }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    NcOptionItem(
                        label = stringResource(R.string.nc_disable_value_keyset),
                        isSelected = !isEnabled,
                        onClick = { isEnabled = false }
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    NcHintMessage(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.nc_value_keyset_hit),
                            style = NunchukTheme.typography.caption
                        )
                    }
                }
            }
        )
    }
}

@PreviewLightDark
@Composable
private fun EnableValueKeysetScreenPreview() {
    EnableValueKeysetScreen {}
}