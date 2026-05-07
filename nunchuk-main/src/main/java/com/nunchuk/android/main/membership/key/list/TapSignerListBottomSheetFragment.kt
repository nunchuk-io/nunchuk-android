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
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.base.BaseComposeBottomSheet
import com.nunchuk.android.core.signer.SelectSignerArgs
import com.nunchuk.android.core.signer.TapSignerListScreen
import com.nunchuk.android.main.membership.MembershipViewModel
import com.nunchuk.android.nav.NunchukNavigator
import dagger.hilt.android.AndroidEntryPoint
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
                val membershipState by activityViewModel.state.collectAsStateWithLifecycle()
                val supportedSigners = membershipState.supportedTypes

                NunchukTheme {
                    TapSignerListScreen(
                        args = SelectSignerArgs(
                            signers = args.signers.toList(),
                            type = args.type,
                            description = args.description,
                            ignoreIndexCheckForAcctX = args.ignoreIndexCheckForAcctX,
                        ),
                        onCloseClicked = ::dismissAllowingStateLoss,
                        supportedSigners = supportedSigners,
                        onAddExistKey = { signer ->
                            findNavController().popBackStack()
                            setFragmentResult(
                                REQUEST_KEY,
                                TapSignerListBottomSheetFragmentArgs(
                                    listOf(signer).toTypedArray(),
                                    args.type,
                                    args.description,
                                    args.ignoreIndexCheckForAcctX
                                ).toBundle()
                            )
                        },
                        onAddNewKey = {
                            findNavController().popBackStack()
                            setFragmentResult(
                                REQUEST_KEY, TapSignerListBottomSheetFragmentArgs(
                                    emptyArray(),
                                    args.type,
                                    args.description,
                                    args.ignoreIndexCheckForAcctX
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
