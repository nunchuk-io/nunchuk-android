package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.sharesecret

import androidx.annotation.Keep
import com.nunchuk.android.main.R

sealed class InheritanceShareSecretEvent {
    data class ContinueClick(val type: Int) : InheritanceShareSecretEvent()
}

data class InheritanceShareSecretState(
    val options: List<InheritanceOption> = initOptions()
)

private fun initOptions(): List<InheritanceOption> {
    val options = mutableListOf<InheritanceOption>().apply {
        add(
            InheritanceOption(
                type = InheritanceShareSecretType.DIRECT.ordinal,
                title = R.string.nc_direct_inheritance,
                desc = R.string.nc_direct_inheritance_desc,
                isSelected = false
            )
        )
        add(
            InheritanceOption(
                type = InheritanceShareSecretType.INDIRECT.ordinal,
                title = R.string.nc_indirect_inheritance,
                desc = R.string.nc_indirect_inheritance_desc,
                isSelected = false
            )
        )
        add(
            InheritanceOption(
                type = InheritanceShareSecretType.JOINT_CONTROL.ordinal,
                title = R.string.nc_joint_control,
                desc = R.string.nc_joint_control_desc,
                isSelected = false
            )
        )
    }
    return options
}

data class InheritanceOption(val type: Int, val title: Int, val desc: Int, val isSelected: Boolean)

@Keep
enum class InheritanceShareSecretType {
    DIRECT, INDIRECT, JOINT_CONTROL
}