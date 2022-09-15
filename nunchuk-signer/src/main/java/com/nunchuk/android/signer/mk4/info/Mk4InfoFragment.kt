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

package com.nunchuk.android.signer.mk4.info

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.core.util.ClickAbleText
import com.nunchuk.android.core.util.makeTextLink
import com.nunchuk.android.core.util.openExternalLink
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.databinding.FragmentMk4InfoBinding
import com.nunchuk.android.widget.util.setOnDebounceClickListener
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class Mk4InfoFragment : BaseFragment<FragmentMk4InfoBinding>() {

    override fun initializeBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): FragmentMk4InfoBinding {
        return FragmentMk4InfoBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    private fun initView() {
        binding.toolbar.setNavigationOnClickListener {
            activity?.finish()
        }
        binding.tvDescOne.makeTextLink(
            ClickAbleText(content = getString(R.string.nc_refer_to)),
            ClickAbleText(content = getString(R.string.nc_this_starter_guide), onClick = {
                requireActivity().openExternalLink(COLDCARD_GUIDE_URL)
            })
        )
        binding.btnContinue.setOnDebounceClickListener {
            findNavController().navigate(Mk4InfoFragmentDirections.actionMk4InfoFragmentToMk4IntroFragment())
        }
    }

    companion object {
        private const val COLDCARD_GUIDE_URL = "https://coldcard.com/docs/quick"
    }
}