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
import com.nunchuk.android.compose.NcSecondaryButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.util.ClickAbleText
import com.nunchuk.android.main.R
import com.nunchuk.android.share.membership.MembershipFragment

/**
 * SET-02a "Customize inheritance": shown before the primary policy sheet when the primary is still
 * the guided default. Continue hands back to the setup screen, which then opens the policy sheet.
 */
class CustomizeInheritanceIntroFragment : MembershipFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                val remainingTime by membershipStepManager.remainingTime.collectAsStateWithLifecycle()
                CustomizeInheritanceIntroContent(
                    remainingTime = remainingTime,
                    onMoreClicked = ::handleShowMore,
                    onContinueClicked = {
                        setFragmentResult(REQUEST_KEY, bundleOf(EXTRA_CONTINUE to true))
                        findNavController().popBackStack()
                    },
                    onKeepCurrentSetupClicked = { findNavController().popBackStack() },
                )
            }
        }
    }

    companion object {
        const val REQUEST_KEY = "CustomizeInheritanceIntroFragment"
        const val EXTRA_CONTINUE = "extra_continue"
    }
}

@Composable
private fun CustomizeInheritanceIntroContent(
    remainingTime: Int = 0,
    onMoreClicked: () -> Unit = {},
    onContinueClicked: () -> Unit = {},
    onKeepCurrentSetupClicked: () -> Unit = {},
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
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    NcPrimaryDarkButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onContinueClicked,
                    ) {
                        Text(text = stringResource(R.string.nc_text_continue))
                    }
                    NcSecondaryButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onKeepCurrentSetupClicked,
                    ) {
                        Text(text = stringResource(R.string.nc_keep_current_setup))
                    }
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
                    text = stringResource(R.string.nc_customize_inheritance),
                    style = NunchukTheme.typography.heading,
                )
                Text(
                    text = stringResource(R.string.nc_customize_inheritance_desc),
                    style = NunchukTheme.typography.body,
                )
                Text(
                    text = stringResource(R.string.nc_customize_inheritance_what_you_can_change),
                    style = NunchukTheme.typography.body,
                )
                NCLabelWithIndex(
                    index = 1,
                    title = stringResource(R.string.nc_customize_inheritance_keys),
                    label = stringResource(R.string.nc_customize_inheritance_keys_desc),
                )
                NCLabelWithIndex(
                    index = 2,
                    title = stringResource(R.string.nc_customize_inheritance_required_signatures),
                    label = stringResource(R.string.nc_customize_inheritance_required_signatures_desc),
                )
                Text(
                    text = stringResource(R.string.nc_customize_inheritance_spending_unchanged),
                    style = NunchukTheme.typography.body,
                )
                NcHintMessage(
                    messages = listOf(ClickAbleText(stringResource(R.string.nc_customize_inheritance_single_key_hint))),
                    type = HighlightMessageType.HINT,
                )
                NcHintMessage(
                    modifier = Modifier.padding(bottom = 16.dp),
                    messages = listOf(ClickAbleText(stringResource(R.string.nc_customize_inheritance_final_hint))),
                    type = HighlightMessageType.HINT,
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun CustomizeInheritanceIntroContentPreview() {
    CustomizeInheritanceIntroContent(remainingTime = 10)
}
