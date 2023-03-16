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

package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.magicalphrase

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.*
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.main.R
import com.nunchuk.android.share.membership.MembershipFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MagicalPhraseIntroFragment : MembershipFragment() {
    private val viewModel: MagicalPhraseIntroViewModel by viewModels()
    private val args by navArgs<MagicalPhraseIntroFragmentArgs>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                MagicalPhraseIntroScreen(viewModel)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.event.flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect { event ->
                    when (event) {
                        is MagicalPhraseIntroEvent.OnContinueClicked -> {
                            findNavController().navigate(
                                MagicalPhraseIntroFragmentDirections.actionMagicalPhraseIntroFragmentToFindBackupPasswordFragment(
                                    magicalPhrase = event.magicalPhrase,
                                    walletId = args.walletId
                                )
                            )
                        }
                        is MagicalPhraseIntroEvent.Error -> showError(message = event.message)
                        is MagicalPhraseIntroEvent.Loading -> showOrHideLoading(loading = event.loading)
                    }
                }
        }
    }
}

@Composable
private fun MagicalPhraseIntroScreen(viewModel: MagicalPhraseIntroViewModel = viewModel()) {
    val remainTime by viewModel.remainTime.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    MagicalPhraseIntroContent(
        remainTime = remainTime,
        magicalPhrase = state.magicalPhrase.orEmpty(),
        viewModel::onContinueClicked
    )
}

@Composable
private fun MagicalPhraseIntroContent(
    remainTime: Int = 0,
    magicalPhrase: String = "",
    onContinueClicked: () -> Unit = {},
) {
    NunchukTheme {
        Scaffold { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .navigationBarsPadding()
            ) {
                Column(
                    modifier = Modifier
                        .background(colorResource(id = R.color.nc_denim_tint_color))
                        .statusBarsPadding()
                ) {
                    NcTopAppBar(
                        backgroundColor = colorResource(id = R.color.nc_denim_tint_color),
                        title = stringResource(
                            id = R.string.nc_estimate_remain_time,
                            remainTime
                        ),
                    )
                }
                Column(
                    modifier = Modifier
                        .background(color = colorResource(id = R.color.nc_denim_tint_color))
                        .height(215.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                modifier = Modifier.fillMaxWidth(),
                                text = magicalPhrase,
                                style = NunchukTheme.typography.body,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                Text(
                    modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
                    text = stringResource(R.string.nc_here_is_plan_magical_phrase),
                    style = NunchukTheme.typography.heading
                )
                NcHighlightText(
                    modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
                    text = stringResource(R.string.nc_magical_phrase_desc),
                    style = NunchukTheme.typography.body
                )
                Spacer(modifier = Modifier.weight(1.0f))
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    onClick = onContinueClicked,
                ) {
                    Text(text = stringResource(id = R.string.nc_text_continue))
                }
            }
        }
    }
}

@Preview
@Composable
private fun MagicalPhraseIntroScreenPreview() {
    MagicalPhraseIntroContent(

    )
}