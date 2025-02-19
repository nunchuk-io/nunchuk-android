package com.nunchuk.android.signer.mk4.inheritance.backup.encrypted

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.fragment.compose.content
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.controlTextPrimary
import com.nunchuk.android.core.util.openExternalLink
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.signer.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ColdCardImportEncryptedBackUpDesktopFragment : MembershipFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = content {
        val remainTime by membershipStepManager.remainingTime.collectAsStateWithLifecycle()
        ColdCardImportEncryptedBackUpDesktopScreen(remainTime, onDownloadDesktopApp = {
            requireContext().openExternalLink("https://github.com/nunchuk-io/nunchuk-desktop/releases")
        }, onContinue = {
            requireActivity().finish()
        })
    }
}


@Composable
internal fun ColdCardImportEncryptedBackUpDesktopScreen(
    remainTime: Int = 0,
    onDownloadDesktopApp: () -> Unit = {},
    onContinue: () -> Unit = {}
) {
    NunchukTheme {
        Scaffold(modifier = Modifier.navigationBarsPadding(), topBar = {
            NcImageAppBar(
                backgroundRes = R.drawable.bg_coldcard_desktop_illustration,
                title = stringResource(
                    id = R.string.nc_estimate_remain_time,
                    remainTime
                )
            )
        }, bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth(),
                    onClick = onContinue
                ) {
                    Text(
                        text = stringResource(id = R.string.nc_text_got_it),
                        style = NunchukTheme.typography.title.copy(color = MaterialTheme.colorScheme.controlTextPrimary)
                    )
                }
            }

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
                    text = "Import encrypted backup file via Desktop app",
                    style = NunchukTheme.typography.heading
                )
                val annotatedText = buildAnnotatedString {
                    append("If you haven’t installed the Desktop app yet, please ")
                    pushStringAnnotation(tag = "DOWNLOAD_HERE", annotation = "download_here")
                    withStyle(
                        style = SpanStyle(
                            textDecoration = TextDecoration.Underline
                        )
                    ) {
                        append("download it here.")
                    }
                    append("\n\nOnce the app is installed,, please follow the instructions below to import the encrypted backup file on your Desktop:")
                    pop()
                }
                Text(
                    modifier = Modifier
                        .padding(16.dp)
                        .clickable {
                            val annotations = annotatedText.getStringAnnotations(
                                tag = "DOWNLOAD_HERE",
                                start = 0,
                                end = annotatedText.length
                            )
                            if (annotations.isNotEmpty()) {
                                onDownloadDesktopApp()
                            }
                        },
                    text = annotatedText,
                    style = NunchukTheme.typography.body
                )
                Box(
                    modifier = Modifier
                        .padding(top = 8.dp, start = 16.dp, end = 16.dp)
                        .background(
                            color = colorResource(R.color.nc_bg_mid_gray),
                            shape = RoundedCornerShape(12.dp)
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = "Navigate to Home → Wallet → Dashboard → Alerts → View",
                            style = NunchukTheme.typography.body,
                            textAlign = TextAlign.Start
                        )
                    }
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun ColdCardImportEncryptedBackUpDesktopScreenPreview() {
    ColdCardImportEncryptedBackUpDesktopScreen()
}