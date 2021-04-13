package com.nunchuk.android.wallet.assign

import android.content.Context
import android.os.Bundle
import androidx.lifecycle.ViewModelProviders
import com.nunchuk.android.arch.BaseActivity
import com.nunchuk.android.arch.vm.NunchukFactory
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.wallet.R
import com.nunchuk.android.wallet.assign.AssignSignerEvent.AssignSignerCompletedEvent
import com.nunchuk.android.wallet.databinding.ActivityWalletAssignSignerBinding
import com.nunchuk.android.widget.util.SimpleTextWatcher
import javax.inject.Inject

class AssignSignerActivity : BaseActivity() {

    @Inject
    lateinit var factory: NunchukFactory

    @Inject
    lateinit var navigator: NunchukNavigator

    private val args: AssignSignerArgs by lazy { AssignSignerArgs.deserializeFrom(intent) }

    private val viewModel: AssignSignerViewModel by lazy {
        ViewModelProviders.of(this, factory).get(AssignSignerViewModel::class.java)
    }

    private lateinit var binding: ActivityWalletAssignSignerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityWalletAssignSignerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        observeEvent()

        viewModel.init()
    }

    private fun observeEvent() {
        viewModel.event.observe(this, ::handleEvent)
        viewModel.state.observe(this, ::handleState)
    }

    private fun handleEvent(event: AssignSignerEvent) {
        when (event) {
            is AssignSignerCompletedEvent -> openWalletConfirmScreen(event.totalRequireSigns, event.selectedSigners)
        }
    }

    private fun openWalletConfirmScreen(totalRequireSigns: Int, signers: List<SingleSigner>) {
        navigator.openWalletConfirmScreen(
            activityContext = this,
            walletName = args.walletName,
            walletType = args.walletType,
            addressType = args.addressType,
            totalRequireSigns = totalRequireSigns,
            signers = signers
        )
    }

    private fun handleState(state: AssignSignerState) {
        bindSigners(state.signers, state.selectedPFXs)
        bindTotalRequireSigns(state.totalRequireSigns)
        val totalRequireSignsValue = "${state.totalRequireSigns}/${state.selectedPFXs.size} ${getString(R.string.nc_wallet_multisig)}"
        binding.totalRequireSigns.text = totalRequireSignsValue
    }

    private fun bindTotalRequireSigns(totalRequireSigns: Int) {
        binding.requiredSingerInput.text?.apply {
            clear()
            append("$totalRequireSigns")
        }
        binding.requiredSingerInput.isEnabled = totalRequireSigns == 0
    }

    private fun bindSigners(signers: List<SingleSigner>, selectedPFXs: List<String>) {
        SignersViewBinder(binding.signersContainer, signers, selectedPFXs, viewModel::updateSelectedXfps).bindItems()
    }

    private fun setupViews() {
        binding.signersContainer.removeAllViews()
        binding.iconPlus.setOnClickListener { viewModel.handleIncreaseRequiredSigners() }
        binding.iconMinus.setOnClickListener { viewModel.handleDecreaseRequiredSigners() }
        binding.requiredSingerInput.addTextChangedListener(object : SimpleTextWatcher() {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                viewModel.updateTotalRequireSigns("$s")
            }
        })
        binding.btnContinue.setOnClickListener { viewModel.handleContinueEvent() }
    }

    companion object {

        fun start(activityContext: Context, walletName: String, walletType: WalletType, addressType: AddressType) {
            activityContext.startActivity(AssignSignerArgs(walletName, walletType, addressType).buildIntent(activityContext))
        }
    }

}