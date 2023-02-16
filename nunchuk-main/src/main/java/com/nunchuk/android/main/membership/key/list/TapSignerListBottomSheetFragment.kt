/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *							          *
 * Copyright (C) 2022 Nunchuk								              *
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
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.*
import com.nunchuk.android.core.base.BaseComposeBottomSheet
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.toReadableDrawableResId
import com.nunchuk.android.core.util.toReadableSignerType
import com.nunchuk.android.main.R
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
            setContent {
                NunchukTheme {
                    TapSignerListScreen(viewModel, args)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        flowObserver(viewModel.event) { event ->
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
            dismissAllowingStateLoss()
        }
    }

    companion object {
        const val REQUEST_KEY = "TapSignerListBottomSheetFragment"
        const val EXTRA_SELECTED_SIGNER_ID = "EXTRA_SELECTED_SIGNER_ID"
    }
}

@OptIn(ExperimentalLifecycleComposeApi::class)
@Composable
private fun TapSignerListScreen(
    viewModel: TapSingerListBottomSheetViewModel,
    args: TapSignerListBottomSheetFragmentArgs,
) {
    val selectedSigner by viewModel.selectSingle.collectAsStateWithLifecycle()
    val onBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    TapSignerListContent(
        onCloseClicked = {
            onBackPressedDispatcher?.onBackPressed()
        },
        onAddExistKeyClicked = viewModel::onAddExistingKey,
        onAddNewKeyClicked = viewModel::onAddNewKey,
        signers = args.signers.toList(),
        type = args.type,
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
) {
    val signerLabel = when (type) {
        SignerType.NFC -> "TAPSIGNER(s)"
        SignerType.COLDCARD_NFC -> "COLDCARD(s)"
        SignerType.AIRGAP -> "Air-gapped key(s)"
        else -> ""
    }
    Column(
        modifier = Modifier.background(
            color = MaterialTheme.colors.surface,
            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
        )
    ) {
        IconButton(
            modifier = Modifier.padding(top = 40.dp), onClick = onCloseClicked
        ) {
            Icon(
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
        Text(
            modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp),
            text = stringResource(R.string.nc_notice_you_have_exist_key, signerLabel),
            style = NunchukTheme.typography.body,
        )
        LazyColumn(
            modifier = Modifier.padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            items(signers) { signer ->
                SignerCard(signer, signer == selectedSigner, onSignerSelected)
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

@Composable
private fun SignerCard(
    signer: SignerModel,
    isSelected: Boolean,
    onSignerSelected: (signer: SignerModel) -> Unit = {},
) {
    Row(
        modifier = Modifier.clickable { onSignerSelected(signer) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NcCircleImage(resId = signer.type.toReadableDrawableResId())
        Column(
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1.0f)
        ) {
            Text(text = signer.name, style = NunchukTheme.typography.body)
            Text(
                modifier = Modifier.padding(top = 4.dp),
                text = signer.getXfpOrCardIdLabel(),
                style = NunchukTheme.typography.bodySmall.copy(
                    color = colorResource(
                        id = R.color.nc_grey_dark_color
                    )
                ),
            )
            NcTag(
                modifier = Modifier
                    .padding(top = 6.dp),
                label = signer.toReadableSignerType(LocalContext.current),
            )
        }
        RadioButton(selected = isSelected, onClick = { onSignerSelected(signer) })
    }
}

@Preview
@Composable
fun TapSignerListContentPreview() {
    NunchukTheme {
        TapSignerListContent(
            signers = listOf(
                SignerModel(
                    "123", "Tom’s TAPSIGNER", fingerPrint = "79EB35F4", derivationPath = ""
                ),
                SignerModel(
                    "123", "Tom’s TAPSIGNER 2", fingerPrint = "79EB35F4", derivationPath = ""
                ),
            ),
        )
    }
}

