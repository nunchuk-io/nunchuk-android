package com.nunchuk.android.signer.util

fun isTestNetPath(path: String): Boolean {
    return path.split("/").getOrNull(2) == "1h"
}