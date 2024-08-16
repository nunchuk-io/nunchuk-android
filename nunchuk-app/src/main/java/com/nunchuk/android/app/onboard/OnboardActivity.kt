package com.nunchuk.android.app.onboard

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.WindowCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.nunchuk.android.R
import com.nunchuk.android.app.onboard.advisor.navigateToOnboardAdvisorInput
import com.nunchuk.android.app.onboard.advisor.navigateToOnboardAdvisorIntro
import com.nunchuk.android.app.onboard.advisor.navigateToOnboardAssistedWalletIntro
import com.nunchuk.android.app.onboard.advisor.onboardAdvisorInput
import com.nunchuk.android.app.onboard.advisor.onboardAdvisorIntro
import com.nunchuk.android.app.onboard.advisor.onboardAssistedWalletIntro
import com.nunchuk.android.app.onboard.hotwallet.hotWalletIntro
import com.nunchuk.android.app.onboard.hotwallet.hotWalletIntroRoute
import com.nunchuk.android.app.onboard.intro.onboardIntro
import com.nunchuk.android.app.onboard.intro.onboardIntroRoute
import com.nunchuk.android.app.onboard.unassisted.navigateToUnassistedIntro
import com.nunchuk.android.app.onboard.unassisted.unassistedIntro
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.base.BaseComposeActivity
import com.nunchuk.android.core.util.AppEvenBus
import com.nunchuk.android.core.util.AppEvent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OnboardActivity : BaseComposeActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val startDestination = intent.getStringExtra(EXTRA_START_DESTINATION) ?: onboardIntroRoute
        val isQuickWallet = intent.getBooleanExtra(EXTRA_IS_QUICK_WALLET, false)
        setContentView(
            ComposeView(this).apply {
                setContent {
                    val navController = rememberNavController()
                    NunchukTheme {
                        NavHost(
                            navController = navController,
                            startDestination = startDestination
                        ) {
                            onboardIntro(
                                onOpenUnassistedIntro = {
                                    navController.navigateToUnassistedIntro()
                                },
                                onOpenAssistedIntro = {
                                    navController.navigateToOnboardAssistedWalletIntro()
                                },
                                openMainScreen = {
                                    navigator.openMainScreen(this@OnboardActivity)
                                    finish()
                                },
                                onSignIn = {
                                    navigator.openSignInScreen(this@OnboardActivity)
                                    finish()
                                }
                            )
                            unassistedIntro(
                                openMainScreen = {
                                    navigator.openMainScreen(this@OnboardActivity)
                                    finish()
                                }
                            )
                            hotWalletIntro(
                                returnToScreen = {
                                    if (isQuickWallet) {
                                        setResult(Activity.RESULT_OK)
                                        finish()
                                    } else {
                                        navigator.returnToMainScreen(this@OnboardActivity)
                                    }
                                },
                                openServiceTab = {
                                    navigator.returnToMainScreen(this@OnboardActivity)
                                    AppEvenBus.instance.publish(AppEvent.OpenServiceTabEvent)
                                }
                            )
                            onboardAssistedWalletIntro(
                                onSkip = {
                                    navigator.openMainScreen(this@OnboardActivity)
                                    finish()
                                },
                                openOnboardAdvisorInputScreen = {
                                    navController.navigateToOnboardAdvisorInput()
                                },
                                onOpenOnboardAdvisorIntroScreen = {
                                    navController.navigateToOnboardAdvisorIntro()
                                }
                            )
                            onboardAdvisorIntro(
                                onSkip = {
                                    navigator.openMainScreen(this@OnboardActivity)
                                    finish()
                                },
                                onSignIn = {
                                    navigator.openSignInScreen(this@OnboardActivity, isNeedNewTask = false)
                                },
                                navigateToOnboardAdvisorInput = {
                                    navController.navigateToOnboardAdvisorInput()
                                },
                                onCreateAccount = {
                                    navigator.openSignUpScreen(this@OnboardActivity)
                                },
                            )
                            onboardAdvisorInput(
                                onSkip = {
                                    navigator.openMainScreen(this@OnboardActivity)
                                    finish()
                                },
                                onOpenMainScreen = {
                                    navigator.openMainScreen(
                                        this@OnboardActivity,
                                        messages = arrayListOf(getString(R.string.nc_query_sent)),
                                        isClearTask = true
                                    )
                                },
                            )
                        }
                    }
                }
            }
        )
    }

    companion object {
        private const val EXTRA_START_DESTINATION = "start_destination"
        private const val EXTRA_IS_QUICK_WALLET = "is_quick_wallet"

        fun openHotWalletIntroScreen(launcher: ActivityResultLauncher<Intent>?, context: Context, isQuickWallet: Boolean) {
            if (launcher != null) {
                launcher.launch(Intent(context, OnboardActivity::class.java).apply {
                    putExtra(EXTRA_START_DESTINATION, hotWalletIntroRoute)
                    putExtra(EXTRA_IS_QUICK_WALLET, isQuickWallet)
                })
            } else {
                context.startActivity(Intent(context, OnboardActivity::class.java).apply {
                    putExtra(EXTRA_START_DESTINATION, hotWalletIntroRoute)
                    putExtra(EXTRA_IS_QUICK_WALLET, isQuickWallet)
                })
            }
        }
    }
}