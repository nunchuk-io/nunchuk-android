package com.nunchuk.android.app.intro

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import com.nunchuk.android.app.splash.GuestModeEvent
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.databinding.ActivityIntroBinding
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.setTransparentStatusBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
internal class IntroActivity : BaseActivity<ActivityIntroBinding>() {
    private val viewModel: GuestModeViewModel by viewModels()

    override fun initializeBinding() = ActivityIntroBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTransparentStatusBar()

        binding.btnGetStarted.setOnClickListener {
            viewModel.initGuestModeNunchuk()
        }

        subscribeEvents()
    }

    private fun handleInitGuestModeNunchukSuccess() {
        hideLoading()
        finish()
        navigator.openMainScreen(this)
        overridePendingTransition(0, 0)
    }

    private fun subscribeEvents() {
        viewModel.event.observe(this, ::handleEvent)
    }

    private fun handleEvent(event: GuestModeEvent) {
        when (event) {
            GuestModeEvent.InitSuccessEvent -> handleInitGuestModeNunchukSuccess()
            is GuestModeEvent.InitErrorEvent -> NCToastMessage(this).showError(event.error)
            is GuestModeEvent.LoadingEvent -> showLoading()
        }
    }

    companion object {
        fun start(activityContext: Context) {
            activityContext.startActivity(Intent(activityContext, IntroActivity::class.java))
        }
    }
}

