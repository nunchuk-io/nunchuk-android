/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *							          *
 * Copyright (C) 2022 Nunchuk								              *
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

package com.nunchuk.android.signer.software.components.primarykey.intro.replace

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import com.nunchuk.android.core.account.PrimaryKeySignerInfoHolder
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.signer.PrimaryKeyFlow
import com.nunchuk.android.signer.software.R
import com.nunchuk.android.signer.software.databinding.ActivityPkeyReplaceKeyIntroBinding
import com.nunchuk.android.widget.NCInputDialog
import com.nunchuk.android.widget.util.setLightStatusBar
import com.nunchuk.android.widget.util.setOnDebounceClickListener
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PKeyReplaceKeyIntroActivity : BaseActivity<ActivityPkeyReplaceKeyIntroBinding>() {

    @Inject
    lateinit var primaryKeySignerInfoHolder: PrimaryKeySignerInfoHolder

    private val viewModel: PKeyReplaceKeyIntroViewModel by viewModels()

    override fun initializeBinding() = ActivityPkeyReplaceKeyIntroBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()
        setupViews()
        observeEvent()
    }

    private fun observeEvent() {
        viewModel.event.observe(this, ::handleEvent)
    }

    private fun handleEvent(event: PKeyReplaceKeyIntroEvent) {
        when (event) {
            is PKeyReplaceKeyIntroEvent.LoadingEvent -> showOrHideLoading(loading = event.loading)
            is PKeyReplaceKeyIntroEvent.CheckNeedPassphraseSent -> showEnterPassphraseDialog(event.isNeeded)
        }
    }

    private fun setupViews() {
        binding.continueBtn.setOnDebounceClickListener {
            viewModel.checkNeedPassphraseSent()
        }
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun showEnterPassphraseDialog(isNeeded: Boolean) {
        if (isNeeded) {
            NCInputDialog(this).showDialog(
                title = getString(R.string.nc_transaction_enter_passphrase),
                onConfirmed = {
                    openNextScreen(it)
                }
            )
        } else {
            openNextScreen("")
        }
    }

    private fun openNextScreen(passphrase: String) {
        navigator.openAddPrimaryKeyScreen(
            this,
            passphrase = passphrase,
            primaryKeyFlow = PrimaryKeyFlow.REPLACE
        )
    }

    companion object {
        fun start(activityContext: Context) {
            activityContext.startActivity(
                Intent(
                    activityContext,
                    PKeyReplaceKeyIntroActivity::class.java
                )
            )
        }
    }
}