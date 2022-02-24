package com.nunchuk.android.app.intro

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.nunchuk.android.arch.vm.ViewModelFactory
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.databinding.ActivityGuestModeMessageIntroBinding
import javax.inject.Inject

internal class GuestModeMessageIntroActivity : BaseActivity<ActivityGuestModeMessageIntroBinding>() {

    @Inject
    lateinit var factory: ViewModelFactory

    override fun initializeBinding() = ActivityGuestModeMessageIntroBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.signIn.setOnClickListener {
            navigator.openSignInScreen(this)
        }
        binding.signUp.setOnClickListener {
            navigator.openSignUpScreen(this)
        }
        binding.ivClose.setOnClickListener {
            onBackPressed()
        }
    }

    companion object {
        fun start(activityContext: Context) {
            activityContext.startActivity(
                Intent(
                    activityContext,
                    GuestModeMessageIntroActivity::class.java
                )
            )
        }
    }
}

