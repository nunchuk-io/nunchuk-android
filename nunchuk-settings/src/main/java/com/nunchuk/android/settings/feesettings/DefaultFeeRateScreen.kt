package com.nunchuk.android.settings.feesettings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcRadioButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.R
import com.nunchuk.android.model.FreeRateOption

@Composable
fun DefaultFeeRateContent(
    defaultSelectedOption: Int = FreeRateOption.ECONOMIC.ordinal,
    onContinueClick: (option: Int) -> Unit = { _ -> },
) {
    var selectedOption by remember(defaultSelectedOption) { mutableIntStateOf(defaultSelectedOption) }

    NunchukTheme {
        Scaffold(
            modifier = Modifier
                .navigationBarsPadding()
                .statusBarsPadding(),
            topBar = {
                NcTopAppBar(
                    title = stringResource(R.string.nc_default_fee_rate),
                    textStyle = NunchukTheme.typography.titleLarge
                )
            },
            bottomBar = {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    NcPrimaryDarkButton(
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .fillMaxWidth(),
                        enabled = selectedOption != defaultSelectedOption,
                        onClick = {
                            onContinueClick(selectedOption)
                        }) {
                        Text(text = stringResource(R.string.nc_save))
                    }
                }
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding()
                    .fillMaxHeight()
            ) {
                Text(
                    modifier = Modifier.padding(bottom = 8.dp),
                    text = stringResource(R.string.nc_select_default_fee_rate),
                    style = NunchukTheme.typography.titleSmall
                )
                OptionItem(
                    title = stringResource(R.string.nc_economy),
                    description = stringResource(R.string.nc_economy_desc),
                    selected = selectedOption == FreeRateOption.ECONOMIC.ordinal,
                    isRecommended = true
                ) {
                    selectedOption = FreeRateOption.ECONOMIC.ordinal
                }
                OptionItem(
                    title = stringResource(R.string.nc_standard_option),
                    description = stringResource(R.string.nc_standard_desc),
                    selected = selectedOption == FreeRateOption.STANDARD.ordinal,
                    isRecommended = false
                ) {
                    selectedOption = FreeRateOption.STANDARD.ordinal
                }
                OptionItem(
                    title = stringResource(R.string.nc_priority),
                    description = stringResource(R.string.nc_priority_desc),
                    selected = selectedOption == FreeRateOption.PRIORITY.ordinal,
                    isRecommended = false
                ) {
                    selectedOption = FreeRateOption.PRIORITY.ordinal
                }
            }
        }
    }
}

@Composable
private fun OptionItem(
    title: String,
    description: String,
    selected: Boolean,
    isRecommended: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clickable(
                onClick = onClick
            )
            .padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f, true), verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row {
                Text(
                    text = title, style = NunchukTheme.typography.body
                )
                if (isRecommended) {
                    Row(
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .background(
                                color = colorResource(R.color.nc_bg_mid_gray),
                                shape = RoundedCornerShape(24.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.nc_recommended),
                            style = NunchukTheme.typography.bold.copy(
                                color = colorResource(R.color.nc_text_primary),
                                fontSize = 10.sp
                            ),
                        )
                    }
                }
            }

            Text(
                text = description,
                style = NunchukTheme.typography.bodySmall.copy(color = colorResource(R.color.nc_text_secondary))
            )
        }
        NcRadioButton(modifier = Modifier.size(24.dp), selected = selected, onClick = onClick)
    }
}

@PreviewLightDark
@Composable
fun DefaultFeeRateContentPreview() {
    DefaultFeeRateContent()
}