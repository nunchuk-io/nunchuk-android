package com.nunchuk.android.app.intro

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import com.nunchuk.android.app.splash.GuestModeEvent
import com.nunchuk.android.app.splash.GuestModeViewModel
import com.nunchuk.android.arch.vm.ViewModelFactory
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.guestmode.SignInMode
import com.nunchuk.android.core.guestmode.SignInModeHolder
import com.nunchuk.android.core.util.hideLoading
import com.nunchuk.android.core.util.showLoading
import com.nunchuk.android.databinding.ActivityGuestModeIntroBinding
import com.nunchuk.android.widget.NCToastMessage
import javax.inject.Inject

internal class GuestModeIntroActivity : BaseActivity<ActivityGuestModeIntroBinding>() {

    @Inject
    lateinit var factory: ViewModelFactory

    override fun initializeBinding() = ActivityGuestModeIntroBinding.inflate(layoutInflater)

    private val viewModel: GuestModeViewModel by viewModels { factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.btnGotIt.setOnClickListener {
            viewModel.initGuestModeNunchuk()
        }

        subscribeEvents()
    }

    private fun handleInitGuestModeNunchukSuccess() {
        hideLoading()
        finish()
        SignInModeHolder.currentMode = SignInMode.GUEST_MODE
        navigator.openMainScreen(this)
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
            activityContext.startActivity(
                Intent(
                    activityContext,
                    GuestModeIntroActivity::class.java
                )
            )
        }
    }
}

