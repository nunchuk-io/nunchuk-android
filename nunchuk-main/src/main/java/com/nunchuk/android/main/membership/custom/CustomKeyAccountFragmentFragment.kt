package com.nunchuk.android.main.membership.custom

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTextField
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.greyLight
import com.nunchuk.android.compose.provider.SignerModelProvider
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.component.SignerCard
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.share.result.GlobalResultKey
import com.nunchuk.android.widget.NCWarningDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CustomKeyAccountFragmentFragment : MembershipFragment() {
    private val viewModel: CustomKeyAccountFragmentViewModel by viewModels()
    private val args: CustomKeyAccountFragmentFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val remainingTime by membershipStepManager.remainingTime.collectAsStateWithLifecycle()
                CustomKeyAccountFragmentScreen(
                    viewModel,
                    args.signer,
                    onShowMoreOptions = ::handleShowMore,
                    remainingTime = remainingTime,
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.event.flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect { event ->
                    when(event) {
                        is CustomKeyAccountFragmentEvent.CheckSigner -> handleCheckSigner(event.signer)
                    }
                }
        }
    }

    private fun handleCheckSigner(signer: SingleSigner?) {
        if (signer == null) {
            NCWarningDialog(requireActivity()).showDialog(
                message = getString(R.string.nc_master_signer_new_index_not_available),
                btnNo = getString(R.string.nc_cancel),
                btnYes = getString(R.string.nc_text_got_it),
                onYesClick = {
                    findNavController().popBackStack()
                }
            )
        } else {
            setFragmentResult(
                REQUEST_KEY,
                Bundle().apply {
                    putParcelable(GlobalResultKey.EXTRA_SIGNER, signer)
                }
            )
            findNavController().popBackStack()
        }
    }

    companion object {
        const val REQUEST_KEY = "CustomKeyAccountFragmentFragment"
    }
}

@Composable
private fun CustomKeyAccountFragmentScreen(
    viewModel: CustomKeyAccountFragmentViewModel = viewModel(),
    signer: SignerModel,
    onShowMoreOptions: () -> Unit = {},
    remainingTime: Int = 0,
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    CustomKeyAccountFragmentContent(
        signer = signer,
        oldIndex = uiState.currentIndex,
        remainingTime = remainingTime,
        onShowMoreOptions = onShowMoreOptions,
        onContinueClicked = viewModel::checkSignerIndex
    )
}

@Composable
private fun CustomKeyAccountFragmentContent(
    signer: SignerModel,
    oldIndex: Int = 0,
    remainingTime: Int = 0,
    onShowMoreOptions: () -> Unit = {},
    onContinueClicked: (newIndex: Int) -> Unit = {},
) {
    var newIndex by remember(oldIndex) {
        mutableStateOf("$oldIndex")
    }
    NunchukTheme {
        Scaffold(
            modifier = Modifier.systemBarsPadding(),
            topBar = {
            NcTopAppBar(
                title = stringResource(
                    id = R.string.nc_estimate_remain_time,
                    remainingTime
                ),
                actions = {
                    IconButton(onClick = onShowMoreOptions) {
                        Icon(
                            painter = painterResource(id = com.nunchuk.android.signer.R.drawable.ic_more),
                            contentDescription = "More icon"
                        )
                    }
                }
            )
        }, bottomBar = {
            NcPrimaryDarkButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                onClick = { onContinueClicked(newIndex.toInt()) },
                enabled = newIndex.isNotEmpty()
            ) {
                Text(text = stringResource(id = R.string.nc_text_continue))
            }
        }) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .fillMaxSize(),
            ) {
                Text(
                    text = stringResource(R.string.nc_customize_key_account),
                    style = NunchukTheme.typography.heading,
                )
                SignerCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    signer = signer,
                    isSelectable = false,
                )
                Text(
                    text = stringResource(R.string.nc_custom_key_account_desc),
                    style = NunchukTheme.typography.body,
                )

                Text(
                    modifier = Modifier.padding(top = 24.dp),
                    text = stringResource(R.string.nc_last_used_account),
                    style = NunchukTheme.typography.titleSmall,
                )

                Text(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .background(
                            MaterialTheme.colors.greyLight,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(16.dp)
                        .fillMaxWidth(),
                    text = oldIndex.toString(),
                    style = NunchukTheme.typography.body,
                )

                Text(
                    modifier = Modifier.padding(top = 4.dp),
                    text = "BIP32 path: m/48h/0h/${oldIndex}h/2h",
                    style = NunchukTheme.typography.bodySmall,
                )

                NcTextField(
                    modifier = Modifier
                        .padding(top = 24.dp),
                    title = stringResource(R.string.nc_new_account),
                    value = newIndex,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    onValueChange = {
                        if (it.isEmpty() || it.last().isDigit()) {
                            newIndex = it.take(8)
                        }
                    }
                )

                Text(
                    modifier = Modifier.padding(top = 4.dp),
                    text = "BIP32 path: m/48h/0h/${newIndex}h/2h",
                    style = NunchukTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Preview
@Composable
private fun CustomKeyAccountFragmentScreenPreview(
    @PreviewParameter(SignerModelProvider::class) signer: SignerModel,
) {
    CustomKeyAccountFragmentContent(
        signer = signer
    )
}