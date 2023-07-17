package com.nunchuk.android.main.membership.key.desktop

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.main.R
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.widget.NCInfoDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class WaitingDesktopKeyFragment : MembershipFragment() {
    private val args: WaitingDesktopKeyFragmentArgs by navArgs()
    private val viewModel: WaitingDesktopKeyViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val remainTime by membershipStepManager.remainingTime.collectAsStateWithLifecycle()
                val onConfirmAddLedger: () -> Unit = {
                    viewModel.checkRequestStatus()
                }
                when (args.signerTag) {
                    SignerTag.LEDGER -> {
                        WaitingDesktopKeyContent(
                            title = context.getString(R.string.nc_waiting_for_ledger_to_be_added),
                            desc = context.getString(R.string.nc_add_ledger_using_desktop_desc),
                            button = context.getString(R.string.nc_i_have_already_added_ledger),
                            remainTime = remainTime,
                            onMoreClicked = ::handleShowMore,
                            onConfirmAddLedger = onConfirmAddLedger
                        )
                    }
                    SignerTag.TREZOR -> {
                        WaitingDesktopKeyContent(
                            title = context.getString(R.string.nc_waiting_for_trezor_to_be_added),
                            desc = context.getString(R.string.nc_add_trezor_using_desktop_desc),
                            button = context.getString(R.string.nc_i_have_already_added_trezor),
                            remainTime = remainTime,
                            onMoreClicked = ::handleShowMore,
                            onConfirmAddLedger = onConfirmAddLedger
                        )
                    }
                    SignerTag.COLDCARD -> {
                        WaitingDesktopKeyContent(
                            title = context.getString(R.string.nc_waiting_for_coldcard_to_be_added),
                            desc = context.getString(R.string.nc_add_coldcard_using_desktop_desc),
                            button = context.getString(R.string.nc_i_have_already_added_coldcard),
                            remainTime = remainTime,
                            onMoreClicked = ::handleShowMore,
                            onConfirmAddLedger = onConfirmAddLedger
                        )
                    }
                    else -> Unit
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect { state ->
                    val isCompleted = state.isCompleted
                    if (isCompleted != null) {
                        if (isCompleted) {
                            findNavController().navigate(
                                WaitingDesktopKeyFragmentDirections.actionWaitingDesktopKeyFragmentToRequestAddKeySuccessFragment(args.signerTag)
                            )
                        } else {
                            NCInfoDialog(requireActivity()).showDialog(
                                message = getString(
                                    R.string.nc_no_device_have_been_detected,
                                    if (args.signerTag == SignerTag.LEDGER) getString(R.string.nc_ledger) else getString(
                                        R.string.nc_trezor
                                    )
                                ),
                                onYesClick = {
                                    if (state.requestCancel) {
                                        findNavController().popBackStack(R.id.addKeyListFragment, false)
                                    }
                                }
                            )
                        }
                        viewModel.markHandleAddKeyResult()
                    }
                }
        }
    }
}