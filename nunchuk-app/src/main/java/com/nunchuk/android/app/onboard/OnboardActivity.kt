package com.nunchuk.android.app.onboard

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.WindowCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.nunchuk.android.R
import com.nunchuk.android.app.onboard.advisor.navigateToOnboardAdvisorInput
import com.nunchuk.android.app.onboard.advisor.onboardAdvisorInput
import com.nunchuk.android.app.onboard.advisor.onboardAdvisorIntro
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
                                onSkip = {
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
                                returnToMainScreen = {
                                    navigator.returnToMainScreen()
                                },
                                openServiceTab = {
                                    navigator.returnToMainScreen()
                                    AppEvenBus.instance.publish(AppEvent.OpenServiceTabEvent)
                                }
                            )
                            onboardAdvisorIntro(
                                onSkip = {
                                    navigator.openMainScreen(this@OnboardActivity)
                                    finish()
                                },
                                onSignIn = {
                                    navigator.openSignInScreen(this@OnboardActivity)
                                    finish()
                                },
                                navigateToOnboardAdvisorInput = {
                                    navController.navigateToOnboardAdvisorInput()
                                },
                                onCreateAccount = {
                                    navigator.openSignUpScreen(this@OnboardActivity)
                                    finish()
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
                                        messages = arrayListOf(getString(R.string.nc_query_sent))
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

        fun openHotWalletIntroScreen(context: Context) {
            context.startActivity(Intent(context, OnboardActivity::class.java).apply {
                putExtra(EXTRA_START_DESTINATION, hotWalletIntroRoute)
            })
        }
    }
}