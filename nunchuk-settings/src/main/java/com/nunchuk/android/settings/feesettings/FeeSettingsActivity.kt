package com.nunchuk.android.settings.feesettings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcRadioButton
import com.nunchuk.android.compose.NcSwitch
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.textSecondary
import com.nunchuk.android.core.R
import com.nunchuk.android.core.base.BaseComposeActivity
import com.nunchuk.android.model.FreeRateOption
import com.nunchuk.android.widget.NCToastMessage
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FeeSettingsActivity : BaseComposeActivity() {

    private val viewModel: FeeSettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(
            ComposeView(this).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    val state by viewModel.state.collectAsStateWithLifecycle()

                    FeeSettingsContent(
                        defaultSelectedOption = state.defaultFee,
                        defaultAntiFeeSnipingEnabled = state.antiFeeSniping,
                    ) { option, antiFeeSniping ->
                        viewModel.setDefaultFee(option)
                        viewModel.setAntiFeeSniping(antiFeeSniping)
                        NCToastMessage(this@FeeSettingsActivity).showMessage(
                            message = getString(R.string.nc_fee_settings_updated),
                            icon = R.drawable.ic_check_circle_outline
                        )
                    }
                }
            })
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, FeeSettingsActivity::class.java))
        }
    }
}

@Composable
fun FeeSettingsContent(
    defaultSelectedOption: Int = FreeRateOption.ECONOMIC.ordinal,
    defaultAntiFeeSnipingEnabled: Boolean = false,
    onContinueClick: (option: Int, antiFeeSniping: Boolean) -> Unit = { _, _ -> },
) {
    var selectedOption by remember(defaultSelectedOption) { mutableIntStateOf(defaultSelectedOption) }
    var antiFeeSnipingEnabled by remember(defaultAntiFeeSnipingEnabled) {
        mutableStateOf(defaultAntiFeeSnipingEnabled)
    }

    NunchukTheme {
        Scaffold(
            modifier = Modifier
                .navigationBarsPadding()
                .statusBarsPadding(),
            topBar = {
                NcTopAppBar(
                    title = stringResource(R.string.nc_fee_settings),
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
                        enabled = selectedOption != defaultSelectedOption ||
                                antiFeeSnipingEnabled != defaultAntiFeeSnipingEnabled,
                        onClick = {
                            onContinueClick(selectedOption, antiFeeSnipingEnabled)
                        }) {
                        Text(text = stringResource(R.string.nc_save_fee_settings))
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

                HorizontalDivider()

                Row(
                    modifier = Modifier.padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f, true)
                            .padding(end = 12.dp),
                    ) {
                        Text(
                            text = stringResource(id = R.string.nc_enable_anti_fee_sniping_by_default),
                            style = NunchukTheme.typography.body
                        )
                        Text(
                            modifier = Modifier.padding(top = 4.dp),
                            text = stringResource(id = R.string.nc_enable_anti_fee_sniping_by_default_desc),
                            style = NunchukTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.textSecondary)
                        )
                    }

                    NcSwitch(
                        checked = antiFeeSnipingEnabled,
                        onCheckedChange = {
                            antiFeeSnipingEnabled = it
                        },
                    )
                }
            }
        }
    }
}

@PreviewLightDark
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

@Preview
@Composable
fun FeeSettingsContentPreview() {
    FeeSettingsContent()
}