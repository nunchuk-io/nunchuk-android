package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.createsuccess

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.main.R
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.reviewplan.InheritanceReviewPlanFragmentArgs
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.reviewplan.InheritanceReviewPlanFragmentDirections
import com.nunchuk.android.nav.NunchukNavigator
import javax.inject.Inject

class InheritanceCreateSuccessFragment : Fragment() {

    @Inject
    lateinit var navigator: NunchukNavigator

    private val args: InheritanceCreateSuccessFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                InheritanceCreateSuccessScreenContent {
                    findNavController().navigate(InheritanceCreateSuccessFragmentDirections.actionInheritanceCreateSuccessFragmentToInheritanceShareSecretFragment(magicalPhrase = args.magicalPhrase))
                }
            }
        }
    }
}

@Composable
fun InheritanceCreateSuccessScreenContent(
    onContinueClick: () -> Unit = {},
) {
    NunchukTheme {
        Scaffold { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                NcTopAppBar(title = "")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.nc_green_stick),
                        contentDescription = ""
                    )
                }
                Text(
                    modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
                    text = stringResource(R.string.nc_inheritance_create_success),
                    style = NunchukTheme.typography.heading
                )

                Spacer(modifier = Modifier.weight(1.0f))

                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    onClick = onContinueClick,
                ) {
                    Text(text = stringResource(id = R.string.nc_text_continue))
                }
            }
        }
    }
}

@Preview
@Composable
private fun InheritanceCreateSuccessScreenContentPreview() {
    InheritanceCreateSuccessScreenContent()
}