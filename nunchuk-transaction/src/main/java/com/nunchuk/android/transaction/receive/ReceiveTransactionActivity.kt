package com.nunchuk.android.transaction.receive

import android.content.Context
import android.os.Bundle
import androidx.lifecycle.ViewModelProviders
import com.nunchuk.android.arch.BaseActivity
import com.nunchuk.android.arch.vm.NunchukFactory
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.transaction.R
import com.nunchuk.android.transaction.databinding.ActivityTransactionReceiveBinding
import com.nunchuk.android.transaction.receive.address.AddressFragmentFactory
import com.nunchuk.android.transaction.receive.address.AddressPagerAdapter
import com.nunchuk.android.transaction.receive.address.AddressTab
import com.nunchuk.android.transaction.receive.address.AddressTab.*
import com.nunchuk.android.widget.util.setLightStatusBar
import javax.inject.Inject

class ReceiveTransactionActivity : BaseActivity(), TabCountChangeListener {

    private lateinit var pagerAdapter: AddressPagerAdapter

    @Inject
    lateinit var factory: NunchukFactory

    @Inject
    lateinit var navigator: NunchukNavigator

    private val args: ReceiveTransactionArgs by lazy { ReceiveTransactionArgs.deserializeFrom(intent) }

    private val viewModel: ReceiveTransactionViewModel by lazy {
        ViewModelProviders.of(this, factory).get(ReceiveTransactionViewModel::class.java)
    }

    private lateinit var binding: ActivityTransactionReceiveBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()

        binding = ActivityTransactionReceiveBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        observeEvent()
    }


    private fun observeEvent() {
        viewModel.event.observe(this, ::handleEvent)
        viewModel.state.observe(this, ::handleState)
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

    private fun handleState(state: ReceiveTransactionState) {

    }

    private fun handleEvent(event: ReceiveTransactionEvent) {
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