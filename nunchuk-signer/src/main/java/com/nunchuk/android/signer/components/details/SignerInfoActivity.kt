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

package com.nunchuk.android.signer.components.details

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.navigation.fragment.NavHostFragment
import com.nunchuk.android.core.domain.membership.WalletsExistingKey
import com.nunchuk.android.core.nfc.BaseNfcActivity
import com.nunchuk.android.signer.R
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.widget.databinding.ActivityNavigationBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SignerInfoActivity : BaseNfcActivity<ActivityNavigationBinding>() {

    override fun initializeBinding() = ActivityNavigationBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
        val navController = navHostFragment.navController
        val inflater = navHostFragment.navController.navInflater
        val graph = inflater.inflate(R.navigation.signer_info_navigation)
        navController.setGraph(graph, intent.extras)
    }
    companion object {

        fun start(
            activityContext: Context,
            isMasterSigner: Boolean,
            id: String,
            masterFingerprint: String,
            name: String,
            type: SignerType,
            derivationPath: String,
            justAdded: Boolean = false,
            setPassphrase: Boolean = false,
            isInWallet: Boolean,
            isInAssistedWallet: Boolean,
            isReplacePrimaryKey: Boolean = false,
            customMessage: String,
            existingKey: WalletsExistingKey? = null,
        ) {
            activityContext.startActivity(Intent(activityContext, SignerInfoActivity::class.java).apply {
                putExtras(SignerInfoFragmentArgs(
                    id = id,
                    name = name,
                    derivationPath = derivationPath,
                    justAdded = justAdded,
                    signerType = type,
                    setPassphrase = setPassphrase,
                    masterFingerprint = masterFingerprint,
                    isInWallet = isInWallet,
                    isInAssistedWallet = isInAssistedWallet,
                    isReplacePrimaryKey = isReplacePrimaryKey,
                    customMessage = customMessage,
                    isMasterSigner = isMasterSigner,
                    existingKey = existingKey
                ).toBundle())
            })
        }
    }

}