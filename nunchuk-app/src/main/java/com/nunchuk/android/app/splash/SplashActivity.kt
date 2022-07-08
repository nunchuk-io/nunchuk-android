package com.nunchuk.android.app.splash

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.nunchuk.android.app.splash.SplashEvent.*
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.setTransparentStatusBar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
internal class SplashActivity : AppCompatActivity() {
    @Inject
    lateinit var navigator: NunchukNavigator

    private val viewModel: SplashViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTransparentStatusBar()
        subscribeEvents()
        viewModel.handleNavigation()
    }

    private fun subscribeEvents() {
        viewModel.event.observe(this, ::handleEvent)
    }

    private fun handleEvent(event: SplashEvent) {
        when (event) {
            NavActivateAccountEvent -> navigator.openChangePasswordScreen(this)
            NavSignInEvent -> navigator.openSignInScreen(this, false)
            NavHomeScreenEvent -> navigator.openMainScreen(this)
            NavIntroEvent -> navigator.openIntroScreen(this)
            is InitErrorEvent -> NCToastMessage(this).showError(event.error)
        }
        overridePendingTransition(0,0)
        finish()
    }
}

