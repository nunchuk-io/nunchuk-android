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
import com.nunchuk.android.core.locale.AppLanguageManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.Serializable

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
                        startDestination = DisplaySettingsRoute
                    ) {
                        composable<DisplaySettingsRoute> {
                            DisplaySettingsContent(
                                uiState = state,
                                onWalletVisibilityClick = {
                                    navHostController.navigate(HomeScreenSettingsRoute)
                                },
                                onDisplayUnitClick = {
                                    navigator.openDisplayUnitScreen(this@DisplaySettingsActivity)
                                },
                                onAppearanceClick = {
                                    navHostController.navigate(AppearanceRoute)
                                },
                                languageLabel = AppLanguageManager.languageInUse(this@DisplaySettingsActivity).nativeName,
                                onLanguageClick = {
                                    navHostController.navigate(LanguageRoute)
                                },
                                onHideWalletDetailsChange = { _ ->
                                    viewModel.updateHideWalletDetail()
                                }
                            )
                        }

                        composable<AppearanceRoute> {
                            AppearanceScreen(
                                uiState = state,
                                onThemeModeChange = { themeMode ->
                                    viewModel.setThemeMode(themeMode)
                                }
                            )
                        }

                        composable<LanguageRoute> {
                            LanguageScreen()
                        }

                        composable<HomeScreenSettingsRoute> {
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
                                    navHostController.navigate(ExchangeRateUnitRoute)
                                }
                            )
                        }

                        composable<ExchangeRateUnitRoute> {
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

@Serializable
internal data object DisplaySettingsRoute

@Serializable
internal data object AppearanceRoute

@Serializable
internal data object LanguageRoute

@Serializable
internal data object HomeScreenSettingsRoute

@Serializable
internal data object ExchangeRateUnitRoute
