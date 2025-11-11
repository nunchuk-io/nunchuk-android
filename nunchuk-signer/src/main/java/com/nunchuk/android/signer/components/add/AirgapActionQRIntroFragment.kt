package com.nunchuk.android.signer.components.add

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
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
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.compose.LabelNumberAndDesc
import com.nunchuk.android.compose.NcClickableText
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.signer.OnChainAddSignerParam
import com.nunchuk.android.core.util.ClickAbleText
import com.nunchuk.android.core.util.JADE_GUIDE_URL
import com.nunchuk.android.core.util.openExternalLink
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.signer.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AirgapActionQRIntroFragment : MembershipFragment() {

    private val isMembershipFlow: Boolean by lazy {
        (requireActivity() as AddAirgapSignerActivity).isMembershipFlow
    }

    private val onChainAddSignerParam: OnChainAddSignerParam? by lazy {
        (requireActivity() as AddAirgapSignerActivity).onChainAddSignerParam
    }

    private val viewModel: AirgapIntroViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val isMembershipFlow = (requireActivity() as AddAirgapSignerActivity).isMembershipFlow
        val signerTag = (requireActivity() as AddAirgapSignerActivity).signerTag
        val replacedXfp = (requireActivity() as AddAirgapSignerActivity).replacedXfp.orEmpty()
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                val remainTime by viewModel.remainTime.collectAsStateWithLifecycle()
                AirgapActionQRIntroScreen(
                    remainTime = remainTime,
                    isMembershipFlow = isMembershipFlow,
                    onChainAddSignerParam = onChainAddSignerParam,
                    isReplaceKey = replacedXfp.isNotEmpty(),
                    onMoreClicked = ::handleShowMore,
                    onOpenGuideClicked = {
                        requireActivity().openExternalLink(JADE_GUIDE_URL)
                    }
                ) {
                    findNavController().navigate(AirgapActionQRIntroFragmentDirections.actionAirgapQRIntroFragmentToAddAirgapSignerFragment())
                }
            }
        }
    }
}


@Composable
internal fun AirgapActionQRIntroScreen(
    remainTime: Int = 0,
    isMembershipFlow: Boolean = true,
    onChainAddSignerParam: OnChainAddSignerParam? = null,
    isReplaceKey: Boolean = false,
    onMoreClicked: () -> Unit = {},
    onOpenGuideClicked: () -> Unit = {},
    onContinueClicked: () -> Unit = {},
) {
    NunchukTheme {
        Scaffold(topBar = {
            NcImageAppBar(
                backgroundRes = R.drawable.bg_airgap_jade_intro,
                title = if (isMembershipFlow && !isReplaceKey) stringResource(
                    id = R.string.nc_estimate_remain_time,
                    remainTime
                ) else "",
                actions = {
                    if (isMembershipFlow && !isReplaceKey) {
                        IconButton(onClick = onMoreClicked) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_more),
                                contentDescription = "More icon"
                            )
                        }
                    }
                }
            )
        }) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(innerPadding)
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
            ) {
                val keyIndex = if (onChainAddSignerParam != null && onChainAddSignerParam.keyIndex >= 0) onChainAddSignerParam.keyIndex else 0
                Text(
                    modifier = Modifier.padding(top = 24.dp, start = 16.dp, end = 16.dp),
                    text = stringResource(R.string.nc_add_jade),
                    style = NunchukTheme.typography.heading
                )
                Text(
                    modifier = Modifier.padding(16.dp),
                    text = stringResource(R.string.nc_ensure_to_following_jade),
                    style = NunchukTheme.typography.body
                )
                LabelNumberAndDesc(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                    index = 1,
                    title = stringResource(id = R.string.nc_init_jade),
                    titleStyle = NunchukTheme.typography.title,
                ) {
                    NcClickableText(
                        modifier = Modifier.padding(top = 8.dp, start = 36.dp),
                        messages = listOf(
                            ClickAbleText(content = stringResource(id = R.string.nc_refer_to)),
                            ClickAbleText(
                                content = stringResource(id = R.string.nc_this_starter_guide),
                                onOpenGuideClicked
                            )
                        ),
                        style = NunchukTheme.typography.body
                    )
                }
                LabelNumberAndDesc(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                    index = 2,
                    title = stringResource(id = R.string.nc_unlock_jade),
                    titleStyle = NunchukTheme.typography.title,
                ) {
                    Text(
                        modifier = Modifier.padding(top = 8.dp, start = 36.dp),
                        text = stringResource(id = R.string.nc_unlock_jade_desc),
                        style = NunchukTheme.typography.body
                    )
                }
                LabelNumberAndDesc(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                    index = 3,
                    title = stringResource(id = R.string.nc_export_xpub_jade, keyIndex),
                    titleStyle = NunchukTheme.typography.title,
                ) {
                    Text(
                        modifier = Modifier.padding(top = 8.dp, start = 36.dp),
                        text = stringResource(id = R.string.nc_export_xpub_jade_desc, keyIndex),
                        style = NunchukTheme.typography.body
                    )
                }
                LabelNumberAndDesc(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                    index = 4,
                    title = stringResource(id = R.string.nc_scan_qr_code),
                    titleStyle = NunchukTheme.typography.title,
                ) {
                    Text(
                        modifier = Modifier.padding(top = 8.dp, start = 36.dp),
                        text = stringResource(id = R.string.nc_scan_qr_code_desc),
                        style = NunchukTheme.typography.body
                    )
                }
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

@PreviewLightDark
@Composable
private fun AirgapActionQRIntroScreenPreview() {
    AirgapActionQRIntroScreen(
        isMembershipFlow = false,
        onChainAddSignerParam = OnChainAddSignerParam(keyIndex = 1)
    )
}