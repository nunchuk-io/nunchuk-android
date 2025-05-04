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

package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claimnote

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.montserratMedium
import com.nunchuk.android.core.sheet.BottomSheetOptionListener
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.core.util.getCurrencyAmount
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.core.util.toAmount
import com.nunchuk.android.main.R
import com.nunchuk.android.share.membership.MembershipFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class InheritanceClaimNoteFragment : MembershipFragment(), BottomSheetOptionListener {

    private val viewModel: InheritanceClaimNoteViewModel by viewModels()
    private val args: InheritanceClaimNoteFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                InheritanceClaimNoteScreen(viewModel, onDoneClick = {
                    requireActivity().finish()
                })
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        flowObserver(viewModel.event) { event ->
            when (event) {
                is InheritanceClaimNoteEvent.Loading -> showOrHideLoading(loading = event.isLoading)
                is InheritanceClaimNoteEvent.WithdrawClick -> {
                    findNavController().navigate(
                        InheritanceClaimNoteFragmentDirections.actionInheritanceClaimNoteFragmentToInheritanceClaimWithdrawBitcoinFragment(
                            walletBalance = viewModel.getBalance().toFloat(),
                            signers = args.signers,
                            magic = args.magic,
                            derivationPaths = args.derivationPaths,
                        )
                    )
                }
                is InheritanceClaimNoteEvent.Error -> showError(message = event.message)
            }
        }
    }
}

@Composable
fun InheritanceClaimNoteScreen(
    viewModel: InheritanceClaimNoteViewModel = viewModel(),
    onDoneClick: () -> Unit

) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    InheritanceClaimNoteContent(
        note = state.inheritanceAdditional?.inheritance?.note.orEmpty(),
        balance = state.inheritanceAdditional?.balance ?: 0.0,
        onWithdrawClick = viewModel::onWithdrawClick,
        onDoneClick = onDoneClick
    )
}

@Composable
private fun InheritanceClaimNoteContent(
    note: String = "",
    balance: Double = 0.0,
    onWithdrawClick: () -> Unit = {},
    onDoneClick: () -> Unit = {}
) {
    NunchukTheme {
        Scaffold { innerPadding ->
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
            ) {
                Column(
                    modifier = Modifier
                        .background(colorResource(id = R.color.nc_denim_tint_color))
                        .statusBarsPadding()
                ) {
                    NcTopAppBar(
                        backgroundColor = colorResource(id = R.color.nc_denim_tint_color),
                        title = "",
                    )
                }
                LazyColumn(modifier = Modifier.weight(1.0f)) {
                    item {
                        Column(
                            modifier = Modifier
                                .background(color = colorResource(id = R.color.nc_denim_tint_color))
                                .height(215.dp)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .fillMaxWidth(),
                                text = stringResource(R.string.nc_your_inheritance),
                                style = NunchukTheme.typography.title,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .fillMaxWidth(),
                                text = balance.toAmount().getBTCAmount(),
                                style = TextStyle(
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = montserratMedium,
                                    color = colorResource(
                                        id = R.color.nc_text_primary
                                    )
                                ),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .fillMaxWidth(),
                                text = balance.toAmount().getCurrencyAmount(),
                                style = NunchukTheme.typography.title,
                                textAlign = TextAlign.Center
                            )
                        }
                        Text(
                            modifier = Modifier.padding(top = 12.dp, start = 16.dp, end = 16.dp),
                            text = stringResource(R.string.nc_congratulation_unlocked_your_inheritance),
                            style = NunchukTheme.typography.heading
                        )
                        if (note.isBlank().not()) {
                            Text(
                                modifier = Modifier.padding(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = 12.dp
                                ),
                                text = stringResource(R.string.nc_you_have_a_message_below),
                                style = NunchukTheme.typography.body
                            )
                            Box(
                                modifier = Modifier
                                    .padding(top = 12.dp, start = 16.dp, end = 16.dp)
                                    .background(
                                        color = colorResource(
                                            id = R.color.nc_grey_light
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                            ) {
                                Text(
                                    modifier = Modifier.padding(12.dp),
                                    style = NunchukTheme.typography.body,
                                    text = note
                                )
                            }
                        }
                    }
                }
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    enabled = balance > 0,
                    onClick = onWithdrawClick,
                ) {
                    Text(text = stringResource(id = R.string.nc_withdraw_bitcoin))
                }
                NcOutlineButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp)
                        .height(48.dp),
                    onClick = onDoneClick,
                ) {
                    Text(text = stringResource(R.string.nc_text_done))
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun InheritanceClaimNotePreview() {
    InheritanceClaimNoteContent(

    )
}