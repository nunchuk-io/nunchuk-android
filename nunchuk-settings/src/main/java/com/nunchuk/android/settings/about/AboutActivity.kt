/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2022, 2023 Nunchuk                                       *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

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
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AboutActivity : BaseActivity<ActivityAboutBinding>() {
    private val viewModel by viewModels<AboutViewModel>()

    override fun initializeBinding(): ActivityAboutBinding = ActivityAboutBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setLightStatusBar()

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