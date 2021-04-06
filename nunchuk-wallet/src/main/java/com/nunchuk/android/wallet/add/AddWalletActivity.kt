package com.nunchuk.android.wallet.add

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.ViewModelProviders
import com.nunchuk.android.arch.BaseActivity
import com.nunchuk.android.arch.vm.NunchukFactory
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.wallet.databinding.ActivityAddWalletBinding
import javax.inject.Inject

class AddWalletActivity : BaseActivity() {

    @Inject
    lateinit var factory: NunchukFactory

    @Inject
    lateinit var navigator: NunchukNavigator

    private val viewModel: AddWalletViewModel by lazy {
        ViewModelProviders.of(this, factory).get(AddWalletViewModel::class.java)
    }

    private lateinit var binding: ActivityAddWalletBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAddWalletBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()

        observeEvent()
    }

    private fun observeEvent() {
        viewModel.event.observe(this) {
        }
    }

    private fun setupViews() {
    }

    companion object {
        fun start(activityContext: Context) {
            activityContext.startActivity(Intent(activityContext, AddWalletActivity::class.java))
        }
    }

}