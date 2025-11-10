package com.nunchuk.android.main.membership.onchaintimelock.changetimelock

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.compose.NCLabelWithIndex
import com.nunchuk.android.compose.NcHintMessage
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.util.ClickAbleText
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.MembershipActivity
import com.nunchuk.android.share.membership.MembershipFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChangeTimelockFragment : MembershipFragment() {

    private val viewModel: ChangeTimelockViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                val uiState by viewModel.state.collectAsStateWithLifecycle()
                ChangeTimelockScreen(
                    changeTimelockFlow = uiState.changeTimelockFlow,
                    onContinueClicked = {
                        viewModel.onContinueClicked()
                    },
                    onReadMoreClicked = {

                    }
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        flowObserver(viewModel.event) { event ->
            when (event) {
                is ChangeTimelockEvent.ChangeTimelockSuccess -> {
                    val replaceWalletId = event.draftWallet.replaceWallet?.localId.orEmpty()
                    (requireActivity() as? MembershipActivity)?.setOnChainReplaceWalletId(replaceWalletId)
                    findNavController().navigate(
                        ChangeTimelockFragmentDirections.actionChangeTimeLockFragmentToIntroAssistedWalletFragment()
                    )
                }
                is ChangeTimelockEvent.ShowError -> {
                    showError(event.message)
                }
            }
        }
    }
}

@Composable
private fun ChangeTimelockScreen(
    changeTimelockFlow: Int = -1,
    onContinueClicked: () -> Unit = {},
    onReadMoreClicked: () -> Unit = {}
) {
    ChangeTimelockContent(
        changeTimelockFlow = changeTimelockFlow,
        onContinueClicked = onContinueClicked,
        onReadMoreClicked = onReadMoreClicked
    )
}

@Composable
private fun ChangeTimelockContent(
    changeTimelockFlow: Int = -1,
    onContinueClicked: () -> Unit = {},
    onReadMoreClicked: () -> Unit = {}
) {
    val titleRes = when (changeTimelockFlow) {
        1 -> R.string.nc_change_to_off_chain_timelock_title
        else -> R.string.nc_change_to_on_chain_timelock_title
    }
    val descriptionRes = when (changeTimelockFlow) {
        1 -> R.string.nc_change_to_off_chain_timelock_desc
        else -> R.string.nc_change_to_on_chain_timelock_desc
    }
    val steps = when (changeTimelockFlow) {
        1 -> listOf(
            R.string.nc_change_to_off_chain_timelock_step_one,
            R.string.nc_change_to_off_chain_timelock_step_two,
            R.string.nc_change_to_off_chain_timelock_step_three,
        )
        else -> listOf(
            R.string.nc_change_to_on_chain_timelock_step_one,
            R.string.nc_change_to_on_chain_timelock_step_two,
            R.string.nc_change_to_on_chain_timelock_step_three,
        )
    }
    val hintMessages = when (changeTimelockFlow) {
        0 -> listOf(
            ClickAbleText(stringResource(R.string.nc_change_to_on_chain_timelock_hint)),
            ClickAbleText(stringResource(R.string.nc_read_more), onClick = onReadMoreClicked),
        )
        else -> emptyList()
    }

    NunchukTheme {
        Scaffold(
            topBar = {
                NcImageAppBar(
                    backgroundRes = R.drawable.bg_timelock_illustrations,
                    title = ""
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    modifier = Modifier.padding(top = 24.dp, start = 16.dp, end = 16.dp),
                    text = stringResource(id = titleRes),
                    style = NunchukTheme.typography.heading
                )

                Text(
                    modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
                    text = stringResource(id = descriptionRes),
                    style = NunchukTheme.typography.body
                )

                steps.forEachIndexed { index, stepRes ->
                    NCLabelWithIndex(
                        modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
                        index = index + 1,
                        label = stringResource(id = stepRes),
                    )
                }

                Spacer(modifier = Modifier.weight(1.0f))

                if (hintMessages.isNotEmpty()) {
                    NcHintMessage(
                        modifier = Modifier
                            .padding(top = 24.dp, start = 16.dp, end = 16.dp)
                            .fillMaxWidth(),
                        messages = hintMessages
                    )
                }

                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    onClick = onContinueClicked,
                ) {
                    Text(text = stringResource(id = com.nunchuk.android.signer.R.string.nc_text_continue))
                }
            }
        }
    }
}

@Preview
@Composable
private fun ChangeTimelockScreenPreview() {
    ChangeTimelockContent()
}