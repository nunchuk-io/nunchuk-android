package com.nunchuk.android.main.membership.key.desktop

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.key.toString
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.widget.NCInfoDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class WaitingDesktopKeyFragment : MembershipFragment() {
    private val args: WaitingDesktopKeyFragmentArgs by navArgs()
    private val viewModel: WaitingDesktopKeyViewModel by viewModels()

    private var noDeviceDialog: Dialog? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val remainTime by membershipStepManager.remainingTime.collectAsStateWithLifecycle()
                WaitingDesktopKeyContent(
                    isMembershipFlow = args.magic.isEmpty(),
                    title = stringResource(
                        R.string.nc_waiting_for_desktop_key_to_be_added,
                        args.signerTag.toString(requireContext())
                    ),
                    desc = stringResource(
                        R.string.nc_add_key_using_desktop_desc,
                        args.signerTag.toString(requireContext())
                    ),
                    button = stringResource(R.string.nc_text_continue),
                    remainTime = remainTime,
                    onMoreClicked = ::handleShowMore,
                    onConfirmAddLedger = {
                        viewModel.checkRequestStatus()
                    }
                )
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
                        if (state.isDuplicateKey) {
                            showError(getString(R.string.nc_error_add_same_key))
                        } else if (isCompleted) {
                            noDeviceDialog?.dismiss()
                            findNavController().navigate(
                                WaitingDesktopKeyFragmentDirections.actionWaitingDesktopKeyFragmentToRequestAddKeySuccessFragment(
                                    signerTag = args.signerTag,
                                    magic = args.magic
                                )
                            )
                        } else {
                            noDeviceDialog = NCInfoDialog(requireActivity()).showDialog(
                                message = getString(
                                    R.string.nc_no_device_have_been_detected,
                                    args.signerTag.toString(requireContext())
                                ),
                                onYesClick = {
                                    if (state.requestCancel) {
                                        findNavController().popBackStack(
                                            R.id.addKeyListFragment,
                                            false
                                        )
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