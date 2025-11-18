package com.nunchuk.android.main.membership.onchaintimelock.importantpassphrase

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.clearFragmentResult
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.sheet.BottomSheetOptionListener
import com.nunchuk.android.core.signer.OnChainAddSignerParam
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.MembershipActivity
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.share.result.GlobalResultKey
import com.nunchuk.android.utils.parcelableArrayList
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ImportantNoticePassphraseFragment : MembershipFragment(), BottomSheetOptionListener {
    private val viewModel: ImportantNoticePassphraseViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                ImportantNoticePassphraseScreen(
                    viewModel = viewModel,
                    onContinue = ::handleContinueClick
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setFragmentResultListener("SignerIntroFragment") { _, bundle ->
            val filteredSigners = bundle.parcelableArrayList<SignerModel>(GlobalResultKey.EXTRA_SIGNERS)
            val activity = requireActivity() as MembershipActivity
            val destinationId = if (activity.groupId.isNotEmpty()) {
                R.id.onChainTimelockByzantineAddKeyFragment
            } else {
                R.id.onChainTimelockAddKeyListFragment
            }
            
            if (!filteredSigners.isNullOrEmpty()) {
                val resultBundle = Bundle().apply {
                    putParcelableArrayList(GlobalResultKey.EXTRA_SIGNERS, ArrayList(filteredSigners))
                }
                setFragmentResult(REQUEST_KEY, resultBundle)
                findNavController().popBackStack(destinationId, false)
            } else {
                findNavController().popBackStack(destinationId, false)
            }
            clearFragmentResult("SignerIntroFragment")
        }
    }

    private fun handleContinueClick() {
        val activity = requireActivity() as MembershipActivity
        findNavController().navigate(
            ImportantNoticePassphraseFragmentDirections.actionImportantNoticePassphraseFragmentToSignerIntroFragment(
                walletId = activity.walletId,
                groupId = activity.groupId,
                supportedSigners = null,
                keyFlow = 0,
                onChainAddSignerParam = OnChainAddSignerParam(
                    flags = OnChainAddSignerParam.FLAG_ADD_INHERITANCE_SIGNER,
                    keyIndex = 0
                )
            )
        )
    }

    companion object {
        const val REQUEST_KEY = "ImportantNoticePassphraseFragment"
    }
}

@Composable
private fun ImportantNoticePassphraseScreen(
    viewModel: ImportantNoticePassphraseViewModel = viewModel(),
    onMoreClicked: () -> Unit = {},
    onContinue: () -> Unit = {},
) {
    val remainTime by viewModel.remainTime.collectAsStateWithLifecycle()
    ImportantNoticePassphraseContent(
        onMoreClicked = onMoreClicked,
        onContinueClicked = onContinue,
        remainTime = remainTime
    )
}

@Composable
private fun ImportantNoticePassphraseContent(
    remainTime: Int = 0,
    onMoreClicked: () -> Unit = {},
    onContinueClicked: () -> Unit = {},
) {
    NunchukTheme {
        Scaffold(topBar = {
            NcImageAppBar(
                backgroundRes = R.drawable.bg_passphrase_notice_illustration,
                title = stringResource(
                    id = R.string.nc_estimate_remain_time,
                    remainTime
                ),
                actions = {
                    IconButton(onClick = onMoreClicked) {
                        Icon(
                            painter = painterResource(id = com.nunchuk.android.signer.R.drawable.ic_more),
                            contentDescription = "More icon"
                        )
                    }
                }
            )
        }) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    modifier = Modifier.padding(top = 24.dp, start = 16.dp, end = 16.dp),
                    text = "Important notice about passphrase",
                    style = NunchukTheme.typography.heading
                )
                Text(
                    modifier = Modifier.padding(16.dp),
                    text = buildAnnotatedString {
                        append("A passphrase is an optional \"extra word\" added to your 12 or 24-word seed phrase.\n\n")
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append("We strongly advise against using a passphrase for the inheritance key.")
                        }
                        append(" It significantly complicates the recovery process and increases the risk of errors for your Beneficiary.\n\n")
                        append("If you still choose to use a passphrase, you must ensure it is shared with your Beneficiary along with the seed phrase backup.\n\n")
                        append("Continue to add the inheritance key to the wallet on the next screen.")
                    },
                    style = NunchukTheme.typography.body
                )
                Spacer(modifier = Modifier.weight(1.0f))
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    onClick = onContinueClicked,
                ) {
                    Text(text = stringResource(id = com.nunchuk.android.signer.R.string.nc_text_continue))
                }
            }
        }
    }
}

@Preview
@Composable
private fun ImportantNoticePassphraseScreenPreview() {
    ImportantNoticePassphraseContent(

    )
}