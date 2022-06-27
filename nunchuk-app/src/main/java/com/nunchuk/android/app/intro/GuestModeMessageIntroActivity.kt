package com.nunchuk.android.app.intro

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.databinding.ActivityGuestModeMessageIntroBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
internal class GuestModeMessageIntroActivity : BaseActivity<ActivityGuestModeMessageIntroBinding>() {

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

