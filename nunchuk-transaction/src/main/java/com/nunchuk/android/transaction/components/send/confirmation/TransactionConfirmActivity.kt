package com.nunchuk.android.transaction.components.send.confirmation

import android.app.Activity
import android.nfc.tech.IsoDep
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import com.nunchuk.android.core.manager.ActivityManager
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.core.nfc.BaseNfcActivity
import com.nunchuk.android.core.nfc.SweepType
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.core.util.getUSDAmount
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.SatsCardSlot
import com.nunchuk.android.transaction.R
import com.nunchuk.android.transaction.components.send.amount.InputAmountActivity
import com.nunchuk.android.transaction.components.send.confirmation.TransactionConfirmEvent.*
import com.nunchuk.android.transaction.components.utils.toTitle
import com.nunchuk.android.transaction.databinding.ActivityTransactionConfirmBinding
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.filter

@AndroidEntryPoint
class TransactionConfirmActivity : BaseNfcActivity<ActivityTransactionConfirmBinding>() {

    @Inject
    lateinit var sessionHolder: SessionHolder

    private val args: TransactionConfirmArgs by lazy { TransactionConfirmArgs.deserializeFrom(intent) }

    private val viewModel: TransactionConfirmViewModel by viewModels()

    override fun initializeBinding() = ActivityTransactionConfirmBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()
        setupViews()
        observeEvent()
        viewModel.init(
            walletId = args.walletId,
            address = args.address,
            sendAmount = args.outputAmount,
            estimateFee = args.estimatedFee,
            subtractFeeFromAmount = args.subtractFeeFromAmount,
            privateNote = args.privateNote,
            manualFeeRate = args.manualFeeRate,
            slots = args.slots
        )
    }

    private fun observeEvent() {
        viewModel.event.observe(this, ::handleEvent)
        flowObserver(nfcViewModel.nfcScanInfo.filter { it.requestCode == REQUEST_SATSCARD_SWEEP_SLOT }) {
            viewModel.handleSweepBalance(IsoDep.get(it.tag), nfcViewModel.inputCvc.orEmpty(), args.slots.toList(), args.sweepType)
            nfcViewModel.clearScanInfo()
        }
    }

    private fun setupViews() {
        binding.toolbarTitle.text = args.sweepType.toTitle(this)
        binding.btnConfirm.text = when (args.sweepType) {
            SweepType.NONE -> getString(R.string.nc_transaction_confirm_and_create_transaction)
            else -> getString(R.string.nc_confirm_and_sweep)
        }
        binding.sendAddressLabel.text = args.address
        binding.estimatedFeeBTC.text = args.estimatedFee.getBTCAmount()
        binding.estimatedFeeUSD.text = args.estimatedFee.getUSDAmount()
        val sendAmount: Double
        val totalAmount: Double
        if (args.subtractFeeFromAmount) {
            sendAmount = args.outputAmount - args.estimatedFee
            totalAmount = args.outputAmount
        } else {
            sendAmount = args.outputAmount
            totalAmount = args.outputAmount + args.estimatedFee
        }
        binding.sendAddressBTC.text = sendAmount.getBTCAmount()
        binding.sendAddressUSD.text = sendAmount.getUSDAmount()
        binding.totalAmountBTC.text = totalAmount.getBTCAmount()
        binding.totalAmountUSD.text = totalAmount.getUSDAmount()
        binding.noteContent.text = args.privateNote

        binding.btnConfirm.setOnClickListener {
            if (args.slots.isNotEmpty()) {
                startNfcFlow(REQUEST_SATSCARD_SWEEP_SLOT)
            } else {
                viewModel.handleConfirmEvent()
            }
        }

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun handleEvent(event: TransactionConfirmEvent) {
        when (event) {
            is CreateTxErrorEvent -> showCreateTransactionError(event.message)
            is CreateTxSuccessEvent -> openTransactionDetailScreen(event.txId)
            is UpdateChangeAddress -> bindChangAddress(event.address, event.amount)
            LoadingEvent -> showLoading()
            is InitRoomTransactionError -> showCreateTransactionError(event.message)
            is InitRoomTransactionSuccess -> returnActiveRoom(event.roomId)
            is Error -> {
                if (nfcViewModel.handleNfcError(event.e).not()) {
                    NCToastMessage(this).showError(event.e?.message.orUnknownError())
                }
            }
            is NfcLoading -> showOrHideLoading(event.isLoading, getString(R.string.nc_keep_holding_nfc))
            is SweepSuccess -> {
                ActivityManager.popUntilRoot()
                if (args.walletId.isNotEmpty()) {
                    navigator.openWalletDetailsScreen(this, args.walletId, true)
                } else {
                    navigator.openTransactionDetailsScreen(this, "", event.transaction.txId, "", "", event.transaction)
                }
            }
            is SweepLoadingEvent -> showOrHideLoading(
                event.isLoading,
                title = getString(R.string.nc_sweeping_is_progress),
                message = getString(R.string.nc_make_sure_internet)
            )
        }
    }

    private fun returnActiveRoom(roomId: String) {
        hideLoading()
        finish()
        ActivityManager.popUntil(InputAmountActivity::class.java, true)
        navigator.openRoomDetailActivity(this, roomId)
    }

    private fun bindChangAddress(changeAddress: String, amount: Amount) {
        hideLoading()

        if (changeAddress.isNotBlank()) {
            binding.changeAddressLabel.text = changeAddress
            binding.changeAddressBTC.text = amount.getBTCAmount()
            binding.changeAddressUSD.text = amount.getUSDAmount()
        } else {
            binding.changeAddress.visibility = View.GONE
            binding.changeAddressLabel.visibility = View.GONE
            binding.changeAddressBTC.visibility = View.GONE
            binding.changeAddressUSD.visibility = View.GONE
        }
    }

    private fun openTransactionDetailScreen(txId: String) {
        hideLoading()
        ActivityManager.popUntil(InputAmountActivity::class.java, true)
        navigator.openTransactionDetailsScreen(
            activityContext = this,
            walletId = args.walletId,
            txId = txId,
            initEventId = "",
            roomId = sessionHolder.getActiveRoomIdSafe()
        )
        NCToastMessage(this).showMessage("Transaction created::$txId")
    }

    private fun showCreateTransactionError(message: String) {
        hideLoading()
        NCToastMessage(this).showError("Create transaction error due to $message")
    }

    companion object {

        fun start(
            activityContext: Activity,
            walletId: String,
            outputAmount: Double,
            availableAmount: Double,
            address: String,
            privateNote: String,
            estimatedFee: Double,
            subtractFeeFromAmount: Boolean = false,
            manualFeeRate: Int = 0,
            sweepType: SweepType = SweepType.NONE,
            slots: List<SatsCardSlot> = emptyList()
        ) {
            activityContext.startActivity(
                TransactionConfirmArgs(
                    walletId = walletId,
                    outputAmount = outputAmount,
                    availableAmount = availableAmount,
                    address = address,
                    privateNote = privateNote,
                    estimatedFee = estimatedFee,
                    subtractFeeFromAmount = subtractFeeFromAmount,
                    manualFeeRate = manualFeeRate,
                    sweepType = sweepType,
                    slots = slots
                ).buildIntent(activityContext)
            )
        }

    }

}