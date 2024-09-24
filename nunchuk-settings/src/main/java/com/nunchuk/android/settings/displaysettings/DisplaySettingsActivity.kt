package com.nunchuk.android.settings.displaysettings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.greyDark
import com.nunchuk.android.core.base.BaseComposeActivity
import com.nunchuk.android.core.domain.data.SAT
import com.nunchuk.android.settings.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DisplaySettingsActivity : BaseComposeActivity() {

    private val viewModel: DisplaySettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(
            ComposeView(this).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

                setContent {
                    val state by viewModel.state.collectAsStateWithLifecycle()

                    DisplaySettingsContent(uiState = state,
                        onWalletVisibilityClick = {
                            navigator.openWalletVisibilitySettingsScreen(this@DisplaySettingsActivity)
                        }, onDisplayUnitClick = {
                        navigator.openDisplayUnitScreen(this@DisplaySettingsActivity)
                    })
                }
            }
        )
    }

    override fun onResume() {
        super.onResume()
        viewModel.getDisplayUnitSetting()
    }

    @Composable
    fun DisplaySettingsContent(
        uiState: DisplaySettingsUiState = DisplaySettingsUiState(),
        onDisplayUnitClick: () -> Unit = { },
        onWalletVisibilityClick: () -> Unit = { }
    ) {
        NunchukTheme {
            Scaffold(topBar = {
                NcTopAppBar(
                    title = stringResource(id = R.string.nc_display_settings),
                    textStyle = NunchukTheme.typography.titleLarge
                )
            }) { innerPadding ->
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp)
                        .navigationBarsPadding()
                        .fillMaxHeight()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(top = 12.dp, bottom = 16.dp)
                            .clickable { onDisplayUnitClick() }) {
                        Text(
                            text = stringResource(id = R.string.nc_display_unit),
                            style = NunchukTheme.typography.body
                        )
                        val unitText = if (uiState.unit == SAT) {
                            stringResource(R.string.nc_currency_sat)
                        } else {
                            stringResource(R.string.nc_currency_btc)
                        }
                        Text(
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .weight(1f, true),
                            text = "($unitText)",
                            style = NunchukTheme.typography.body.copy(color = MaterialTheme.colorScheme.greyDark)
                        )

                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow),
                            contentDescription = ""
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp, bottom = 16.dp)
                            .clickable { onWalletVisibilityClick() }) {
                        Text(
                            text = stringResource(id = R.string.nc_wallet_visibility_settings),
                            style = NunchukTheme.typography.body,
                            modifier = Modifier.weight(1f)
                        )

                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow),
                            contentDescription = ""
                        )
                    }
                }
            }
        }
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, DisplaySettingsActivity::class.java))
        }
    }
}

@Preview
@Composable
fun DisplaySettingsContentPreview() {
    DisplaySettingsActivity().DisplaySettingsContent()
}