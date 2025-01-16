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

package com.nunchuk.android.main.membership.key.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.provider.SignersModelProvider
import com.nunchuk.android.compose.signer.SignerCard
import com.nunchuk.android.compose.signer.SingleChoiceSignerCard
import com.nunchuk.android.compose.strokePrimary
import com.nunchuk.android.core.base.BaseComposeBottomSheet
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.MembershipViewModel
import com.nunchuk.android.model.signer.SupportedSigner
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.type.SignerType
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@AndroidEntryPoint
class TapSignerListBottomSheetFragment : BaseComposeBottomSheet() {
    @Inject
    lateinit var navigator: NunchukNavigator

    private val activityViewModel by activityViewModels<MembershipViewModel>()
    private val args: TapSignerListBottomSheetFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                val supportedSigners by activityViewModel.state.map { it.supportedTypes }
                    .collectAsStateWithLifecycle(emptyList())

                NunchukTheme {
                    TapSignerListScreen(
                        args = args,
                        onCloseClicked = ::dismissAllowingStateLoss,
                        supportedSigners = supportedSigners,
                        onAddExistKey = { signer ->
                            findNavController().popBackStack()
                            setFragmentResult(
                                REQUEST_KEY,
                                TapSignerListBottomSheetFragmentArgs(
                                    listOf(signer).toTypedArray(),
                                    args.type
                                ).toBundle()
                            )
                        },
                        onAddNewKey = {
                            findNavController().popBackStack()
                            setFragmentResult(
                                REQUEST_KEY, TapSignerListBottomSheetFragmentArgs(
                                    emptyArray(),
                                    args.type
                                ).toBundle()
                            )
                        }
                    )
                }
            }
        }
    }

    companion object {
        const val REQUEST_KEY = "TapSignerListBottomSheetFragment"
    }
}

@Composable
fun TapSignerListScreen(
    viewModel: TapSingerListBottomSheetViewModel = hiltViewModel(),
    args: TapSignerListBottomSheetFragmentArgs,
    onCloseClicked: () -> Unit = {},
    supportedSigners: List<SupportedSigner> = emptyList(),
    onAddExistKey: (SignerModel) -> Unit = {},
    onAddNewKey: () -> Unit = {},
) {
    val selectedSigner by viewModel.selectSingle.collectAsStateWithLifecycle()

    val selectableSigner = if (supportedSigners.isNotEmpty()) args.signers.partition { signer ->
        supportedSigners.any {
            it.type == signer.type && (it.tag == null || signer.tags.contains(it.tag))
        }
    } else {
        Pair(args.signers.toList(), emptyList())
    }

    LaunchedEffect(Unit) {
        viewModel.onSignerSelected(null)
        viewModel.event.collect {
            when (it) {
                is TapSignerListBottomSheetEvent.OnAddExistingKey -> onAddExistKey(it.signer)
                TapSignerListBottomSheetEvent.OnAddNewKey -> onAddNewKey()
            }
        }
    }

    TapSignerListContent(
        onCloseClicked = onCloseClicked,
        onAddExistKeyClicked = viewModel::onAddExistingKey,
        onAddNewKeyClicked = viewModel::onAddNewKey,
        signers = selectableSigner.first,
        unsupportedSigners = selectableSigner.second,
        type = args.type,
        description = args.description,
        onSignerSelected = viewModel::onSignerSelected,
        selectedSigner = selectedSigner,
    )
}

@Composable
private fun TapSignerListContent(
    onCloseClicked: () -> Unit = {},
    onAddExistKeyClicked: () -> Unit = {},
    onAddNewKeyClicked: () -> Unit = {},
    signers: List<SignerModel> = emptyList(),
    unsupportedSigners: List<SignerModel> = emptyList(),
    onSignerSelected: (signer: SignerModel) -> Unit = {},
    selectedSigner: SignerModel? = null,
    type: SignerType = SignerType.NFC,
    description: String = "",
) {
    val signerLabel = when (type) {
        SignerType.NFC -> "TAPSIGNER(s)"
        SignerType.COLDCARD_NFC -> "COLDCARD(s)"
        SignerType.AIRGAP -> "Air-gapped key(s)"
        SignerType.HARDWARE -> "Wired key(s)"
        SignerType.SOFTWARE -> "Software key(s)"
        else -> ""
    }
    val screenHeightDp = LocalConfiguration.current.screenHeightDp
    Column(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
            )
            .nestedScroll(rememberNestedScrollInteropConnection())
    ) {
        IconButton(
            modifier = Modifier.padding(top = 40.dp), onClick = onCloseClicked
        ) {
            NcIcon(
                painter = painterResource(id = R.drawable.ic_close),
                contentDescription = "Back Icon"
            )
        }
        Text(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp),
            text = stringResource(
                R.string.nc_do_you_want_add_existing_key
            ),
            style = NunchukTheme.typography.title,
        )
        if (signerLabel.isNotEmpty() || description.isNotEmpty()) {
            Text(
                modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp),
                text = description.ifEmpty {
                    stringResource(
                        R.string.nc_notice_you_have_exist_key,
                        signerLabel
                    )
                },
                style = NunchukTheme.typography.body,
            )
        }
        LazyColumn(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .heightIn(max = screenHeightDp.div(2).dp),
            contentPadding = PaddingValues(vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            items(signers) { signer ->
                SingleChoiceSignerCard(
                    signer = signer,
                    isChecked = signer == selectedSigner,
                    onSelectSigner = onSignerSelected,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (unsupportedSigners.isNotEmpty()) {
                item {
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.strokePrimary
                    )
                }

                item {
                    Text(
                        text = stringResource(
                            R.string.nc_keys_not_yet_supporting_taproot
                        ),
                        style = NunchukTheme.typography.titleSmall,
                    )
                }

                items(unsupportedSigners) { signer ->
                    SignerCard(
                        item = signer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(0.4f),
                    )
                }
            }
        }

        NcPrimaryDarkButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            onClick = onAddExistKeyClicked,
            enabled = selectedSigner != null,
        ) {
            Text(
                text = stringResource(R.string.nc_add_existing_key),
            )
        }
        NcOutlineButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(48.dp),
            onClick = onAddNewKeyClicked,
        ) {
            Text(
                text = stringResource(R.string.nc_take_me_to_add_new_key),
            )
        }
    }
}

@Preview
@Composable
fun TapSignerListContentPreview(
    @PreviewParameter(SignersModelProvider::class) signers: List<SignerModel>,
) {
    NunchukTheme {
        TapSignerListContent(
            signers = signers,
            unsupportedSigners = signers
        )
    }
}

