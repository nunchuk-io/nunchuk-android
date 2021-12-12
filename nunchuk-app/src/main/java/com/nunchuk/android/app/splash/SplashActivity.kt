package com.nunchuk.android.app.splash

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import com.nunchuk.android.app.splash.SplashEvent.*
import com.nunchuk.android.arch.R
import com.nunchuk.android.arch.vm.ViewModelFactory
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.bus.RestartAppEventBus
import com.nunchuk.android.databinding.ActivitySplashBinding
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.setTransparentStatusBar
import javax.inject.Inject

internal class SplashActivity : BaseActivity<ActivitySplashBinding>() {

    @Inject
    lateinit var factory: ViewModelFactory

    private val viewModel: SplashViewModel by viewModels { factory }

    override fun initializeBinding() = ActivitySplashBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTransparentStatusBar()
        subscribeEvents()
    }

    override fun onResume() {
        super.onResume()
        viewModel.handleNavigation()
        RestartAppEventBus.instance().subscribe {
            finish()
            startActivity(Intent(this, SplashActivity::class.java))
            overridePendingTransition(R.anim.enter, R.anim.exit)
        }
    }

    override fun onDestroy() {
        RestartAppEventBus.instance().unsubscribe()
        super.onDestroy()
    }

    private fun subscribeEvents() {
        viewModel.event.observe(this, ::handleEvent)
    }

    private fun handleEvent(event: SplashEvent) {
        when (event) {
            NavActivateAccountEvent -> navigator.openChangePasswordScreen(this)
            NavSignInEvent -> navigator.openSignInScreen(this)
            NavHomeScreenEvent -> navigator.openMainScreen(this)
            is InitErrorEvent -> NCToastMessage(this).showError(event.error)
        }
        finish()
    }

}

