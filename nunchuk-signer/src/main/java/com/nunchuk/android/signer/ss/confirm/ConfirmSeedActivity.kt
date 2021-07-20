package com.nunchuk.android.signer.ss.confirm

import android.content.Context
import android.os.Bundle
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.nunchuk.android.arch.vm.NunchukFactory
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.databinding.ActivityConfirmSeedBinding
import com.nunchuk.android.signer.ss.confirm.ConfirmSeedEvent.ConfirmSeedCompletedEvent
import com.nunchuk.android.signer.ss.confirm.ConfirmSeedEvent.SelectedIncorrectWordEvent
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.setLightStatusBar
import javax.inject.Inject

class ConfirmSeedActivity : BaseActivity<ActivityConfirmSeedBinding>() {

    @Inject
    lateinit var factory: NunchukFactory

    private val viewModel: ConfirmSeedViewModel by viewModels { factory }

    override fun initializeBinding() = ActivityConfirmSeedBinding.inflate(layoutInflater)

    private val args: ConfirmSeedArgs by lazy { ConfirmSeedArgs.deserializeFrom(intent) }

    private lateinit var adapter: ConfirmSeedAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()
        setupViews()
        observeEvent()
        viewModel.init(args.mnemonic)
    }

    private fun observeEvent() {
        viewModel.event.observe(this, ::handleEvent)
        viewModel.state.observe(this, ::handleState)
    }

    private fun handleState(state: ConfirmSeedState) {
        adapter.items = state.groups
    }

    private fun handleEvent(event: ConfirmSeedEvent) {
        when (event) {
            ConfirmSeedCompletedEvent -> openSetNameScreen()
            SelectedIncorrectWordEvent -> NCToastMessage(this).showError(getString(R.string.nc_ssigner_confirm_seed_error))
        }
    }

    private fun openSetNameScreen() {
        navigator.openAddSoftwareSignerNameScreen(this, args.mnemonic)
    }

    private fun setupViews() {
        adapter = ConfirmSeedAdapter(viewModel::updatePhraseWordGroup)

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        binding.btnContinue.setOnClickListener { viewModel.handleContinueEvent() }
    }

    companion object {

        fun start(activityContext: Context, mnemonic: String) {
            activityContext.startActivity(ConfirmSeedArgs(mnemonic = mnemonic).buildIntent(activityContext))
        }
    }

}