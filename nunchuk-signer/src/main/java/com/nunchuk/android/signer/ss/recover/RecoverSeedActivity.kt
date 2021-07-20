package com.nunchuk.android.signer.ss.recover

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nunchuk.android.arch.vm.NunchukFactory
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.databinding.ActivityRecoverSeedBinding
import com.nunchuk.android.signer.ss.recover.RecoverSeedEvent.*
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
    }

    private fun handleEvent(event: RecoverSeedEvent) {
        when (event) {
            MnemonicRequiredEvent -> binding.mnemonic.setError(getString(R.string.nc_text_required))
            InvalidMnemonicEvent -> binding.mnemonic.setError(getString(R.string.nc_error_invalid_signer_spec))
            is ValidMnemonicEvent -> navigator.openAddSoftwareSignerNameScreen(this, event.mnemonic)
            is UpdateMnemonicEvent -> updateMnemonic(event.mnemonic)
            is CanGoNextStepEvent -> bindButtonState(event.canGoNext)
        }
    }

    private fun bindButtonState(enable: Boolean) {
        binding.btnContinue.isEnabled = enable
        binding.btnContinue.isClickable = enable
        if (enable) {
            binding.btnContinue.setTextColor(ContextCompat.getColor(this, R.color.nc_white_color))
            binding.btnContinue.background = ContextCompat.getDrawable(this, R.drawable.nc_rounded_dark_background)
        } else {
            binding.btnContinue.setTextColor(ContextCompat.getColor(this, R.color.nc_grey_dark_color))
            binding.btnContinue.background = ContextCompat.getDrawable(this, R.drawable.nc_rounded_whisper_disable_background)
        }
    }

    private fun updateMnemonic(mnemonic: String) {
        binding.mnemonic.getEditTextView().setText(mnemonic)
        binding.mnemonic.getEditTextView().setSelection(mnemonic.length)
    }

    private fun setupViews() {
        binding.mnemonic.heightExtended(resources.getDimensionPixelSize(R.dimen.nc_height_60))
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
