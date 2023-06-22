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

package com.nunchuk.android.wallet.components.coin

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.NavHostFragment
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.push.PushEvent
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.toAmount
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.utils.serializable
import com.nunchuk.android.wallet.R
import com.nunchuk.android.wallet.components.coin.list.CoinListEvent
import com.nunchuk.android.wallet.components.coin.list.CoinListViewModel
import com.nunchuk.android.widget.databinding.ActivityNavigationBinding
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CoinActivity : BaseActivity<ActivityNavigationBinding>() {
    private val viewMode: CoinListViewModel by viewModels()
    override fun initializeBinding(): ActivityNavigationBinding =
        ActivityNavigationBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setLightStatusBar()
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
        val inflater = navHostFragment.navController.navInflater
        val graph = inflater.inflate(R.navigation.coin_navigation)
        graph.setStartDestination(
            when (intent.serializable<CoinScreen>(KEY_SCREEN)!!) {
                CoinScreen.SELECTION_VIEW -> R.id.coinListFragment
                CoinScreen.DETAIL -> R.id.coinDetailFragment
                CoinScreen.SEARCH -> R.id.coinSearchFragment
            }
        )
        navHostFragment.navController.setGraph(graph, intent.extras)
        flowObserver(viewMode.event) {
            when (it) {
                is CoinListEvent.Loading -> showOrHideLoading(it.isLoading)
                else -> Unit
            }
        }
        flowObserver(pushEventManager.event, Lifecycle.State.CREATED) {
            if (it is PushEvent.TransactionCreatedEvent) {
                finish()
            }
        }
    }

    companion object {
        private const val KEY_WALLET_ID = "wallet_id"
        private const val KEY_SCREEN = "screen"
        private const val KEY_TX_ID = "txId"
        private const val KEY_AMOUNT = "amount"
        private const val KEY_INPUT = "inputs"
        private const val KEY_OUTPUT = "output"

        fun buildIntent(
            context: Context,
            walletId: String,
            txId: String = "",
            inputs: List<UnspentOutput> = emptyList(),
            amount: Double = 0.0,
        ) = Intent(context, CoinActivity::class.java).apply {
            putExtra(KEY_WALLET_ID, walletId)
            putExtra(KEY_TX_ID, txId)
            putExtra(KEY_AMOUNT, amount.toAmount())
            if (inputs.isNotEmpty()) {
                putExtra(KEY_INPUT, inputs.toTypedArray())
                putExtra(KEY_SCREEN, CoinScreen.SEARCH)
            } else {
                putExtra(KEY_SCREEN, CoinScreen.SELECTION_VIEW)
            }
        }

        fun buildIntent(
            context: Context, walletId: String, output: UnspentOutput
        ) = Intent(context, CoinActivity::class.java).apply {
            putExtra(KEY_WALLET_ID, walletId)
            putExtra(KEY_SCREEN, CoinScreen.DETAIL)
            putExtra(KEY_OUTPUT, output)
        }
    }
}