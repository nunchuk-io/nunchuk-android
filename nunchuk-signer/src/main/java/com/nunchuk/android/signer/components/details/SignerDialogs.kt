package com.nunchuk.android.signer.components.details

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.nunchuk.android.compose.dialog.NcConfirmationDialog
import com.nunchuk.android.compose.dialog.NcInfoDialog
import com.nunchuk.android.compose.dialog.NcInputDialog
import com.nunchuk.android.signer.R

internal class SignerDialogs {
    // Compose counterparts of NCInfoDialog / NCWarningDialog, which the old fragment used.
    private sealed interface Confirmation {
        val title: String?
        val message: String

        data class Info(
            override val title: String?,
            override val message: String,
            val btnYes: String?,
            val btnInfo: String?,
            val onYesClick: () -> Unit,
            val onInfoClick: () -> Unit,
        ) : Confirmation

        data class Warning(
            override val title: String?,
            override val message: String,
            val btnYes: String?,
            val btnNo: String?,
            val onYesClick: () -> Unit,
        ) : Confirmation
    }

    private data class Input(val title: String, val onConfirm: (String) -> Unit)

    private var confirmation by mutableStateOf<Confirmation?>(null)
    private var input by mutableStateOf<Input?>(null)

    fun info(
        message: String,
        title: String? = null,
        btnYes: String? = null,
        btnInfo: String? = null,
        onYesClick: () -> Unit = {},
        onInfoClick: () -> Unit = {},
    ) {
        confirmation = Confirmation.Info(title, message, btnYes, btnInfo, onYesClick, onInfoClick)
    }

    fun warning(
        message: String,
        title: String? = null,
        btnYes: String? = null,
        btnNo: String? = null,
        onYesClick: () -> Unit,
    ) {
        confirmation = Confirmation.Warning(title, message, btnYes, btnNo, onYesClick)
    }

    fun input(title: String, onConfirmed: (String) -> Unit) {
        input = Input(title, onConfirmed)
    }

    @Composable
    fun Content() {
        when (val dialog = confirmation) {
            is Confirmation.Info -> NcInfoDialog(
                title = dialog.title ?: stringResource(R.string.nc_text_info),
                message = dialog.message,
                positiveButtonText = dialog.btnYes ?: stringResource(R.string.nc_text_got_it),
                negativeButtonText = dialog.btnInfo,
                onPositiveClick = {
                    confirmation = null
                    dialog.onYesClick()
                },
                onNegativeClick = {
                    confirmation = null
                    dialog.onInfoClick()
                },
                // Not cancelable, like NCInfoDialog: only the buttons close it.
                onDismiss = {},
            )

            is Confirmation.Warning -> NcConfirmationDialog(
                title = dialog.title ?: stringResource(R.string.nc_text_warning),
                message = dialog.message,
                positiveButtonText = dialog.btnYes ?: stringResource(R.string.nc_text_yes),
                negativeButtonText = dialog.btnNo ?: stringResource(R.string.nc_text_no),
                onPositiveClick = {
                    confirmation = null
                    dialog.onYesClick()
                },
                onDismiss = { confirmation = null },
            )

            null -> Unit
        }
        input?.let { dialog ->
            NcInputDialog(
                title = dialog.title,
                onConfirmed = { value -> input = null; dialog.onConfirm(value) },
                onCanceled = { input = null },
                onDismiss = { input = null },
            )
        }
    }
}
