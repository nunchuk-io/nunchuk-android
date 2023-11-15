package com.nunchuk.android.core.data.model.membership

import com.google.gson.annotations.SerializedName

data class PermissionResponse(
    @SerializedName("default_permissions")
    val defaultPermissions: HashMap<String, List<Data>>? = null
) {
    data class Data(
        @SerializedName("alternative_names")
        val alternativeNames: HashMap<String, String>? = null,
        @SerializedName("hidden")
        val hidden: Boolean? = null,
        @SerializedName("name")
        val name: String? = null,
        @SerializedName("slug")
        val slug: String? = null,
    )
}