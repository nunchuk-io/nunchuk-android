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

package com.nunchuk.android.main.membership.authentication.dummytx

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.compose.*
import com.nunchuk.android.core.util.getCurrencyAmount
import com.nunchuk.android.main.R
import com.nunchuk.android.model.Amount

class DummyTransactionIntroFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                DummyTransactionIntroContent(
                    onContinueClicked = {
                        findNavController().navigate(
                            DummyTransactionIntroFragmentDirections.actionDummyTransactionIntroToDummyTransactionDetailsFragment()
                        )
                    },
                    onCancelClicked = {
                        requireActivity().finish()
                    }
                )
            }
        }
    }
}

@Composable
fun DummyTransactionIntroContent(
    onContinueClicked: () -> Unit = {},
    onCancelClicked: () -> Unit = {},
) {
    NunchukTheme {
        Scaffold { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
            ) {
                NcTopAppBar(title = "")
                Text(
                    modifier = Modifier.padding(top = 0.dp, start = 16.dp, end = 16.dp),
                    text = stringResource(R.string.nc_two_signatures_required),
                    style = NunchukTheme.typography.heading
                )
                NcHighlightText(
                    modifier = Modifier.padding( 16.dp),
                    text = stringResource(R.string.nc_dummy_transaction_desc, Amount(value = 10000).getCurrencyAmount())
                )
                Spacer(modifier = Modifier.weight(1.0f))
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                    onClick = onContinueClicked,
                ) {
                    Text(text = stringResource(R.string.nc_sign_dummy_transaction))
                }
                NcOutlineButton(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    onClick = onCancelClicked
                ) {
                    Text(text = stringResource(R.string.nc_cancel))
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