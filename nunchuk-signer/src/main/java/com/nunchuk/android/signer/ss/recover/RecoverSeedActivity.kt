package com.nunchuk.android.signer.ss.recover

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nunchuk.android.arch.BaseActivity
import com.nunchuk.android.arch.vm.NunchukFactory
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.databinding.ActivityRecoverSeedBinding
import com.nunchuk.android.signer.ss.recover.RecoverSeedEvent.*
import com.nunchuk.android.widget.util.SimpleTextWatcher
import com.nunchuk.android.widget.util.heightExtended
import com.nunchuk.android.widget.util.setLightStatusBar
import javax.inject.Inject

class RecoverSeedActivity : BaseActivity() {

    @Inject
    lateinit var factory: NunchukFactory

    @Inject
    lateinit var navigator: NunchukNavigator

    private val viewModel: RecoverSeedViewModel by lazy {
        ViewModelProviders.of(this, factory).get(RecoverSeedViewModel::class.java)
    }

    private lateinit var binding: ActivityRecoverSeedBinding

    private lateinit var adapter: RecoverSeedSuggestionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()

        binding = ActivityRecoverSeedBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
            binding.btnContinue.background = ContextCompat.getDrawable(this, R.drawable.nc_rounded_light_background)
        }
    }

    private fun updateMnemonic(mnemonic: String) {
        binding.mnemonic.getEditTextView().setText(mnemonic)
        binding.mnemonic.getEditTextView().setSelection(mnemonic.length)
    }

    private fun setupViews() {
        binding.mnemonic.heightExtended(resources.getDimensionPixelSize(R.dimen.nc_height_180))
        binding.mnemonic.addTextChangedListener(object : SimpleTextWatcher() {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                viewModel.handleInputEvent("$s")
            }
        })
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