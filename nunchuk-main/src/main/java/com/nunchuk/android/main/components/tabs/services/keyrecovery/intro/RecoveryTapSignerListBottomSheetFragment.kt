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

package com.nunchuk.android.main.components.tabs.services.keyrecovery.intro

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.NcCircleImage
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcRadioButton
import com.nunchuk.android.compose.NcTag
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.provider.SignersModelProvider
import com.nunchuk.android.core.base.BaseComposeBottomSheet
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.toReadableDrawableResId
import com.nunchuk.android.main.R
import com.nunchuk.android.nav.NunchukNavigator
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class RecoveryTapSignerListBottomSheetFragment : BaseComposeBottomSheet() {
    @Inject
    lateinit var navigator: NunchukNavigator

    private val viewModel by viewModels<RecoveryTapSignerListBottomSheetViewModel>()
    private val args: RecoveryTapSignerListBottomSheetFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                NunchukTheme {
                    RecoveryTapSignerListScreen(viewModel, args)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        flowObserver(viewModel.event) { event ->
            when (event) {
                RecoveryTapSignerListBottomSheetEvent.ContinueClick -> {
                    setFragmentResult(
                        REQUEST_KEY,
                        bundleOf(EXTRA_SIGNER to viewModel.selectedSigner)
                    )
                    dismissAllowingStateLoss()
                }
            }
        }
    }

    companion object {
        const val REQUEST_KEY = "RecoveryTapSignerListBottomSheetFragment"
        const val EXTRA_SIGNER = "EXTRA_SIGNER"
    }
}

@Composable
private fun RecoveryTapSignerListScreen(
    viewModel: RecoveryTapSignerListBottomSheetViewModel,
    args: RecoveryTapSignerListBottomSheetFragmentArgs,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val onBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    RecoveryTapSignerListContent(
        signers = args.signers.toList(),
        selectedSignerId = state.selectedSignerId.orEmpty(),
        onSignerSelected = viewModel::onSignerSelected,
        onCloseClick = {
            onBackPressedDispatcher?.onBackPressed()
        },
        onContinueClick = viewModel::onContinueClicked
    )
}

@Composable
private fun RecoveryTapSignerListContent(
    signers: List<SignerModel> = emptyList(),
    selectedSignerId: String = "",
    onSignerSelected: (signer: SignerModel) -> Unit = {},
    onCloseClick: () -> Unit = {},
    onContinueClick: () -> Unit = {},
) {
    Column(
        modifier = Modifier.background(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
        ).padding(bottom = 21.dp)
    ) {
        IconButton(
            modifier = Modifier.padding(top = 28.dp), onClick = onCloseClick
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_close),
                contentDescription = "Back Icon"
            )
        }
        Text(
            modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
            text = stringResource(R.string.nc_select_tapsigner_recover),
            style = NunchukTheme.typography.title,
        )

        LazyColumn(
            modifier = Modifier.padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            items(signers) { signer ->
                SignerCard(signer, signer.id == selectedSignerId, onSignerSelected)
            }
        }
        NcPrimaryDarkButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            onClick = onContinueClick,
            enabled = selectedSignerId.isNotEmpty(),
        ) {
            Text(
                text = stringResource(R.string.nc_text_continue),
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
        NcCircleImage(resId = signer.toReadableDrawableResId())
        Column(
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1.0f)
        ) {
            Text(text = signer.name, style = NunchukTheme.typography.body)
            NcTag(
                modifier = Modifier
                    .padding(top = 6.dp),
                label = stringResource(id = R.string.nc_nfc),
            )
            Text(
                modifier = Modifier.padding(top = 4.dp),
                text = signer.getXfpOrCardIdLabel(),
                style = NunchukTheme.typography.bodySmall.copy(
                    color = colorResource(
                        id = R.color.nc_grey_dark_color
                    )
                ),
            )
        }
        NcRadioButton(selected = isSelected, onClick = {
            onSignerSelected(signer)
        })
    }
}

@Preview
@Composable
fun RecoveryTapSignerListContentPreview(
    @PreviewParameter(SignersModelProvider::class) signers: List<SignerModel>,
) {
    NunchukTheme {
        RecoveryTapSignerListContent(
            signers = signers
        )
    }
}