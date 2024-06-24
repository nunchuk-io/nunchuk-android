package com.nunchuk.android.main.membership.custom

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.nunchuk.android.core.sheet.BottomSheetOption
import com.nunchuk.android.core.sheet.BottomSheetOptionListener
import com.nunchuk.android.core.sheet.SheetOption
import com.nunchuk.android.core.sheet.SheetOptionType
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.isAirgapTag
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.MembershipActivity
import com.nunchuk.android.main.membership.component.SignerCard
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.share.ColdcardAction
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.share.result.GlobalResult
import com.nunchuk.android.share.result.GlobalResultKey
import com.nunchuk.android.signer.mk4.Mk4Activity
import com.nunchuk.android.signer.tapsigner.NfcSetupActivity
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.widget.NCInfoDialog
import com.nunchuk.android.widget.NCWarningDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class CustomKeyAccountFragment : MembershipFragment(), BottomSheetOptionListener {
    private val viewModel: CustomKeyAccountFragmentViewModel by viewModels()
    private val args: CustomKeyAccountFragmentArgs by navArgs()

    @Inject
    lateinit var navigator: NunchukNavigator
    
    private val coldcardOrAirgapLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            findNavController().popBackStack()
        } else if (it.resultCode == GlobalResult.RESULT_INDEX_NOT_MATCH) {
            showError(getString(R.string.nc_coldcard_index_not_match, viewModel.getNewIndex()))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val remainingTime by membershipStepManager.remainingTime.collectAsStateWithLifecycle()
                CustomKeyAccountFragmentScreen(
                    viewModel = viewModel,
                    signer = args.signer,
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
                    when (event) {
                        is CustomKeyAccountFragmentEvent.CheckSigner -> handleCheckSigner(event.signer)
                        is CustomKeyAccountFragmentEvent.OpenScanTapSigner -> openCreateBackUpTapSigner(
                            event.index
                        )
                    }
                }
        }
    }

    override fun onOptionClicked(option: SheetOption) {
        super.onOptionClicked(option)
        when (option.type) {
            SheetOptionType.TYPE_ADD_COLDCARD_NFC -> {
                if (args.isFreeWallet) {
                    coldcardOrAirgapLauncher.launch(
                        Mk4Activity.buildIntent(
                            activity = requireActivity(),
                            action = ColdcardAction.CREATE,
                            walletId = (activity as MembershipActivity).walletId,
                            newIndex = viewModel.getNewIndex(),
                            xfp = args.signer.fingerPrint,
                        )
                    )
                } else {
                    navigator.startSetupMk4ForResult(
                        launcher = coldcardOrAirgapLauncher,
                        activity = requireActivity(),
                        fromMembershipFlow = true,
                        action = ColdcardAction.CREATE,
                        groupId = (activity as MembershipActivity).groupId,
                        newIndex = viewModel.getNewIndex(),
                        xfp = args.signer.fingerPrint,
                        replacedXfp = args.replacedXfp,
                        walletId = (activity as MembershipActivity).walletId,
                    )
                }
            }

            SheetOptionType.TYPE_ADD_COLDCARD_FILE -> {
                if (args.isFreeWallet) {
                    coldcardOrAirgapLauncher.launch(
                        Mk4Activity.buildIntent(
                            activity = requireActivity(),
                            action = ColdcardAction.RECOVER_KEY,
                            walletId = (activity as MembershipActivity).walletId,
                            newIndex = viewModel.getNewIndex(),
                            xfp = args.signer.fingerPrint,
                        )
                    )
                } else {
                    navigator.startSetupMk4ForResult(
                        launcher = coldcardOrAirgapLauncher,
                        activity = requireActivity(),
                        fromMembershipFlow = true,
                        action = ColdcardAction.RECOVER_KEY,
                        groupId = (activity as MembershipActivity).groupId,
                        newIndex = viewModel.getNewIndex(),
                        xfp = args.signer.fingerPrint,
                        replacedXfp = args.replacedXfp,
                        walletId = (activity as MembershipActivity).walletId,
                    )
                }
            }
        }
    }

    private fun openCreateBackUpTapSigner(index: Int) {
        findNavController().popBackStack()
        if (args.isFreeWallet) {
            startActivity(
                NfcSetupActivity.buildIntent(
                    activity = requireActivity(),
                    setUpAction = NfcSetupActivity.SETUP_TAP_SIGNER,
                    walletId = (activity as MembershipActivity).walletId,
                    masterSignerId = args.signer.fingerPrint,
                )
            )
        } else {
            navigator.openCreateBackUpTapSigner(
                activity = requireActivity(),
                fromMembershipFlow = true,
                masterSignerId = args.signer.fingerPrint,
                groupId = (activity as MembershipActivity).groupId,
                signerIndex = index,
                replacedXfp = args.replacedXfp,
                walletId = (activity as MembershipActivity).walletId,
            )
        }
    }

    private fun handleCheckSigner(signer: SingleSigner?) {
        if (signer == null) {
            when {
                args.signer.type == SignerType.COLDCARD_NFC || args.signer.tags.contains(SignerTag.COLDCARD) -> {
                    showAddColdcardOptions()
                }

                args.signer.type == SignerType.AIRGAP -> {
                    NCInfoDialog(requireActivity()).showDialog(
                        message = getString(R.string.nc_new_account_needed_airgap),
                        btnInfo = getString(R.string.nc_cancel),
                        btnYes = getString(R.string.nc_text_continue),
                        onYesClick = {
                            args.signer.tags.find { it.isAirgapTag }?.let { tag ->
                                navigator.openAddAirSignerScreenForResult(
                                    launcher = coldcardOrAirgapLauncher,
                                    activityContext = requireActivity(),
                                    isMembershipFlow = true,
                                    tag = tag,
                                    groupId = (activity as MembershipActivity).groupId,
                                    xfp = args.signer.fingerPrint,
                                    newIndex = viewModel.getNewIndex(),
                                    replacedXfp = args.replacedXfp,
                                    walletId = (activity as MembershipActivity).walletId,
                                )
                            }
                        }
                    )
                }

                args.signer.type == SignerType.FOREIGN_SOFTWARE -> {
                    NCInfoDialog(requireActivity()).showDialog(
                        message = getString(R.string.nc_foreign_key_index_not_available_msg),
                        onYesClick = {
                            findNavController().popBackStack()
                        }
                    )
                }

                args.signer.type == SignerType.NFC -> openCreateBackUpTapSigner(viewModel.getNewIndex())

                else -> {
                    NCWarningDialog(requireActivity()).showDialog(
                        message = getString(R.string.nc_master_signer_new_index_not_available),
                        btnNo = getString(R.string.nc_cancel),
                        btnYes = getString(R.string.nc_text_got_it),
                        onYesClick = {
                            findNavController().popBackStack()
                        }
                    )
                }
            }
        } else {
            setFragmentResult(
                REQUEST_KEY,
                Bundle().apply {
                    putParcelable(GlobalResultKey.EXTRA_SIGNER, signer)
                },
            )
            findNavController().popBackStack()
        }
    }

    private fun showAddColdcardOptions() {
        BottomSheetOption.newInstance(
            listOf(
                SheetOption(
                    type = SheetOptionType.TYPE_ADD_COLDCARD_NFC,
                    label = getString(R.string.nc_add_coldcard_via_nfc),
                    resId = R.drawable.ic_nfc_indicator_small
                ),
                SheetOption(
                    type = SheetOptionType.TYPE_ADD_COLDCARD_FILE,
                    label = getString(R.string.nc_add_coldcard_via_file),
                    resId = R.drawable.ic_import
                ),
            )
        ).show(childFragmentManager, "BottomSheetOption")
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
        isTestNet = uiState.isTestNet,
        onShowMoreOptions = onShowMoreOptions,
        onContinueClicked = viewModel::checkSignerIndex
    )
}

@Composable
private fun CustomKeyAccountFragmentContent(
    signer: SignerModel,
    oldIndex: Int = 0,
    isTestNet: Boolean = false,
    remainingTime: Int = 0,
    onShowMoreOptions: () -> Unit = {},
    onContinueClicked: (newIndex: Int) -> Unit = {},
) {
    var newIndex by remember(oldIndex) {
        mutableStateOf(if (oldIndex >= 0) "" else "0")
    }
    val formatOldIndex = if (oldIndex >= 0) oldIndex.toString() else ""
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
                            MaterialTheme.colorScheme.greyLight,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(16.dp)
                        .fillMaxWidth(),
                    text = formatOldIndex,
                    style = NunchukTheme.typography.body,
                )

                Text(
                    modifier = Modifier.padding(top = 4.dp),
                    text = getPath(formatOldIndex, isTestNet),
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
                    text = getPath(newIndex, isTestNet),
                    style = NunchukTheme.typography.bodySmall,
                )
            }
        }
    }
}

private fun getPath(index: String, isTestNet: Boolean): String {
    if (index.isEmpty()) return ""
    return if (isTestNet) "BIP32 path: m/48h/1h/${index}h/2h" else "BIP32 path: m/48h/0h/${index}h/2h"
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