package com.nunchuk.android.signer.mk4.inheritance.backup.encrypted

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.fragment.compose.content
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.mk4.inheritance.ColdCardAction
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ColdCardImportEncryptedBackUpFragment : MembershipFragment() {

    @Inject
    lateinit var navigator: NunchukNavigator

    private val viewModel: ColdCardImportEncryptedBackUpViewModel by viewModels()

    private val importFileLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                viewModel.getBackUpFilePath(it)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = content {
        val remainTime by membershipStepManager.remainingTime.collectAsStateWithLifecycle()
        ColdCardImportEncryptedBackUpScreen(remainTime) {
            when (it) {
                ColdCardAction.FILE -> {
                    importFileLauncher.launch("*/*")
                }

                ColdCardAction.NFC -> {

                }

                else -> {
                    // do nothing
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        flowObserver(viewModel.event) { event ->
            when (event) {
                is ColdCardImportEncryptedBackUpEvent.Success -> {
                    findNavController().navigate(
                        ColdCardImportEncryptedBackUpFragmentDirections.actionColdCardImportEncryptedBackUpFragmentToColdCardBackingUpFragment(filePath = event.filePath)
                    )
                }
            }
        }
    }
}


@Composable
internal fun ColdCardImportEncryptedBackUpScreen(
    remainTime: Int = 0,
    onColdCardAction: (ColdCardAction) -> Unit = {}
) {
    NunchukTheme {
        Scaffold(modifier = Modifier.navigationBarsPadding(), topBar = {
            NcImageAppBar(
                backgroundRes = R.drawable.bg_uploading_backup_illustration,
                title = stringResource(
                    id = R.string.nc_estimate_remain_time,
                    remainTime
                )
            )
        }) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(innerPadding)
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    modifier = Modifier.padding(top = 24.dp, start = 16.dp, end = 16.dp),
                    text = "Import encrypted backup",
                    style = NunchukTheme.typography.heading
                )
                Text(
                    modifier = Modifier.padding(16.dp),
                    text = "Next, please import the encrypted backup file.",
                    style = NunchukTheme.typography.body
                )

                ActionItem(
                    title = "Import via file",
                    iconId = R.drawable.ic_import,
                    onClick = { onColdCardAction(ColdCardAction.FILE) }
                )

            }
        }
    }
}

@Composable
private fun ActionItem(
    title: String,
    @DrawableRes iconId: Int,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = iconId),
            contentDescription = "",
            modifier = Modifier.size(24.dp)
        )

        Text(
            modifier = Modifier
                .padding(start = 8.dp)
                .weight(1f),
            text = title,
            style = NunchukTheme.typography.body
        )

        Icon(
            painter = painterResource(id = R.drawable.ic_arrow),
            contentDescription = "",
        )
    }
}

@PreviewLightDark
@Composable
private fun ColdCardImportEncryptedBackUpScreenPreview() {
    ColdCardImportEncryptedBackUpScreen()
}