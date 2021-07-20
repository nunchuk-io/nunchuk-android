package com.nunchuk.android.app.intro

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.databinding.ActivityIntroBinding
import com.nunchuk.android.widget.util.setTransparentStatusBar

internal class IntroActivity : BaseActivity<ActivityIntroBinding>() {

    override fun initializeBinding() = ActivityIntroBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTransparentStatusBar()

        binding.btnGetStarted.setOnClickListener {
            finish()
            navigator.openSignUpScreen(this)
        }
    }

    companion object {
        fun start(activityContext: Context) {
            activityContext.startActivity(Intent(activityContext, IntroActivity::class.java))
        }
    }
}

