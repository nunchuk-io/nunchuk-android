package com.nunchuk.android.app.splash

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import com.nunchuk.android.app.splash.SplashEvent.*
import com.nunchuk.android.arch.R
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.databinding.ActivitySplashBinding
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.setTransparentStatusBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
internal class SplashActivity : BaseActivity<ActivitySplashBinding>() {

    private val viewModel: SplashViewModel by viewModels()

    override fun initializeBinding() = ActivitySplashBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTransparentStatusBar()
        subscribeEvents()
    }

    override fun onResume() {
        super.onResume()
        viewModel.handleNavigation()
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

    companion object {

        fun start(activity: Activity) {
            activity.startActivity(Intent(activity, SplashActivity::class.java))
            activity.overridePendingTransition(R.anim.enter, R.anim.exit)
        }

    }

}

