package com.nunchuk.android.wallet.personal.components

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResult
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.base.BaseComposeBottomSheet
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.parcelize.Parcelize

@AndroidEntryPoint
class UnassistedWalletTypeBottomSheet : BaseComposeBottomSheet() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                NunchukTheme {
                    UnassistedWalletTypeBottomSheetContent(
                        onWalletTypeSelected = { walletType ->
                            setFragmentResult(
                                TAG, Bundle().apply {
                                    putParcelable(
                                        RESULT,
                                        Result(walletType = walletType)
                                    )
                                }
                            )
                            dismissAllowingStateLoss()
                        }
                    )
                }
            }
        }
    }

    companion object {
        const val TAG = "UnassistedWalletTypeBottomSheet"
        const val RESULT = "UnassistedWalletTypeBottomSheetResult"

        fun show(
            fragmentManager: FragmentManager,
        ) {
            UnassistedWalletTypeBottomSheet().show(fragmentManager, TAG)
        }
    }

    @Parcelize
    data class Result(
        val walletType: WalletType,
    ) : Parcelable
}

@Composable
@Preview
fun UnassistedWalletTypeBottomSheetContent(
    onWalletTypeSelected: (WalletType) -> Unit = {},
) {
    Column(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
            )
            .padding(vertical = 24.dp)
            .nestedScroll(rememberNestedScrollInteropConnection())
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 16.dp),
            text = "Select unassisted wallet type:",
            style = NunchukTheme.typography.title,
        )

        FreeUserWalletTypeContent(onWalletTypeSelected = onWalletTypeSelected)
    }
}