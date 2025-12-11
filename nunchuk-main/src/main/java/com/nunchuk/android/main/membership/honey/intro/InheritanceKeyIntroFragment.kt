/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2022, 2023 Nunchuk                                       *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

package com.nunchuk.android.main.membership.honey.intro

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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.clearFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.HighlightMessageType
import com.nunchuk.android.compose.NcHintMessage
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.sheet.BottomSheetOption
import com.nunchuk.android.core.sheet.BottomSheetOptionListener
import com.nunchuk.android.core.sheet.SheetOption
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.ClickAbleText
import com.nunchuk.android.core.util.InheritancePlanType
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.MembershipActivity
import com.nunchuk.android.main.membership.key.list.TapSignerListBottomSheetFragment
import com.nunchuk.android.main.membership.key.list.TapSignerListBottomSheetFragmentArgs
import com.nunchuk.android.nav.args.SetupMk4Args
import com.nunchuk.android.share.ColdcardAction
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.type.SignerType
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class InheritanceKeyIntroFragment : MembershipFragment(), BottomSheetOptionListener {
    private val viewModel: TapSignerInheritanceIntroViewModel by viewModels()
    private val args: InheritanceKeyIntroFragmentArgs by navArgs<InheritanceKeyIntroFragmentArgs>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                TapSignerInheritanceIntroScreen(
                    inheritanceType = args.inheritanceType,
                    viewModel = viewModel,
                    onMoreClicked = ::handleShowMore
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
                        TapSignerInheritanceIntroEvent.OnContinueClicked -> handleAddKey()
                    }
                }
        }
        setFragmentResultListener(TapSignerListBottomSheetFragment.REQUEST_KEY) { _, bundle ->
            val data = TapSignerListBottomSheetFragmentArgs.fromBundle(bundle)
            if (data.signers.isNotEmpty()) {
                when (data.signers.first().type) {
                    SignerType.NFC -> openCreateBackUpTapSigner(data.signers.first().id)
                    SignerType.COLDCARD_NFC, SignerType.AIRGAP -> openCreateBackUpColdCard(data.signers.first())
                    else -> throw IllegalArgumentException("Signer type invalid ${data.signers.first().type}")
                }
                findNavController().popBackStack()
            } else {
                openSelectHardwareOption()
            }
            clearFragmentResult(TapSignerListBottomSheetFragment.REQUEST_KEY)
        }
    }

    private fun openCreateBackUpTapSigner(masterSignerId: String) {
        if (membershipStepManager.isKeyExisted(masterSignerId).not()) {
            navigator.openCreateBackUpTapSigner(
                activity = requireActivity(),
                fromMembershipFlow = true,
                masterSignerId = masterSignerId,
                groupId = (activity as MembershipActivity).groupId
            )
        } else {
            showSameSignerAdded()
        }
    }

    private fun openCreateBackUpColdCard(signer: SignerModel) {
        if (membershipStepManager.isKeyExisted(signer.fingerPrint).not()) {
            navigator.openSetupMk4(
                activity = requireActivity(),
                args = SetupMk4Args(
                    fromMembershipFlow = true,
                    xfp = signer.fingerPrint,
                    groupId = (activity as MembershipActivity).groupId,
                    action = ColdcardAction.INHERITANCE_PASSPHRASE_QUESTION,
                    walletId = (activity as MembershipActivity).walletId,
                    keyName = signer.name,
                    signerType = signer.type
                )
            )
        } else {
            showSameSignerAdded()
        }
    }

    private fun showSameSignerAdded() {
        showError(getString(R.string.nc_error_add_same_key))
    }

    private fun handleAddKey() {
        runCatching {
            if (args.inheritanceType == InheritancePlanType.ON_CHAIN) {
                findNavController().navigate(
                    InheritanceKeyIntroFragmentDirections.actionInheritanceKeyIntroFragmentToImportantNoticePassphraseFragment(
                        onChainAddSignerParam = args.onChainAddSignerParam
                    )
                )
            } else {
                if (viewModel.getSigners().isNotEmpty()) {
                    findNavController().navigate(
                        InheritanceKeyIntroFragmentDirections.actionInheritanceKeyIntroFragmentToTapSignerListBottomSheetFragment(
                            signers = viewModel.getSigners().toTypedArray(),
                            type = SignerType.NFC,
                            description = "We noticed that you already have a TAPSIGNER or COLDCARD in your key manager"
                        )
                    )
                } else {
                    openSelectHardwareOption()
                }
            }
        }
    }

    private fun openSetupTapSigner() {
        navigator.openSetupTapSigner(
            activity = requireActivity(),
            fromMembershipFlow = true,
            groupId = (activity as MembershipActivity).groupId,
        )
    }

    private fun openSelectHardwareOption() {
        val options = listOf(
            SheetOption(
                type = SignerType.NFC.ordinal,
                label = getString(R.string.nc_tapsigner)
            ),
            SheetOption(
                type = SignerType.COLDCARD_NFC.ordinal,
                label = getString(R.string.nc_coldcard)
            )
        )
        BottomSheetOption.newInstance(
            options = options,
            desc = "We support Inheritance Key on COLDCARD and TAPSIGNER.",
            title = getString(R.string.nc_what_type_of_hardware_want_to_add),
        ).show(childFragmentManager, "BottomSheetOption")
    }

    override fun onOptionClicked(option: SheetOption) {
        super.onOptionClicked(option)
        when (option.type) {
            SignerType.NFC.ordinal -> {
                openSetupTapSigner()
                findNavController().popBackStack()
            }


            SignerType.COLDCARD_NFC.ordinal -> {
                navigator.openSetupMk4(
                    activity = requireActivity(),
                    args = SetupMk4Args(
                        fromMembershipFlow = true,
                        action = ColdcardAction.INHERITANCE_PASSPHRASE_QUESTION,
                        groupId = (activity as MembershipActivity).groupId,
                        walletId = (activity as MembershipActivity).walletId
                    )
                )
                findNavController().popBackStack()
            }
        }
    }
}

@Composable
private fun TapSignerInheritanceIntroScreen(
    inheritanceType: InheritancePlanType,
    viewModel: TapSignerInheritanceIntroViewModel = viewModel(),
    onMoreClicked: () -> Unit = {},
) {
    val remainTime by viewModel.remainTime.collectAsStateWithLifecycle()
    TapSignerInheritanceIntroContent(
        onContinueClicked = viewModel::onContinueClicked,
        inheritanceType = inheritanceType,
        onMoreClicked = onMoreClicked,
        remainTime = remainTime
    )
}

@Composable
private fun TapSignerInheritanceIntroContent(
    remainTime: Int = 0,
    inheritanceType: InheritancePlanType = InheritancePlanType.ON_CHAIN,
    onMoreClicked: () -> Unit = {},
    onContinueClicked: () -> Unit = {},
) {
    NunchukTheme {
        Scaffold(topBar = {
            NcImageAppBar(
                backgroundRes = R.drawable.bg_inheritance_key_illustration,
                title = if (remainTime <= 0) "" else stringResource(
                    id = R.string.nc_estimate_remain_time,
                    remainTime
                ),
                actions = {
                    IconButton(onClick = onMoreClicked) {
                        Icon(
                            painter = painterResource(id = com.nunchuk.android.signer.R.drawable.ic_more),
                            contentDescription = "More icon"
                        )
                    }
                }
            )
        }) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    modifier = Modifier.padding(top = 24.dp, start = 16.dp, end = 16.dp),
                    text = stringResource(R.string.nc_your_inheritance_key),
                    style = NunchukTheme.typography.heading
                )
                if (inheritanceType == InheritancePlanType.OFF_CHAIN) {
                    Text(
                        modifier = Modifier.padding(16.dp),
                        text = "This key will serve as your designated inheritance key. Please take a moment to label or mark it accordingly.",
                        style = NunchukTheme.typography.body
                    )
                } else {
                    Text(
                        modifier = Modifier.padding(16.dp),
                        text = buildAnnotatedString {
                            append("The inheritance key is used to claim funds and spend from the wallet after the timelock.\n\n")
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append("If you do not already have a BIP39 seed phrase, view it on your device or generate a new one. ")
                            }
                            append("Either a 12-word or 24-word seed phrase is acceptable (12 words are sufficient). Keep it secret.\n\n")
                            append("You will later share this seed phrase backup with your Beneficiary so they can access the inheritance.")
                        },
                        style = NunchukTheme.typography.body
                    )
                }
                Spacer(modifier = Modifier.weight(1.0f))

                if (inheritanceType == InheritancePlanType.ON_CHAIN) {
                    NcHintMessage(
                        messages = listOf(
                            ClickAbleText(
                                content = "Your device may use a different term for the seed phrase, such as \"recovery phrase\" or \"mnemonic.\""
                            )
                        ),
                        modifier = Modifier.padding(horizontal = 16.dp),
                        type = HighlightMessageType.HINT,
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
private fun TapSignerInheritanceIntroScreenPreview() {
    TapSignerInheritanceIntroContent()
}