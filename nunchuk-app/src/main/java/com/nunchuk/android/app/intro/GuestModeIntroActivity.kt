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

package com.nunchuk.android.app.intro

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import com.nunchuk.android.app.splash.GuestModeEvent
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.databinding.ActivityGuestModeIntroBinding
import com.nunchuk.android.widget.NCToastMessage
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
internal class GuestModeIntroActivity : BaseActivity<ActivityGuestModeIntroBinding>() {

    override fun initializeBinding() = ActivityGuestModeIntroBinding.inflate(layoutInflater)

    private val viewModel: GuestModeViewModel by viewModels()

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
        navigator.openMainScreen(this)
    }

    private fun subscribeEvents() {
        viewModel.event.observe(this, ::handleEvent)
    }

    private fun handleEvent(event: GuestModeEvent) {
        when (event) {
            GuestModeEvent.InitSuccessEvent -> handleInitGuestModeNunchukSuccess()
            is GuestModeEvent.InitErrorEvent -> NCToastMessage(this).showError(event.error)
            is GuestModeEvent.LoadingEvent -> showOrHideLoading(event.loading)
            GuestModeEvent.OpenSignInScreen -> {}
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

