package com.nunchuk.android.wallet.assign

import android.content.Context
import android.content.Intent
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
            is AssignSignerCompletedEvent -> openWalletConfirmScreen(event.requiredSignersNumber, event.signers)
        }
    }

    private fun openWalletConfirmScreen(requiredSignersNumber: Int, signers: List<SingleSigner>) {

    }

    private fun handleState(state: AssignSignerState) {
        bindSigners(state.signers, state.selectedPFXs)
        bindRequiredSignersInput(state.requiredSignersNumber)
        val requiredSignersCounterValue = "${state.requiredSignersNumber}/${state.selectedPFXs.size} ${getString(R.string.nc_wallet_multisig)}"
        binding.requiredSignersCounter.text = requiredSignersCounterValue
    }

    private fun bindRequiredSignersInput(requiredSignersNumber: Int) {
        binding.requiredSingerInput.text?.apply {
            clear()
            append("$requiredSignersNumber")
        }
        binding.requiredSingerInput.isEnabled = requiredSignersNumber == 0
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
                viewModel.updateRequiredSingerInput("$s")
            }
        })
    }

    companion object {

        fun start(activityContext: Context, walletName: String, walletType: WalletType, addressType: AddressType) {
            AssignSignerArgs(walletName, walletType, addressType)
            activityContext.startActivity(Intent(activityContext, AssignSignerActivity::class.java))
        }
    }

}