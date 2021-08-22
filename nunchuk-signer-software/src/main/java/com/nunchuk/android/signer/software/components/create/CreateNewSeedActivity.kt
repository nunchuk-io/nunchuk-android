package com.nunchuk.android.signer.software.components.create

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.nunchuk.android.arch.vm.NunchukFactory
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.signer.software.components.create.CreateNewSeedEvent.GenerateMnemonicCodeErrorEvent
import com.nunchuk.android.signer.software.components.create.CreateNewSeedEvent.OpenSelectPhraseEvent
import com.nunchuk.android.signer.software.databinding.ActivityCreateSeedBinding
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.setLightStatusBar
import javax.inject.Inject

class CreateNewSeedActivity : BaseActivity<ActivityCreateSeedBinding>() {

    @Inject
    lateinit var factory: NunchukFactory

    private val viewModel: CreateNewSeedViewModel by viewModels { factory }

    override fun initializeBinding() = ActivityCreateSeedBinding.inflate(layoutInflater)

    private lateinit var adapter: CreateNewSeedAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()
        setupViews()
        observeEvent()
        viewModel.init()
    }

    private fun observeEvent() {
        viewModel.event.observe(this, ::handleEvent)
        viewModel.state.observe(this, ::handleState)
    }

    private fun handleState(state: CreateNewSeedState) {
        adapter.items = state.seeds
    }

    private fun handleEvent(event: CreateNewSeedEvent) {
        when (event) {
            is GenerateMnemonicCodeErrorEvent -> NCToastMessage(this).showWarning(event.message)
            is OpenSelectPhraseEvent -> navigator.openSelectPhraseScreen(this, event.mnemonic)
        }
    }

    private fun setupViews() {
        adapter = CreateNewSeedAdapter()
        binding.seedGrid.layoutManager = GridLayoutManager(this, COLUMNS)
        binding.seedGrid.adapter = adapter
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        binding.btnContinue.setOnClickListener { viewModel.handleContinueEvent() }
    }

    companion object {
        private const val COLUMNS = 3

        fun start(activityContext: Context) {
            activityContext.startActivity(Intent(activityContext, CreateNewSeedActivity::class.java))
        }
    }

}