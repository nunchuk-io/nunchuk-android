package com.nunchuk.android.main.components.tabs.services.keyrecovery

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Keep
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.NcHighlightText
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.manager.ActivityManager
import com.nunchuk.android.main.R
import com.nunchuk.android.nav.NunchukNavigator
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class KeyRecoverySuccessStateFragment : Fragment() {

    @Inject
    lateinit var navigator: NunchukNavigator

    private val args: KeyRecoverySuccessStateFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                KeyRecoverySuccessStateScreen(args.type, onGotItClick = {
                    requireActivity().finish()
                })
            }
        }
    }
}

@Keep
enum class KeyRecoverySuccessState {
    KEY_RECOVERY_APPROVED,
    KEY_RECOVERY_REQUEST_SENT
}

@Composable
fun KeyRecoverySuccessStateScreen(
    type: String,
    onGotItClick: () -> Unit
) {
    KeyRecoverySuccessStateScreenContent(
        type = type,
        onGotItClick = onGotItClick
    )
}

@Composable
fun KeyRecoverySuccessStateScreenContent(
    type: String = "",
    onGotItClick: () -> Unit = {},
) {
    NunchukTheme {
        Scaffold { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                NcTopAppBar(title = "", elevation = 0.dp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_key_recovery_success_state),
                        contentDescription = ""
                    )
                }
                val title = when (type) {
                    KeyRecoverySuccessState.KEY_RECOVERY_APPROVED.name -> stringResource(R.string.nc_key_recovery_approved)
                    KeyRecoverySuccessState.KEY_RECOVERY_REQUEST_SENT.name -> stringResource(R.string.nc_key_recovery_request_sent)
                    else -> ""

                }
                val desc = when (type) {
                    KeyRecoverySuccessState.KEY_RECOVERY_APPROVED.name -> stringResource(R.string.nc_key_recovery_approved_desc)
                    KeyRecoverySuccessState.KEY_RECOVERY_REQUEST_SENT.name -> stringResource(R.string.nc_key_recovery_request_sent_desc)
                    else -> ""
                }
                Text(
                    modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
                    text = title,
                    style = NunchukTheme.typography.heading
                )
                NcHighlightText(
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
                    text = desc,
                    style = NunchukTheme.typography.body
                )

                Spacer(modifier = Modifier.weight(1.0f))

                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    onClick = onGotItClick,
                ) {
                    Text(text = stringResource(id = R.string.nc_text_got_it))
                }
            }
        }
    }
}

@Preview
@Composable
private fun KeyRecoverySuccessStateScreenContentPreview() {
    KeyRecoverySuccessStateScreenContent()
}