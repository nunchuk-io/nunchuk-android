package com.nunchuk.android.main.membership.onchaintimelock.importantpassphrase

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.sheet.BottomSheetOptionListener
import com.nunchuk.android.core.signer.OnChainAddSignerParam
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.MembershipActivity
import com.nunchuk.android.main.membership.MembershipViewModel
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.share.result.GlobalResultKey
import com.nunchuk.android.utils.parcelableArrayList
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ImportantNoticePassphraseFragment : MembershipFragment(), BottomSheetOptionListener {
    private val viewModel: ImportantNoticePassphraseViewModel by viewModels()
    private val membershipViewModel: MembershipViewModel by activityViewModels()

    private val signerIntroLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val filteredSigners = result.data?.parcelableArrayList<SignerModel>(GlobalResultKey.EXTRA_SIGNERS)
                if (!filteredSigners.isNullOrEmpty()) {
                    // Pass the result back to OnChainTimelockAddKeyListFragment via fragment result
                    val bundle = Bundle().apply {
                        putParcelableArrayList(GlobalResultKey.EXTRA_SIGNERS, ArrayList(filteredSigners))
                    }
                    setFragmentResult(REQUEST_KEY, bundle)
                    // Navigate back to OnChainTimelockAddKeyListFragment directly
                    findNavController().popBackStack(R.id.onChainTimelockAddKeyListFragment, false)
                } else {
                    findNavController().popBackStack(R.id.onChainTimelockAddKeyListFragment, false)
                }
            }
        }

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

    private fun handleContinueClick() {
        val activity = requireActivity() as MembershipActivity
        navigator.openSignerIntroScreenForResult(
            launcher = signerIntroLauncher,
            activityContext = activity,
            walletId = activity.walletId,
            groupId = activity.groupId,
            supportedSigners = membershipViewModel.getSupportedSigners(),
            onChainAddSignerParam = OnChainAddSignerParam(
                flags = OnChainAddSignerParam.FLAG_ADD_INHERITANCE_SIGNER,
                keyIndex = 0
            ),
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
                    text = "A passphrase is not recommended for the inheritance key, as it greatly complicates the setup and increases the risk of errors for both you and your Beneficiary.\n" +
                            "\n" +
                            "If you choose to use a passphrase, you must share it with your Beneficiary along with the seed phrase backup.\n" +
                            "\n" +
                            "Continue to add the inheritance key to the wallet on the next screen.",
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