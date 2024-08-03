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

package com.nunchuk.android.transaction.components.receive

import android.content.Context
import android.os.Bundle
import com.nunchuk.android.core.domain.data.PortalAction
import com.nunchuk.android.core.domain.data.VerifyAddress
import com.nunchuk.android.core.nfc.BasePortalActivity
import com.nunchuk.android.core.nfc.PortalDeviceEvent
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.transaction.R
import com.nunchuk.android.transaction.components.receive.address.AddressFragmentFactory
import com.nunchuk.android.transaction.components.receive.address.AddressPagerAdapter
import com.nunchuk.android.transaction.components.receive.address.AddressTab
import com.nunchuk.android.transaction.components.receive.address.AddressTab.UNUSED
import com.nunchuk.android.transaction.components.receive.address.AddressTab.USED
import com.nunchuk.android.transaction.databinding.ActivityTransactionReceiveBinding
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.map

@AndroidEntryPoint
class ReceiveTransactionActivity : BasePortalActivity<ActivityTransactionReceiveBinding>(),
    TabCountChangeListener {

    private lateinit var pagerAdapter: AddressPagerAdapter
    private var currentAddress: String = ""

    private val args: ReceiveTransactionArgs by lazy { ReceiveTransactionArgs.deserializeFrom(intent) }

    override fun initializeBinding() = ActivityTransactionReceiveBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()
        setupViews()
    }

    override fun handleLoading() {
        flowObserver(portalViewModel.state.map { it.isLoading }) {
            showOrHideLoading(
                loading = it,
                title = getString(R.string.nc_put_your_device_near_the_nfc_key_to_verify_address),
                message = currentAddress
            )
        }
    }

    private fun setupViews() {
        val pagers = binding.pagers
        val tabs = binding.tabs

        pagerAdapter =
            AddressPagerAdapter(this, AddressFragmentFactory(args.walletId), supportFragmentManager)
        binding.pagers.offscreenPageLimit = AddressTab.entries.size
        AddressTab.entries.forEach {
            tabs.addTab(tabs.newTab().setText(it.titleId(this@ReceiveTransactionActivity, 0)))
        }
        val position = pagers.currentItem
        pagers.adapter = pagerAdapter
        tabs.setupWithViewPager(pagers)
        pagers.currentItem = position

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    override fun handlePortalAction(action: PortalAction) {
        currentAddress = (action as? VerifyAddress)?.address.orEmpty()
        super.handlePortalAction(action)
    }

    override fun onHandledPortalAction(event: PortalDeviceEvent) {}

    override fun onChange(tab: AddressTab, count: Int) {
        binding.tabs.getTabAt(tab.position)?.apply {
            text = tab.titleId(this@ReceiveTransactionActivity, count)
        }
    }

    companion object {
        fun start(activityContext: Context, walletId: String) {
            activityContext.startActivity(
                ReceiveTransactionArgs(walletId = walletId).buildIntent(
                    activityContext
                )
            )
        }
    }

}

fun AddressTab.titleId(context: Context, count: Int) = when (this) {
    UNUSED -> context.resources.getQuantityString(
        R.plurals.nc_transaction_unused_with_count,
        count,
        count
    )

    USED -> context.resources.getQuantityString(
        R.plurals.nc_transaction_used_with_count,
        count,
        count
    )
}