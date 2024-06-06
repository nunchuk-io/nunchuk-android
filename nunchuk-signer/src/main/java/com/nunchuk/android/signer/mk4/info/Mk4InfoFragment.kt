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

package com.nunchuk.android.signer.mk4.info

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.core.util.COLDCARD_GUIDE_URL
import com.nunchuk.android.core.util.openExternalLink
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.signer.mk4.Mk4Activity
import com.nunchuk.android.signer.mk4.info.component.Mk4InfoContent
import com.nunchuk.android.signer.mk4.intro.Mk4IntroFragmentArgs
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class Mk4InfoFragment : MembershipFragment() {

    private val args: Mk4IntroFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val replacedXfp = (activity as Mk4Activity).replacedXfp.orEmpty()
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                val remainTime by membershipStepManager.remainingTime.collectAsStateWithLifecycle()
                Mk4InfoContent(
                    remainTime = remainTime,
                    onContinueClicked = {
                        findNavController().navigate(
                            Mk4InfoFragmentDirections.actionMk4InfoFragmentToMk4IntroFragment(
                                args.isMembershipFlow
                            )
                        )
                    },
                    onOpenGuideClicked = {
                        requireActivity().openExternalLink(COLDCARD_GUIDE_URL)
                    },
                    isMembershipFlow = args.isMembershipFlow,
                    isReplaceKey = replacedXfp.isNotEmpty(),
                    onMoreClicked = ::handleShowMore
                )
            }
        }
    }
}