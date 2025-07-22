package com.nunchuk.android.settings.walletvisibilitysettings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nunchuk.android.compose.NcSwitch
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.greyDark
import com.nunchuk.android.core.base.BaseComposeActivity
import com.nunchuk.android.settings.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WalletVisibilitySettingsActivity : BaseComposeActivity() {

    private val viewModel: WalletVisibilitySettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                val state by viewModel.state.collectAsStateWithLifecycle()

                WalletVisibilitySettingsContent(uiState = state, onFontSizeChange = {
                    viewModel.onFontSizeChange(it)
                },
                    onDisplayTotalBalanceChange = {
                        viewModel.onDisplayTotalBalanceChange(it)
                    },
                    onDisplayWalletShortcutChange = {
                        viewModel.onDisplayWalletShortcutsChange(it)
                    })
            }
        })
    }

    @Composable
    fun WalletVisibilitySettingsContent(
        uiState: WalletVisibilitySettingsUiState = WalletVisibilitySettingsUiState(),
        onFontSizeChange: (Boolean) -> Unit = { },
        onDisplayTotalBalanceChange: (Boolean) -> Unit = { },
        onDisplayWalletShortcutChange: (Boolean) -> Unit = { }
    ) {
        NunchukTheme {
            Scaffold(topBar = {
                NcTopAppBar(
                    title = stringResource(id = R.string.nc_home_screen_settings),
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
                        modifier = Modifier.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            modifier = Modifier
                                .weight(1f, true)
                                .padding(end = 12.dp),
                            text = stringResource(id = R.string.nc_use_large_font_balances_home_screen),
                            style = NunchukTheme.typography.body
                        )

                        NcSwitch(
                            checked = uiState.homeDisplaySetting.useLargeFont,
                            onCheckedChange = onFontSizeChange,
                        )
                    }

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
                                text = stringResource(id = R.string.nc_display_total_balance_home_screen),
                                style = NunchukTheme.typography.body
                            )
                        }

                        NcSwitch(
                            checked = uiState.homeDisplaySetting.showTotalBalance,
                            onCheckedChange = onDisplayTotalBalanceChange,
                        )
                    }

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
                                text = stringResource(id = R.string.nc_display_wallet_shortcut),
                                style = NunchukTheme.typography.body
                            )
                            Text(
                                modifier = Modifier.padding(top = 4.dp),
                                text = stringResource(id = R.string.nc_display_wallet_shortcut_desc),
                                style = NunchukTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.greyDark)
                            )
                        }

                        NcSwitch(
                            checked = uiState.homeDisplaySetting.showWalletShortcuts,
                            onCheckedChange = onDisplayWalletShortcutChange,
                        )
                    }
                }
            }
        }
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, WalletVisibilitySettingsActivity::class.java))
        }
    }
}

@Preview
@Composable
fun WalletVisibilitySettingsContentPreview() {
    WalletVisibilitySettingsActivity().WalletVisibilitySettingsContent()
}