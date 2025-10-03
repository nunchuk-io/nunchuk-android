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
import com.nunchuk.android.core.signer.OnChainAddSignerParam
import com.nunchuk.android.model.MembershipStep
import com.nunchuk.android.nav.args.AddAirSignerArgs
import com.nunchuk.android.signer.R
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.widget.databinding.ActivityNavigationBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddAirgapSignerActivity : BaseActivity<ActivityNavigationBinding>() {

    private val args: AddAirSignerArgs by lazy {
        AddAirSignerArgs.deserializeFrom(intent)
    }
    
    val isMembershipFlow: Boolean by lazy { args.isMembershipFlow }
    val signerTag: SignerTag? by lazy { args.tag }
    val groupId: String by lazy { args.groupId }
    val xfp: String? by lazy { args.xfp }
    val newIndex: Int by lazy { args.newIndex }
    val replacedXfp: String? by lazy { args.replacedXfp }
    val walletId: String by lazy { args.walletId }
    val step: MembershipStep? by lazy { args.step }
    val onChainAddSignerParam: OnChainAddSignerParam? by lazy { args.onChainAddSignerParam }

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
        fun buildIntent(
            activityContext: Context,
            args: AddAirSignerArgs,
        ) = Intent(
            activityContext,
            AddAirgapSignerActivity::class.java
        ).apply {
            putExtras(args.buildBundle())
        }
    }

    override fun initializeBinding(): ActivityNavigationBinding {
        return ActivityNavigationBinding.inflate(layoutInflater)
    }
}