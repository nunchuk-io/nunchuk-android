package com.nunchuk.android.main.membership.onchaintimelock.customize

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.compose.HighlightMessageType
import com.nunchuk.android.compose.NCLabelWithIndex
import com.nunchuk.android.compose.NcHintMessage
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.util.ClickAbleText
import com.nunchuk.android.main.R
import com.nunchuk.android.share.membership.MembershipFragment

/**
 * FBK-01 "Fallbacks": shown before the first fallback's policy sheet. Continue hands back to the
 * setup screen, which then opens the policy sheet (FBK-01a).
 */
class FallbackInheritanceIntroFragment : MembershipFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                val remainingTime by membershipStepManager.remainingTime.collectAsStateWithLifecycle()
                FallbackInheritanceIntroContent(
                    remainingTime = remainingTime,
                    onMoreClicked = ::handleShowMore,
                    onContinueClicked = {
                        setFragmentResult(REQUEST_KEY, bundleOf(EXTRA_CONTINUE to true))
                        findNavController().popBackStack()
                    },
                )
            }
        }
    }

    companion object {
        const val REQUEST_KEY = "FallbackInheritanceIntroFragment"
        const val EXTRA_CONTINUE = "extra_continue"
    }
}

@Composable
private fun FallbackInheritanceIntroContent(
    remainingTime: Int = 0,
    onMoreClicked: () -> Unit = {},
    onContinueClicked: () -> Unit = {},
) {
    NunchukTheme {
        Scaffold(
            modifier = Modifier.navigationBarsPadding(),
            topBar = {
                NcTopAppBar(
                    title = if (remainingTime <= 0) "" else stringResource(R.string.nc_estimate_remain_time, remainingTime),
                    actions = {
                        IconButton(onClick = onMoreClicked) {
                            NcIcon(
                                painter = painterResource(id = R.drawable.ic_more),
                                contentDescription = stringResource(R.string.nc_membership_cd_more_icon)
                            )
                        }
                    },
                )
            },
            bottomBar = {
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    onClick = onContinueClicked,
                ) {
                    Text(text = stringResource(R.string.nc_text_continue))
                }
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    modifier = Modifier.padding(top = 8.dp),
                    text = stringResource(R.string.nc_replace_fallbacks_section),
                    style = NunchukTheme.typography.heading,
                )
                Text(text = stringResource(R.string.nc_fallbacks_intro_desc), style = NunchukTheme.typography.body)
                Text(text = stringResource(R.string.nc_fallbacks_intro_each_has), style = NunchukTheme.typography.body)
                NCLabelWithIndex(
                    index = 1,
                    title = stringResource(R.string.nc_fallbacks_intro_own_keys),
                    label = stringResource(R.string.nc_fallbacks_intro_own_keys_desc),
                    borderWidth = 1.5.dp,
                )
                NCLabelWithIndex(
                    index = 2,
                    title = stringResource(R.string.nc_fallbacks_intro_own_timelock),
                    label = stringResource(R.string.nc_fallbacks_intro_own_timelock_desc),
                    borderWidth = 1.5.dp,
                )
                NcHintMessage(
                    messages = listOf(ClickAbleText(stringResource(R.string.nc_fallbacks_intro_single_key_hint))),
                    type = HighlightMessageType.HINT,
                )
                NcHintMessage(
                    modifier = Modifier.padding(bottom = 16.dp),
                    messages = listOf(ClickAbleText(stringResource(R.string.nc_fallbacks_intro_no_reserve_hint))),
                    type = HighlightMessageType.HINT,
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun FallbackInheritanceIntroContentPreview() {
    FallbackInheritanceIntroContent(remainingTime = 10)
}
