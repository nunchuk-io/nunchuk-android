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

@file:OptIn(ExperimentalGlideComposeApi::class)

package com.nunchuk.android.main.nonsubscriber.intro

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
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
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.sheet.BottomSheetOption
import com.nunchuk.android.core.sheet.BottomSheetOptionListener
import com.nunchuk.android.core.sheet.SheetOption
import com.nunchuk.android.core.sheet.SheetOptionType
import com.nunchuk.android.core.util.openExternalLink
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.core.util.showSuccess
import com.nunchuk.android.main.R
import com.nunchuk.android.main.nonsubscriber.intro.model.AssistedWalletPoint
import com.nunchuk.android.widget.NCVerticalInputDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class NonSubscriberIntroFragment : Fragment(), BottomSheetOptionListener {
    private val viewModel: NonSubscriberIntroViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                val state by viewModel.state.collectAsStateWithLifecycle()
                NonSubscriberIntroContent(
                    state,
                    onTellMeMore = {
                        showTellMeMoreDialog()
                    },
                    onTryOnTestNet = {
                        findNavController().navigate(
                            NonSubscriberIntroFragmentDirections.actionNonSubscriberIntroFragmentToTryAssistedWalletFragment()
                        )
                    },
                    onVisitOurWebsite = ::handleGoOurWebsite,
                    onActionClick = {
                        showActionOptions()
                    }
                )
            }
        }
    }

    private fun showActionOptions() {
        (childFragmentManager.findFragmentByTag("BottomSheetOption") as? DialogFragment)?.dismiss()
        val dialog = BottomSheetOption.newInstance(
            listOf(
                SheetOption(
                    type = SheetOptionType.TYPE_ONE_OPTION_CONFIRM,
                    stringId = R.string.nc_dismiss_banner_from_home,
                ),
            )
        )
        dialog.show(childFragmentManager, "BottomSheetOption")
    }

    override fun onOptionClicked(option: SheetOption) {
        if (option.type == SheetOptionType.TYPE_ONE_OPTION_CONFIRM) {
            viewModel.hideUpsellBanner()
        }
    }

    private fun handleGoOurWebsite() {
        requireActivity().openExternalLink("https://nunchuk.io")
    }

    private fun showTellMeMoreDialog() {
        NCVerticalInputDialog(requireContext()).showDialog(
            title = getString(R.string.nc_enter_your_email),
            positiveText = getString(R.string.nc_send_me_the_info),
            negativeText = getString(R.string.nc_visit_our_website),
            neutralText = getString(R.string.nc_text_do_this_later),
            defaultInput = viewModel.getEmail(),
            cancellable = true,
            onPositiveClicked = {
                viewModel.submitEmail(it)
            },
            onNegativeClicked = ::handleGoOurWebsite
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.event.flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect { event ->
                    when (event) {
                        is NonSubscriberIntroEvent.Loading -> showOrHideLoading(event.isLoading)
                        is NonSubscriberIntroEvent.ShowError -> showError(event.message)
                        is NonSubscriberIntroEvent.OnSubmitEmailSuccess -> showSuccess("We sent an email to ${event.email}")
                        NonSubscriberIntroEvent.EmailInvalid -> showError(getString(R.string.nc_text_email_invalid))
                        NonSubscriberIntroEvent.HideUpsellBannerSuccess -> {
                            requireActivity().onBackPressedDispatcher.onBackPressed()
                        }
                    }
                }
        }
    }
}

@Composable
private fun NonSubscriberIntroContent(
    state: NonSubscriberState,
    onTellMeMore: () -> Unit = {},
    onTryOnTestNet: () -> Unit = {},
    onVisitOurWebsite: () -> Unit = {},
    onActionClick: () -> Unit = {}
) {
    NunchukTheme {
        Scaffold { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .navigationBarsPadding()
            ) {
                if (state.items.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier.weight(1.0f),
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                    ) {
                        item {
                            NcImageAppBar(
                                backgroundRes = R.drawable.bg_assisted_wallet,
                                backIconRes = R.drawable.ic_close,
                                actions = {
                                    IconButton(onClick = onActionClick) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_more),
                                            contentDescription = "More icon"
                                        )
                                    }
                                }
                            )
                            Text(
                                modifier = Modifier.padding(
                                    top = 16.dp,
                                    start = 16.dp,
                                    end = 16.dp
                                ),
                                text = state.title,
                                style = NunchukTheme.typography.heading
                            )
                            Text(
                                modifier = Modifier.padding(
                                    top = 16.dp,
                                    start = 16.dp,
                                    end = 16.dp
                                ),
                                text = state.desc,
                                style = NunchukTheme.typography.body
                            )
                        }
                        items(state.items) {
                            AssistedWalletPointWidget(point = it)
                        }

                        item {
                            NcPrimaryDarkButton(
                                modifier = Modifier
                                    .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                                    .fillMaxWidth(),
                                onClick = { onTellMeMore() },
                            ) {
                                Text(text = stringResource(R.string.nc_tell_me_more))
                            }
                            NcOutlineButton(
                                modifier = Modifier
                                    .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                                    .fillMaxWidth(),
                                onClick = { onTryOnTestNet() },
                            ) {
                                Text(text = stringResource(R.string.nc_try_on_testnet))
                            }
                            NcOutlineButton(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                onClick = { onVisitOurWebsite() },
                            ) {
                                Text(text = stringResource(R.string.nc_visit_our_website))
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun AssistedWalletPointWidget(modifier: Modifier = Modifier, point: AssistedWalletPoint) {
    Row(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(color = colorResource(id = R.color.nc_grey_light), shape = CircleShape),
        ) {
            GlideImage(
                model = point.iconUrl,
                contentDescription = "",
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.Center),
            )
        }
        Column(
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1.0f)
        ) {
            Text(text = point.title, style = NunchukTheme.typography.title)
            Text(
                modifier = Modifier.padding(top = 8.dp),
                text = point.desc,
                style = NunchukTheme.typography.body
            )
        }
    }
}

@Preview
@Composable
private fun AssistedWalletPointWidgetPreview() {
    AssistedWalletPointWidget(
        point = AssistedWalletPoint(
            iconUrl = "https://cdn-icons-png.flaticon.com/512/9133/9133608.png",
            title = "Emergency lockdown",
            desc = "Lock your wallet access when there is a security threat or when you go off the grid."
        )
    )
}

@Preview
@Composable
private fun NonSubscriberIntroScreenPreview() {
    NonSubscriberIntroContent(
        NonSubscriberState(
            title = "Get more out of Nunchuk with an assisted wallet",
            desc = "Here are some of the things that you can do with an assisted wallet:",
            items = listOf(
                AssistedWalletPoint(
                    iconUrl = "https://cdn-icons-png.flaticon.com/512/9133/9133608.png",
                    "No single point of failure",
                    "Eliminate single points of failure by using multisig: two keys are always required for spending. The loss of any one key will not compromise your funds.",
                ),
                AssistedWalletPoint(
                    iconUrl = "https://cdn-icons-png.flaticon.com/512/9133/9133608.png",
                    "No single point of failure",
                    "Eliminate single points of failure by using multisig: two keys are always required for spending. The loss of any one key will not compromise your funds.",
                ),
                AssistedWalletPoint(
                    iconUrl = "https://cdn-icons-png.flaticon.com/512/9133/9133608.png",
                    "No single point of failure",
                    "Eliminate single points of failure by using multisig: two keys are always required for spending. The loss of any one key will not compromise your funds.",
                ), AssistedWalletPoint(
                    iconUrl = "https://cdn-icons-png.flaticon.com/512/9133/9133608.png",
                    "No single point of failure",
                    "Eliminate single points of failure by using multisig: two keys are always required for spending. The loss of any one key will not compromise your funds.",
                )
            )
        )
    )
}