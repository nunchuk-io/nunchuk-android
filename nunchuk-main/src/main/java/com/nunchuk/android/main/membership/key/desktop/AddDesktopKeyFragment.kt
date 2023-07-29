package com.nunchuk.android.main.membership.key.desktop

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
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
import com.nunchuk.android.type.SignerTag
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AddDesktopKeyFragment : MembershipFragment() {
    private val viewModel: AddDesktopKeyViewModel by viewModels()
    private val args: AddDesktopKeyFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val remainTime by membershipStepManager.remainingTime.collectAsStateWithLifecycle()
                AddLedgerScreen(
                    tag = args.signerTag,
                    remainTime = remainTime,
                    onMoreClicked = ::handleShowMore,
                    onContinueClicked = {
                        viewModel.requestAddDesktopKey()
                    },
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.event.flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect { event ->
                    when (event) {
                        is AddDesktopKeyEvent.RequestAddKeyFailed -> showError(event.message)
                        is AddDesktopKeyEvent.RequestAddKeySuccess -> findNavController().navigate(
                            AddDesktopKeyFragmentDirections.actionAddDesktopKeyFragmentToWaitingDesktopKeyFragment(
                                args.signerTag,
                                event.requestId
                            )
                        )
                    }
                }
        }
    }
}

@Composable
private fun AddLedgerScreen(
    tag: SignerTag,
    onContinueClicked: () -> Unit = {},
    onMoreClicked: () -> Unit = {},
    remainTime: Int = 0,
) {
    val desc = when(tag) {
        SignerTag.COLDCARD -> stringResource(id = R.string.nc_main_add_coldcard_desc)
        SignerTag.TREZOR -> stringResource(id = R.string.nc_main_add_trezor_desc)
        SignerTag.LEDGER -> stringResource(id = R.string.nc_main_add_ledger_desc)
        SignerTag.BITBOX -> stringResource(id = R.string.nc_main_add_bitbox_desc)
        else -> ""
    }
    val imageId = when(tag) {
        SignerTag.COLDCARD -> R.drawable.bg_coldcard_desktop
        SignerTag.TREZOR -> R.drawable.bg_add_trezor
        SignerTag.LEDGER -> R.drawable.bg_add_ledger
        SignerTag.BITBOX -> R.drawable.bg_add_bitbox
        else -> 0
    }

    AddDesktopKeyContent(
        title = stringResource(
            id = R.string.nc_add_desktop_key,
            tag.toString(LocalContext.current)
        ),
        desc = desc,
        button = stringResource(id = R.string.nc_text_continue),
        onContinueClicked = onContinueClicked,
        onMoreClicked = onMoreClicked,
        remainTime = remainTime,
        backgroundId = imageId
    )
}