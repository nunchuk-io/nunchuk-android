package com.nunchuk.android.signer.ss.create

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.GridLayoutManager
import com.nunchuk.android.arch.BaseActivity
import com.nunchuk.android.arch.vm.NunchukFactory
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.signer.databinding.ActivityCreateSeedBinding
import com.nunchuk.android.signer.ss.create.CreateNewSeedEvent.GenerateMnemonicCodeErrorEvent
import com.nunchuk.android.signer.ss.create.CreateNewSeedEvent.OpenSelectPhraseEvent
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.setLightStatusBar
import javax.inject.Inject

class CreateNewSeedActivity : BaseActivity() {

    @Inject
    lateinit var factory: NunchukFactory

    @Inject
    lateinit var navigator: NunchukNavigator

    private val viewModel: CreateNewSeedViewModel by lazy {
        ViewModelProviders.of(this, factory).get(CreateNewSeedViewModel::class.java)
    }

    private lateinit var binding: ActivityCreateSeedBinding

    private lateinit var adapter: CreateNewSeedAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()

        binding = ActivityCreateSeedBinding.inflate(layoutInflater)
        setContentView(binding.root)

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