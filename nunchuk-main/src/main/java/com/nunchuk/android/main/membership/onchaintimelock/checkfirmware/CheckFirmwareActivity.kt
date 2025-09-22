package com.nunchuk.android.main.membership.onchaintimelock.checkfirmware

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.main.R
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.nav.args.CheckFirmwareArgs
import com.nunchuk.android.nav.args.SetupMk4Args
import com.nunchuk.android.share.result.GlobalResultKey
import com.nunchuk.android.type.SignerTag
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CheckFirmwareActivity : ComponentActivity() {

    @Inject
    lateinit var navigator: NunchukNavigator

    private val args: CheckFirmwareArgs by lazy {
        CheckFirmwareArgs.deserializeFrom(intent)
    }

    private val viewModel: CheckFirmwareViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.init(args)

        setContent {
            ColdCardCheckFirmwareScreen(
                viewModel = viewModel,
                onFilteredSignersReady = { signers ->
                    val resultIntent = Intent().apply {
                        putParcelableArrayListExtra(GlobalResultKey.EXTRA_SIGNERS, ArrayList(signers))
                    }
                    setResult(RESULT_OK, resultIntent)
                    finish()
                },
                onOpenNextScreen = {
                    openNextScreen()
                }
            )
        }
    }

    private fun openNextScreen() {
        when (args.signerTag) {
            SignerTag.COLDCARD -> {
                navigator.openSetupMk4(
                    activity = this,
                    args = SetupMk4Args(
                        fromMembershipFlow = args.onChainAddSignerParam != null,
                        isFromAddKey = true,
                        groupId = args.groupId,
                        walletId = args.walletId,
                        onChainAddSignerParam = args.onChainAddSignerParam,
                    )
                )
            }
            SignerTag.JADE -> {
                navigator.openAddAirSignerScreen(
                    activityContext = this,
                    isMembershipFlow = args.onChainAddSignerParam != null,
                    tag = SignerTag.JADE,
                    groupId = args.groupId,
                    walletId = args.walletId,
                )
            }
            else -> {
                // Handle other signer types if needed
            }
        }
        finish()
    }

    companion object {
        fun navigate(
            context: Context,
            launcher: ActivityResultLauncher<Intent>?,
            args: CheckFirmwareArgs
        ) {
            val intent = Intent(context, CheckFirmwareActivity::class.java).apply {
                    putExtras(args.buildBundle())
                }
            if (launcher == null) {
                context.startActivity(intent)
            } else {
                launcher.launch(intent)
            }
        }
    }
}

@Composable
private fun ColdCardCheckFirmwareScreen(
    viewModel: CheckFirmwareViewModel = viewModel(),
    onMoreClicked: () -> Unit = {},
    onFilteredSignersReady: (List<SignerModel>) -> Unit = {},
    onOpenNextScreen: () -> Unit = {}
) {
    val remainTime by viewModel.remainTime.collectAsStateWithLifecycle()
    
    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                is CheckFirmwareEvent.ShowFilteredSigners -> {
                    onFilteredSignersReady(event.signers)
                }
                CheckFirmwareEvent.OpenNextScreen -> {
                    onOpenNextScreen()
                }
            }
        }
    }
    
    ColdCardCheckFirmwareContent(
        onMoreClicked = onMoreClicked,
        remainTime = remainTime,
        onContinueClicked = viewModel::onContinueClicked
    )
}

@Composable
private fun ColdCardCheckFirmwareContent(
    remainTime: Int = 0,
    onMoreClicked: () -> Unit = {},
    onContinueClicked: () -> Unit = {},
) {
    NunchukTheme {
        Scaffold(topBar = {
            NcImageAppBar(
                backgroundRes = R.drawable.bg_check_coldcard_firmware_illustration,
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
                    text = "Check ColdCard firmware",
                    style = NunchukTheme.typography.heading
                )
                Text(
                    modifier = Modifier.padding(16.dp),
                    text = "Before adding your ColdCard as a signer, it's important to verify that your device is running the latest firmware version.\n" +
                            "\n" +
                            "Having the most up-to-date firmware ensures optimal security and compatibility with Nunchuk.\n" +
                            "\n" +
                            "Please check your ColdCard firmware version and update if necessary before proceeding.",
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
private fun ColdCardCheckFirmwareScreenPreview() {
    ColdCardCheckFirmwareContent(

    )
}

