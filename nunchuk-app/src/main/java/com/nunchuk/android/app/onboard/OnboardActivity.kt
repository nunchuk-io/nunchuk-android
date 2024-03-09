package com.nunchuk.android.app.onboard

import android.os.Bundle
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.WindowCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.nunchuk.android.app.onboard.advisor.navigateToOnboardAdvisorInput
import com.nunchuk.android.app.onboard.advisor.navigateToOnboardAdvisorIntro
import com.nunchuk.android.app.onboard.advisor.onboardAdvisorInput
import com.nunchuk.android.app.onboard.advisor.onboardAdvisorIntro
import com.nunchuk.android.app.onboard.intro.onboardIntro
import com.nunchuk.android.app.onboard.intro.onboardIntroRoute
import com.nunchuk.android.app.onboard.unassisted.navigateToUnassistedIntro
import com.nunchuk.android.app.onboard.unassisted.unassistedIntro
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.base.BaseComposeActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OnboardActivity : BaseComposeActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(
            ComposeView(this).apply {
                setContent {
                    val navController = rememberNavController()
                    NunchukTheme {
                        NavHost(
                            navController = navController,
                            startDestination = onboardIntroRoute
                        ) {
                            onboardIntro(
                                onOpenUnassistedIntro = {
                                    navController.navigateToUnassistedIntro()
                                },
                                onSkip = {
                                    navigator.openMainScreen(this@OnboardActivity)
                                },
                                onSignIn = {
                                    navigator.openSignInScreen(this@OnboardActivity)
                                }
                            )
                            unassistedIntro(
                                openMainScreen = {
                                    navigator.openMainScreen(this@OnboardActivity)
                                }
                            )
                            onboardAdvisorIntro(onSkip = {
                                navigator.openMainScreen(this@OnboardActivity)
                            }, onSignIn = {
                                navigator.openSignInScreen(this@OnboardActivity)
                            }, navigateToOnboardAdvisorInput = {
                                navController.navigateToOnboardAdvisorInput()
                            })
                            onboardAdvisorInput()
                        }
                    }
                }
            }
        )
    }
}