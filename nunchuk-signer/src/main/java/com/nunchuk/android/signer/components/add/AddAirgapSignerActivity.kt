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

package com.nunchuk.android.signer.components.add

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.view.WindowCompat
import androidx.navigation.fragment.NavHostFragment
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.signer.R
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.utils.serializable
import com.nunchuk.android.widget.databinding.ActivityNavigationBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddAirgapSignerActivity : BaseActivity<ActivityNavigationBinding>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val navHostFragment =
            (supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment)
        navHostFragment.navController.setGraph(R.navigation.airgap_navigation)
        navHostFragment.navController.addOnDestinationChangedListener { _, destination, _ ->
            when(destination.id) {
                R.id.addAirgapSignerFragment ->  WindowCompat.setDecorFitsSystemWindows(window, true)
                else ->  WindowCompat.setDecorFitsSystemWindows(window, false)
            }
        }
    }

    val isMembershipFlow : Boolean by lazy { intent.getBooleanExtra(EXTRA_IS_MEMBERSHIP_FLOW, false) }
    val signerTag : SignerTag? by lazy { intent.serializable(EXTRA_SIGNER_TAG) }

    companion object {
        private const val EXTRA_IS_MEMBERSHIP_FLOW = "is_membership_flow"
        private const val EXTRA_SIGNER_TAG = "signer_tag"

        fun start(activityContext: Context, isMembershipFlow: Boolean, tag: SignerTag?) {
            activityContext.startActivity(
                Intent(
                    activityContext,
                    AddAirgapSignerActivity::class.java
                ).apply {
                    putExtra(EXTRA_IS_MEMBERSHIP_FLOW, isMembershipFlow)
                    putExtra(EXTRA_SIGNER_TAG, tag)
                }
            )
        }
    }

    override fun initializeBinding(): ActivityNavigationBinding {
        return ActivityNavigationBinding.inflate(layoutInflater)
    }
}