package com.nunchuk.android.core.util

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource

/** Text resolved at the UI boundary so retained state follows the current UI locale. */
sealed interface UiText {
    data class Dynamic(val value: String) : UiText

    data class Resource(@param:StringRes val id: Int, val args: List<Any> = emptyList()) : UiText

    data class Plural(
        @param:PluralsRes val id: Int,
        val quantity: Int,
        val args: List<Any> = emptyList(),
    ) : UiText

    companion object {
        fun resource(@StringRes id: Int, vararg args: Any): UiText = Resource(id, args.toList())
    }
}

/** Call with the Activity/Fragment context that owns the UI, not applicationContext. */
fun UiText.resolve(context: Context): String = when (this) {
    is UiText.Dynamic -> value
    is UiText.Resource -> if (args.isEmpty()) context.getString(id)
        else context.getString(id, *args.toTypedArray())
    is UiText.Plural -> if (args.isEmpty()) context.resources.getQuantityString(id, quantity)
        else context.resources.getQuantityString(id, quantity, *args.toTypedArray())
}

@Composable
fun UiText.asString(): String = when (this) {
    is UiText.Dynamic -> value
    is UiText.Resource -> if (args.isEmpty()) stringResource(id)
        else stringResource(id, *args.toTypedArray())
    is UiText.Plural -> if (args.isEmpty()) pluralStringResource(id, quantity)
        else pluralStringResource(id, quantity, *args.toTypedArray())
}
