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
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
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
import com.nunchuk.android.core.util.openExternalLink
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.core.util.showSuccess
import com.nunchuk.android.main.R
import com.nunchuk.android.main.nonsubscriber.intro.model.AssistedWalletPoint
import com.nunchuk.android.widget.NCVerticalInputDialog
import dagger.hilt.android.AndroidEntryPoint

@OptIn(ExperimentalLifecycleComposeApi::class)
@AndroidEntryPoint
class NonSubscriberIntroFragment : Fragment() {
    private val viewModel: NonSubscriberIntroViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
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
                    onVisitOurWebsite = ::handleGoOurWebsite
                )
            }
        }
    }

    private fun handleGoOurWebsite() {
        requireActivity().openExternalLink("https://nunchuk.io")
    }

    private fun showTellMeMoreDialog() {
        NCVerticalInputDialog(requireContext()).showDialog(
            title = getString(R.string.nc_enter_your_email),
            positiveText = getString(R.string.nc_send_me_the_info),
            negativeText = getString(R.string.nc_or_visit_our_website),
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
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.event.flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect { event ->
                    when (event) {
                        is NonSubscriberIntroEvent.Loading -> showOrHideLoading(event.isLoading)
                        is NonSubscriberIntroEvent.ShowError -> showError(event.message)
                        is NonSubscriberIntroEvent.OnSubmitEmailSuccess -> showSuccess("We sent an email to ${event.email}")
                        NonSubscriberIntroEvent.EmailInvalid -> showError(getString(R.string.nc_text_email_invalid))
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
) {
    NunchukTheme {
        Scaffold { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .navigationBarsPadding()
            ) {
                NcImageAppBar(backgroundRes = R.drawable.bg_assisted_wallet)
                if (state.items.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier.weight(1.0f),
                        contentPadding = PaddingValues(vertical = 24.dp, horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                    ) {
                        item {
                            Text(
                                modifier = Modifier.padding(top = 16.dp),
                                text = state.title,
                                style = NunchukTheme.typography.heading
                            )
                            Text(
                                modifier = Modifier.padding(top = 16.dp),
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
                                    .padding(top = 16.dp)
                                    .fillMaxWidth(),
                                onClick = { onTellMeMore() },
                            ) {
                                Text(text = stringResource(R.string.nc_tell_me_more))
                            }
                            NcOutlineButton(
                                modifier = Modifier
                                    .padding(top = 16.dp)
                                    .fillMaxWidth(),
                                onClick = { onTryOnTestNet() },
                            ) {
                                Text(text = stringResource(R.string.nc_try_on_testnet))
                            }
                            NcOutlineButton(
                                modifier = Modifier
                                    .padding(top = 16.dp)
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

@Composable
private fun AssistedWalletPointWidget(modifier: Modifier = Modifier, point: AssistedWalletPoint) {
    Row(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(color = colorResource(id = R.color.nc_grey_light), shape = CircleShape),
        ) {
            GlideImage(
                model = point.iconUrl,
                contentDescription = "",
                modifier = Modifier.size(24.dp),
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