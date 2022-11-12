package com.nunchuk.android.main.membership.honey.intro

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.*
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.compose.NcHintMessage
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.ClickAbleText
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.key.AddKeyListFragmentDirections
import com.nunchuk.android.main.membership.key.AddKeyListViewModel
import com.nunchuk.android.main.membership.key.list.TapSignerListBottomSheetFragment
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.utils.parcelable
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TapSignerInheritanceIntroFragment : Fragment() {
    private val viewModel: TapSignerInheritanceIntroViewModel by viewModels()
    private val addKeyViewModel: AddKeyListViewModel by activityViewModels()

    @Inject
    lateinit var navigator: NunchukNavigator

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                TapSignerInheritanceIntroScreen(viewModel)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.event.flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect { event ->
                    when (event) {
                        TapSignerInheritanceIntroEvent.OnContinueClicked -> handleAddTapSigner()
                    }
                }
        }
    }

    private fun openCreateBackUpTapSigner(masterSignerId: String) {
        if (addKeyViewModel.isSignerExist(masterSignerId).not()) {
            navigator.openCreateBackUpTapSigner(
                activity = requireActivity(),
                fromMembershipFlow = true,
                masterSignerId = masterSignerId
            )
        } else {
            showSameSignerAdded()
        }
    }

    private fun showSameSignerAdded() {
        showError(getString(R.string.nc_error_add_same_tap_signer))
    }

    private fun handleAddTapSigner() {
        if (addKeyViewModel.getTapSigners().isNotEmpty()) {
            clearFragmentResultListener(TapSignerListBottomSheetFragment.REQUEST_KEY)
            setFragmentResultListener(TapSignerListBottomSheetFragment.REQUEST_KEY) { _, bundle ->
                bundle.parcelable<SignerModel>(TapSignerListBottomSheetFragment.EXTRA_SELECTED_SIGNER_ID)
                    ?.let {
                        openCreateBackUpTapSigner(it.id)
                    } ?: run {
                    openSetupTapSigner()
                }
            }
            findNavController().navigate(
                TapSignerInheritanceIntroFragmentDirections.actionTapSignerInheritanceIntroFragmentToTapSignerListBottomSheetFragment(
                    addKeyViewModel.getTapSigners().toTypedArray()
                )
            )
        } else {
            openSetupTapSigner()
        }
    }

    private fun openSetupTapSigner() {
        navigator.openSetupTapSigner(
            activity = requireActivity(),
            fromMembershipFlow = true
        )
        findNavController().popBackStack()
    }
}

@OptIn(ExperimentalLifecycleComposeApi::class)
@Composable
private fun TapSignerInheritanceIntroScreen(
    viewModel: TapSignerInheritanceIntroViewModel = viewModel(),
) {
    val remainTime by viewModel.remainTime.collectAsStateWithLifecycle()
    TapSignerInheritanceIntroContent(
        onContinueClicked = viewModel::onContinueClicked,
        remainTime = remainTime
    )
}

@Composable
private fun TapSignerInheritanceIntroContent(
    remainTime: Int = 0,
    onContinueClicked: () -> Unit = {}
) {
    NunchukTheme {
        Scaffold { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .navigationBarsPadding()
            ) {
                NcImageAppBar(
                    backgroundRes = R.drawable.bg_inheritancekey,
                    title = stringResource(
                        id = R.string.nc_estimate_remain_time,
                        remainTime
                    ),
                )
                Text(
                    modifier = Modifier.padding(top = 24.dp, start = 16.dp, end = 16.dp),
                    text = stringResource(R.string.nc_your_inheritance_key),
                    style = NunchukTheme.typography.heading
                )
                Text(
                    modifier = Modifier.padding(16.dp),
                    text = stringResource(R.string.nc_inheritance_intro_desc),
                    style = NunchukTheme.typography.body
                )
                Spacer(modifier = Modifier.weight(1.0f))
                NcHintMessage(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    messages = listOf(ClickAbleText(stringResource(R.string.nc_inheritance_intro_hint)))
                )
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
private fun TapSignerInheritanceIntroScreenPreview() {
    TapSignerInheritanceIntroContent(

    )
}