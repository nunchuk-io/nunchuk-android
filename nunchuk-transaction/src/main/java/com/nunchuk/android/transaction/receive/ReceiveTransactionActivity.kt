package com.nunchuk.android.transaction.receive

import android.content.Context
import android.os.Bundle
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.transaction.R
import com.nunchuk.android.transaction.databinding.ActivityTransactionReceiveBinding
import com.nunchuk.android.transaction.receive.address.AddressFragmentFactory
import com.nunchuk.android.transaction.receive.address.AddressPagerAdapter
import com.nunchuk.android.transaction.receive.address.AddressTab
import com.nunchuk.android.transaction.receive.address.AddressTab.*
import com.nunchuk.android.widget.util.setLightStatusBar

class ReceiveTransactionActivity : BaseActivity<ActivityTransactionReceiveBinding>(), TabCountChangeListener {

    private lateinit var pagerAdapter: AddressPagerAdapter

    private val args: ReceiveTransactionArgs by lazy { ReceiveTransactionArgs.deserializeFrom(intent) }

    override fun initializeBinding() = ActivityTransactionReceiveBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()
        setupViews()
    }

    private fun setupViews() {
        val pagers = binding.pagers
        val tabs = binding.tabs

        pagerAdapter = AddressPagerAdapter(this, AddressFragmentFactory(args.walletId), supportFragmentManager)
        binding.pagers.offscreenPageLimit = values().size
        values().forEach {
            tabs.addTab(tabs.newTab().setText(it.titleId(this@ReceiveTransactionActivity)))
        }
        val position = pagers.currentItem
        pagers.adapter = pagerAdapter
        tabs.setupWithViewPager(pagers)
        pagers.currentItem = position

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    override fun onChange(tab: AddressTab, count: Int) {
        binding.tabs.getTabAt(tab.position)?.apply {
            text = "${tab.titleId(this@ReceiveTransactionActivity)} ($count)"
        }
    }

    companion object {
        fun start(activityContext: Context, walletId: String) {
            activityContext.startActivity(ReceiveTransactionArgs(walletId = walletId).buildIntent(activityContext))
        }
    }

}

fun AddressTab.titleId(context: Context) = when (this) {
    UNUSED -> context.getString(R.string.nc_transaction_unused)
    USED -> context.getString(R.string.nc_transaction_used)
}