package com.nunchuk.android.app.referral

fun simplifyAddress(address: String): String {
    runCatching {
        return address.substring(0, 5) + "..." + address.substring(
            address.length - 4,
            address.length
        )
    }
    return address
}