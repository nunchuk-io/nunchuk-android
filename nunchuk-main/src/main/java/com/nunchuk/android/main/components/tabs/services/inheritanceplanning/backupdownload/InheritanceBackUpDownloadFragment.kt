package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.backupdownload

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.findNavController
import com.nunchuk.android.compose.NCLabelWithIndex
import com.nunchuk.android.compose.NcHighlightText
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.main.R
import com.nunchuk.android.share.membership.MembershipFragment

class InheritanceBackUpDownloadFragment : MembershipFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                InheritanceBackUpDownloadContent() {
                    findNavController().popBackStack()
                }
            }
        }
    }
}

@Composable
private fun InheritanceBackUpDownloadContent(
    onContinueClicked: () -> Unit = {}
) {
    NunchukTheme {
        Scaffold(
            topBar = {
                NcTopAppBar(
                    title = ""
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    modifier = Modifier.padding(top = 24.dp),
                    text = "The Backup Password",
                    style = NunchukTheme.typography.heading
                )
                NcHighlightText(
                    modifier = Modifier.padding(top = 16.dp),
                    text = "The Backup Password for the encrypted backup, which can be found on the device designated for inheritance.",
                    style = NunchukTheme.typography.body
                )
                NCLabelWithIndex(
                    modifier = Modifier.padding(top = 16.dp),
                    index = 1,
                    title = "TAPSIGNER",
                    label = "The Backup Password can be found on the back of the TAPSIGNER designated for inheritance. (It might be listed under “Backup key”).",
                )
                NCLabelWithIndex(
                    modifier = Modifier.padding(top = 16.dp),
                    index = 2,
                    title = "COLDCARD",
                    label = "The Backup Password (12 words) that was displayed on the COLDCARD device when you created the backup file.",
                )
                Spacer(modifier = Modifier.weight(1.0f))
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    onClick = onContinueClicked,
                ) {
                    Text(text = stringResource(id = R.string.nc_text_got_it))
                }
            }
        }
    }
}

@Preview
@Composable
private fun InheritanceBackUpDownloadScreenPreview() {
    InheritanceBackUpDownloadContent(

    )
}