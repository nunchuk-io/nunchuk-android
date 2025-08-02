package com.nunchuk.android.settings.displaysettings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nunchuk.android.core.base.BaseComposeActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DisplaySettingsActivity : BaseComposeActivity() {

    private val viewModel: DisplaySettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(
            ComposeView(this).apply {
                setContent {
                    val navHostController = rememberNavController()
                    val state by viewModel.state.collectAsStateWithLifecycle()

                    NavHost(
                        navController = navHostController,
                        startDestination = "display_settings"
                    ) {
                        composable("display_settings") {
                            DisplaySettingsContent(
                                uiState = state,
                                onWalletVisibilityClick = {
                                    navHostController.navigate("home_screen_settings")
                                },
                                onDisplayUnitClick = {
                                    navigator.openDisplayUnitScreen(this@DisplaySettingsActivity)
                                },
                                onAppearanceClick = {
                                    navHostController.navigate("appearance")
                                },
                                onHideWalletDetailsChange = { _ ->
                                    viewModel.updateHideWalletDetail()
                                }
                            )
                        }

                        composable("appearance") {
                            AppearanceScreen(
                                uiState = state,
                                onThemeModeChange = { themeMode ->
                                    viewModel.setThemeMode(themeMode)
                                }
                            )
                        }

                        composable("home_screen_settings") {
                            HomeScreenSettingsContent(
                                uiState = state,
                                onFontSizeChange = { largeFont ->
                                    viewModel.onFontSizeChange(largeFont)
                                },
                                onDisplayTotalBalanceChange = { displayTotalBalance ->
                                    viewModel.onDisplayTotalBalanceChange(displayTotalBalance)
                                },
                                onDisplayWalletShortcutChange = { displayWalletShortcuts ->
                                    viewModel.onDisplayWalletShortcutsChange(displayWalletShortcuts)
                                },
                                onExchangeRateUnitClick = {
                                    navHostController.navigate("exchange_rate_unit")
                                }
                            )
                        }

                        composable("exchange_rate_unit") {
                            ExchangeRateUnitContent(
                                currentUnit = state.homeDisplaySetting.exchangeRateUnit,
                                onUnitSelected = { unit ->
                                    viewModel.setExchangeRateUnit(unit)
                                }
                            )
                        }
                    }
                }
            }
        )
    }

    override fun onResume() {
        super.onResume()
        viewModel.getDisplayUnitSetting()
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, DisplaySettingsActivity::class.java))
        }
    }
}