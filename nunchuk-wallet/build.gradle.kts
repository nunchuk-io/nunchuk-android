plugins {
    id("nunchuk.android.library")
    id("nunchuk.android.nativesdk")
    alias(libs.plugins.hilt)
    alias(libs.plugins.androidx.navigation.safeargs)
    alias(libs.plugins.kotlin.parcelize)
}

android {
    namespace = "com.nunchuk.android.wallet"
}

dependencies {
    implementation(project(":nunchuk-arch"))
    implementation(project(":nunchuk-core"))
    implementation(project(":nunchuk-domain"))
    implementation(project(":nunchuk-widget"))
    implementation(project(":nunchuk-auth"))
    implementation(project(":nunchuk-wallet-core"))
    implementation(project(":nunchuk-wallet-personal"))
    implementation(project(":nunchuk-wallet-shared"))
    implementation(libs.androidx.compose.runtime.livedata)


    implementation(libs.bundles.androidx)
    implementation(libs.bundles.lifecycle)

    implementation(libs.bundles.zxing)
    implementation(libs.bundles.paging)
    implementation(libs.matrix.android.sdk2)

    implementation(libs.revealswipe)

    testImplementation(libs.junit)
}
