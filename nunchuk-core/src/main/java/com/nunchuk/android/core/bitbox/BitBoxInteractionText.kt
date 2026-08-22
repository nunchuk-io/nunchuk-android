package com.nunchuk.android.core.bitbox

import android.content.Context
import com.nunchuk.android.core.R
import com.nunchuk.android.type.BitBoxUserInteraction

/**
 * Status copy for a `UserInteraction` the device is waiting on — the Confluence §0
 * "Recommended UI text" table.
 *
 * Shared by every BitBox host (the add-key flow and the sign / health-check sheet) so the same
 * device state doesn't get two different wordings. The controller already de-duplicates
 * repeats, so this only has to map.
 *
 * The setup and management interactions (recovery words, backups, firmware, factory reset) fall
 * through to the generic "communicating" line: they can only arise in BitBoxApp now, since that
 * part of the flow was dropped from the design.
 */
fun BitBoxUserInteraction.statusText(context: Context): String = when (this) {
    BitBoxUserInteraction.UNLOCK_DEVICE ->
        context.getString(R.string.nc_bitbox_unlock_device)

    BitBoxUserInteraction.CONFIRM_PAIRING ->
        context.getString(R.string.nc_bitbox_interaction_confirm_pairing)

    BitBoxUserInteraction.VERIFY_ADDRESS ->
        context.getString(R.string.nc_bitbox_interaction_verify_address)

    BitBoxUserInteraction.REGISTER_WALLET ->
        context.getString(R.string.nc_bitbox_interaction_register_wallet)

    BitBoxUserInteraction.SIGN_MESSAGE ->
        context.getString(R.string.nc_bitbox_interaction_sign_message)

    BitBoxUserInteraction.SIGN_TRANSACTION ->
        context.getString(R.string.nc_bitbox_interaction_sign_transaction)

    else -> context.getString(R.string.nc_bitbox_communicating)
}
