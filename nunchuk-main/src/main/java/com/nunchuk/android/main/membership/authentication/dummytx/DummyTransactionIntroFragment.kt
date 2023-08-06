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

package com.nunchuk.android.main.membership.authentication.dummytx

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.compose.NcHighlightText
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.util.getCurrencyAmount
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.authentication.WalletAuthenticationViewModel
import com.nunchuk.android.model.Amount

class DummyTransactionIntroFragment : Fragment() {
    private val activityViewModel by activityViewModels<WalletAuthenticationViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                val uiState by activityViewModel.state.collectAsStateWithLifecycle()
                DummyTransactionIntroContent(
                    pendingSignature = uiState.pendingSignature,
                    onContinueClicked = {
                        findNavController().navigate(
                            DummyTransactionIntroFragmentDirections.actionDummyTransactionIntroToDummyTransactionDetailsFragment()
                        )
                    }
                ) {
                    requireActivity().finish()
                }
            }
        }
    }
}

@Composable
fun DummyTransactionIntroContent(
    pendingSignature: Int = 0,
    onContinueClicked: () -> Unit = {},
    onCancelClicked: () -> Unit = {},
) {
    NunchukTheme {
        Scaffold { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
            ) {
                NcTopAppBar(title = "", elevation = 0.dp)
                Text(
                    modifier = Modifier.padding(top = 0.dp, start = 16.dp, end = 16.dp),
                    text = stringResource(R.string.nc_signatures_required),
                    style = NunchukTheme.typography.heading
                )
                NcHighlightText(
                    modifier = Modifier.padding(16.dp),
                    text = stringResource(
                        R.string.nc_dummy_transaction_desc,
                        Amount(value = 10000).getCurrencyAmount()
                    )
                )
                Spacer(modifier = Modifier.weight(1.0f))
                Row(
                    modifier = Modifier
                        .align(CenterHorizontally)
                        .padding(bottom = 16.dp),
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_pending_signatures),
                        contentDescription = "Icon pending signatures"
                    )
                    Text(
                        modifier = Modifier.padding(start = 4.dp),
                        text = pluralStringResource(
                            R.plurals.nc_transaction_pending_signature,
                            pendingSignature,
                            pendingSignature
                        ), style = NunchukTheme.typography.bodySmall
                    )
                }
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                    onClick = onContinueClicked,
                ) {
                    Text(text = stringResource(R.string.nc_sign_dummy_transaction))
                }
                TextButton(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    onClick = onCancelClicked
                ) {
                    Text(
                        text = stringResource(R.string.nc_text_do_this_later),
                        style = NunchukTheme.typography.title
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun DummyTransactionIntroContentPreview() {
    DummyTransactionIntroContent()
}