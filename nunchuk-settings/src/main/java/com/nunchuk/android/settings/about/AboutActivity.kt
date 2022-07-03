package com.nunchuk.android.settings.about

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.util.CONTACT_EMAIL
import com.nunchuk.android.core.util.TWITTER_LINK
import com.nunchuk.android.core.util.openExternalLink
import com.nunchuk.android.core.util.sendEmail
import com.nunchuk.android.settings.R
import com.nunchuk.android.settings.databinding.ActivityAboutBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AboutActivity : BaseActivity<ActivityAboutBinding>() {
    private val viewModel by viewModels<AboutViewModel>()

    override fun initializeBinding(): ActivityAboutBinding = ActivityAboutBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initViews()
        registerEvents()
    }

    private fun registerEvents() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        binding.icTwitter.setOnClickListener { openExternalLink(TWITTER_LINK) }
        binding.tvFollowUs.setOnClickListener { openExternalLink(TWITTER_LINK) }

        binding.icEmail.setOnClickListener { sendEmail(email = CONTACT_EMAIL) }
        binding.tvEmail.setOnClickListener { sendEmail(email = CONTACT_EMAIL) }
    }

    @SuppressLint("SetTextI18n")
    private fun initViews() {
        binding.tvVersion.text = "${getString(R.string.nc_version)} ${viewModel.version}"
    }

    companion object {
        fun start(activityContext: Context) {
            activityContext.startActivity(Intent(activityContext, AboutActivity::class.java))
        }
    }
}