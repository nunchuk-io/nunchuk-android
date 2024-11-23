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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.provider.SignersModelProvider
import com.nunchuk.android.core.base.BaseComposeBottomSheet
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.component.SignerCard
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.type.SignerType
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TapSignerListBottomSheetFragment : BaseComposeBottomSheet() {
    @Inject
    lateinit var navigator: NunchukNavigator

    private val viewModel by viewModels<TapSingerListBottomSheetViewModel>()
    private val args: TapSignerListBottomSheetFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                NunchukTheme {
                    TapSignerListScreen(viewModel, args, onCloseClicked = ::dismissAllowingStateLoss)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        flowObserver(viewModel.event) { event ->
            findNavController().popBackStack()
            when (event) {
                is TapSignerListBottomSheetEvent.OnAddExistingKey -> setFragmentResult(
                    REQUEST_KEY,
                    TapSignerListBottomSheetFragmentArgs(
                        listOf(event.signer).toTypedArray(),
                        args.type
                    ).toBundle()
                )

                TapSignerListBottomSheetEvent.OnAddNewKey -> setFragmentResult(
                    REQUEST_KEY, TapSignerListBottomSheetFragmentArgs(
                        emptyArray(),
                        args.type
                    ).toBundle()
                )
            }
        }
    }

    companion object {
        const val REQUEST_KEY = "TapSignerListBottomSheetFragment"
        const val EXTRA_SELECTED_SIGNER_ID = "EXTRA_SELECTED_SIGNER_ID"
    }
}

@Composable
private fun TapSignerListScreen(
    viewModel: TapSingerListBottomSheetViewModel,
    args: TapSignerListBottomSheetFragmentArgs,
    onCloseClicked : () -> Unit = {},
) {
    val selectedSigner by viewModel.selectSingle.collectAsStateWithLifecycle()

    TapSignerListContent(
        onCloseClicked = onCloseClicked,
        onAddExistKeyClicked = viewModel::onAddExistingKey,
        onAddNewKeyClicked = viewModel::onAddNewKey,
        signers = args.signers.toList(),
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
        modifier = Modifier.background(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
        ).nestedScroll(rememberNestedScrollInteropConnection())
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
                text = description.ifEmpty { stringResource(R.string.nc_notice_you_have_exist_key, signerLabel) },
                style = NunchukTheme.typography.body,
            )
        }
        LazyColumn(
            modifier = Modifier.padding(horizontal = 16.dp).heightIn(max = screenHeightDp.div(2).dp),
            contentPadding = PaddingValues(vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            items(signers) { signer ->
                SignerCard(
                    signer = signer,
                    isSelected = signer == selectedSigner,
                    onSignerSelected = onSignerSelected,
                    modifier = Modifier.fillMaxWidth()
                )
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
            signers = signers
        )
    }
}

