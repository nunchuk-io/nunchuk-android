package com.nunchuk.android.wallet.confirm

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.ViewModelProviders
import com.nunchuk.android.arch.BaseActivity
import com.nunchuk.android.arch.vm.NunchukFactory
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.wallet.databinding.ActivityWalletConfirmationBinding
import javax.inject.Inject

class WalletConfirmActivity : BaseActivity() {

    @Inject
    lateinit var factory: NunchukFactory

    @Inject
    lateinit var navigator: NunchukNavigator

    private val viewModel: WalletConfirmViewModel by lazy {
        ViewModelProviders.of(this, factory).get(WalletConfirmViewModel::class.java)
    }

    private lateinit var binding: ActivityWalletConfirmationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityWalletConfirmationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        observeEvent()

        viewModel.init()
    }

    private fun observeEvent() {
        viewModel.event.observe(this, ::handleEvent)
        viewModel.state.observe(this, ::handleState)
    }

    private fun handleEvent(event: WalletConfirmEvent) {
    }

    private fun handleState(state: WalletConfirmState) {
    }

    private fun setupViews() {
    }

    companion object {

        fun start(activityContext: Context, walletName: String, walletType: WalletType, addressType: AddressType) {
            activityContext.startActivity(Intent(activityContext, WalletConfirmActivity::class.java))
        }
    }

}