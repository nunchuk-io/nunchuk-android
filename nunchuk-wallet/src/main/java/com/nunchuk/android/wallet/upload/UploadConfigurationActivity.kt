package com.nunchuk.android.wallet.upload

import android.content.Context
import android.os.Bundle
import androidx.lifecycle.ViewModelProviders
import com.nunchuk.android.arch.BaseActivity
import com.nunchuk.android.arch.vm.NunchukFactory
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.wallet.databinding.ActivityWalletUploadConfigurationBinding
import javax.inject.Inject

class UploadConfigurationActivity : BaseActivity() {

    @Inject
    lateinit var factory: NunchukFactory

    @Inject
    lateinit var navigator: NunchukNavigator

    private val args: UploadConfigurationArgs by lazy { UploadConfigurationArgs.deserializeFrom(intent) }

    private val viewModel: UploadConfigurationViewModel by lazy {
        ViewModelProviders.of(this, factory).get(UploadConfigurationViewModel::class.java)
    }

    private lateinit var binding: ActivityWalletUploadConfigurationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityWalletUploadConfigurationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        observeEvent()

        viewModel.init()
    }

    private fun setupViews() {
    }

    private fun observeEvent() {
        viewModel.event.observe(this, ::handleEvent)
        viewModel.state.observe(this, ::handleState)
    }

    private fun handleEvent(event: UploadConfigurationEvent) {
    }

    private fun handleState(state: UploadConfigurationState) {

    }

    companion object {

        fun start(activityContext: Context) {
            activityContext.startActivity(UploadConfigurationArgs().buildIntent(activityContext))
        }
    }

}