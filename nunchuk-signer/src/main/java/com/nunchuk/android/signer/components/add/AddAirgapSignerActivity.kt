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

package com.nunchuk.android.signer.components.add

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.navigation.fragment.NavHostFragment
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.model.MembershipStep
import com.nunchuk.android.signer.R
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.utils.serializable
import com.nunchuk.android.widget.databinding.ActivityNavigationBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddAirgapSignerActivity : BaseActivity<ActivityNavigationBinding>() {

    val isMembershipFlow: Boolean by lazy {
        intent.getBooleanExtra(
            EXTRA_IS_MEMBERSHIP_FLOW,
            false
        )
    }
    val signerTag: SignerTag? by lazy { intent.serializable(EXTRA_SIGNER_TAG) }
    val groupId: String by lazy { intent.getStringExtra(EXTRA_GROUP_ID).orEmpty() }
    val xfp: String? by lazy { intent.getStringExtra(EXTRA_XFP) }
    val newIndex: Int by lazy { intent.getIntExtra(EXTRA_NEW_INDEX, 0) }
    val replacedXfp: String? by lazy { intent.getStringExtra(EXTRA_REPLACED_XFP) }
    val walletId: String by lazy { intent.getStringExtra(EXTRA_WALLET_ID).orEmpty() }
    val step: MembershipStep? by lazy { intent.serializable(EXTRA_MEMBERSHIP_STEP) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val navHostFragment =
            (supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment)
        val inflater = navHostFragment.navController.navInflater
        val graph = inflater.inflate(R.navigation.airgap_navigation)

        if (signerTag == SignerTag.JADE) {
            graph.setStartDestination(R.id.airgapActionIntroFragment)
        } else {
            graph.setStartDestination(R.id.airgapIntroFragment)
        }
        navHostFragment.navController.setGraph(graph, intent.extras)
    }

    companion object {
        private const val EXTRA_IS_MEMBERSHIP_FLOW = "is_membership_flow"
        private const val EXTRA_SIGNER_TAG = "signer_tag"
        private const val EXTRA_GROUP_ID = "group_id"
        private const val EXTRA_XFP = "xfp"
        private const val EXTRA_NEW_INDEX = "new_index"
        private const val EXTRA_REPLACED_XFP = "replaced_xfp"
        private const val EXTRA_WALLET_ID = "wallet_id"
        private const val EXTRA_MEMBERSHIP_STEP = "step"

        fun buildIntent(
            activityContext: Context,
            isMembershipFlow: Boolean,
            tag: SignerTag?,
            groupId: String,
            xfp: String?,
            newIndex: Int,
            replacedXfp: String? = null,
            walletId: String = "",
            step: MembershipStep? = null
        ) = Intent(
            activityContext,
            AddAirgapSignerActivity::class.java
        ).apply {
            putExtra(EXTRA_IS_MEMBERSHIP_FLOW, isMembershipFlow)
            putExtra(EXTRA_SIGNER_TAG, tag)
            putExtra(EXTRA_GROUP_ID, groupId)
            putExtra(EXTRA_XFP, xfp)
            putExtra(EXTRA_NEW_INDEX, newIndex)
            putExtra(EXTRA_REPLACED_XFP, replacedXfp)
            putExtra(EXTRA_WALLET_ID, walletId)
            putExtra(EXTRA_MEMBERSHIP_STEP, step)
        }
    }

    override fun initializeBinding(): ActivityNavigationBinding {
        return ActivityNavigationBinding.inflate(layoutInflater)
    }
}