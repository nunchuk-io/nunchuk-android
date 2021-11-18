package com.nunchuk.android.signer.software.components.recover

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nunchuk.android.arch.vm.NunchukFactory
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.util.bindEnableState
import com.nunchuk.android.signer.software.components.recover.RecoverSeedEvent.*
import com.nunchuk.android.signer.software.R
import com.nunchuk.android.signer.software.databinding.ActivityRecoverSeedBinding
import com.nunchuk.android.widget.util.addTextChangedCallback
import com.nunchuk.android.widget.util.heightExtended
import com.nunchuk.android.widget.util.setLightStatusBar
import javax.inject.Inject

class RecoverSeedActivity : BaseActivity<ActivityRecoverSeedBinding>() {

    @Inject
    lateinit var factory: NunchukFactory

    private val viewModel: RecoverSeedViewModel by viewModels { factory }

    private lateinit var adapter: RecoverSeedSuggestionAdapter

    override fun initializeBinding() = ActivityRecoverSeedBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()
        setupViews()
        observeEvent()
    }

    private fun observeEvent() {
        viewModel.event.observe(this, ::handleEvent)
        viewModel.state.observe(this, ::handleState)
    }

    private fun handleState(state: RecoverSeedState) {
        adapter.items = state.suggestions
        binding.recyclerView.scrollToPosition(0)
    }

    private fun handleEvent(event: RecoverSeedEvent) {
        when (event) {
            MnemonicRequiredEvent -> binding.mnemonic.setError(getString(R.string.nc_text_required))
            InvalidMnemonicEvent -> binding.mnemonic.setError(getString(R.string.nc_error_invalid_signer_spec))
            is ValidMnemonicEvent -> navigator.openAddSoftwareSignerNameScreen(this, event.mnemonic)
            is UpdateMnemonicEvent -> updateMnemonic(event.mnemonic)
            is CanGoNextStepEvent -> binding.btnContinue.bindEnableState(event.canGoNext)
        }
    }

    private fun updateMnemonic(mnemonic: String) {
        val withSpace = "$mnemonic "
        binding.mnemonic.getEditTextView().setText(withSpace)
        binding.mnemonic.getEditTextView().setSelection(mnemonic.length + 1)
    }

    private fun setupViews() {
        binding.mnemonic.heightExtended(resources.getDimensionPixelSize(R.dimen.nc_height_120))
        binding.mnemonic.addTextChangedCallback(viewModel::handleInputEvent)
        adapter = RecoverSeedSuggestionAdapter(viewModel::handleSelectWord)
        binding.recyclerView.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        binding.recyclerView.adapter = adapter
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        binding.btnContinue.setOnClickListener {
            viewModel.handleContinueEvent()
        }
    }

    companion object {
        fun start(activityContext: Context) {
            activityContext.startActivity(Intent(activityContext, RecoverSeedActivity::class.java))
        }
    }

}
