package com.nunchuk.android.settings.feesettings

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
import com.nunchuk.android.core.R
import com.nunchuk.android.core.base.BaseComposeActivity
import com.nunchuk.android.nav.args.FeeSettingArgs
import com.nunchuk.android.nav.args.FeeSettingStartDestination
import com.nunchuk.android.widget.NCToastMessage
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FeeSettingsActivity : BaseComposeActivity() {

    private val viewModel: FeeSettingsViewModel by viewModels()
    private val args: FeeSettingArgs by lazy { FeeSettingArgs.deserializeFrom(intent.extras ?: Bundle()) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val startDestination = when (args.destination) {
            FeeSettingStartDestination.MAIN -> "fee_settings"
            FeeSettingStartDestination.DEFAULT_FEE_RATE -> "default_fee_rate"
            FeeSettingStartDestination.TAPROOT_FEE_SELECTION -> "taproot_fee_selection"
        }

        setContentView(
            ComposeView(this).apply {
                setContent {
                    val navHostController = rememberNavController()
                    val state by viewModel.state.collectAsStateWithLifecycle()

                    NavHost(
                        navController = navHostController,
                        startDestination = startDestination
                    ) {
                        composable("fee_settings") {
                            FeeSettingsContent(
                                feeRate = state.defaultFee,
                                defaultAntiFeeSnipingEnabled = state.antiFeeSniping,
                                openDefaultFeeRate = {
                                    navHostController.navigate("default_fee_rate")
                                },
                                openTaprootFeeSelection = {
                                    navHostController.navigate("taproot_fee_selection")
                                },
                                onContinueClick = { antiFeeSniping ->
                                    viewModel.setAntiFeeSniping(antiFeeSniping)

                                }
                            )
                        }

                        composable("default_fee_rate") {
                            DefaultFeeRateContent(
                                defaultSelectedOption = state.defaultFee,
                            ) { option ->
                                viewModel.setDefaultFee(option)
                                NCToastMessage(this@FeeSettingsActivity).showMessage(
                                    message = getString(R.string.nc_fee_settings_updated),
                                    icon = R.drawable.ic_check_circle_outline
                                )
                            }
                        }

                        composable("taproot_fee_selection") {
                            TaprootFeeSelectionContent(
                                state = state,
                            ) { enabled, taprootPercentage, taprootAmount ->
                                viewModel.saveTaprootSelectionFeeSetting(enabled, taprootPercentage, taprootAmount)
                                NCToastMessage(this@FeeSettingsActivity).showMessage(
                                    message = getString(R.string.nc_fee_settings_updated),
                                    icon = R.drawable.ic_check_circle_outline
                                )
                            }
                        }
                    }
                }
            })
    }

    companion object {
        fun start(context: Context, args: FeeSettingArgs) {
            context.startActivity(Intent(context, FeeSettingsActivity::class.java).apply {
                putExtras(args.buildBundle())
            })
        }
    }
}