package com.nunchuk.android.model

data class DefaultPermissions(
    val permissions: Map<String, List<Data>>,
) {
    data class Data(
        val alternativeNames: HashMap<String, String>,
        val hidden: Boolean,
        val name: String,
        val slug: String,
    )
}