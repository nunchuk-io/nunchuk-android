plugins {
    id("nunchuk.android.library")
    id("nunchuk.android.nativesdk")
    alias(libs.plugins.hilt)
    alias(libs.plugins.androidx.navigation.safeargs)
    alias(libs.plugins.kotlin.parcelize)
}

android {
    namespace = "com.nunchuk.android.wallet.personal"
}

dependencies {
    implementation(project(":nunchuk-arch"))
    implementation(project(":nunchuk-core"))
    implementation(project(":nunchuk-domain"))
    implementation(project(":nunchuk-widget"))
    implementation(project(":nunchuk-wallet-core"))


    implementation(libs.bundles.androidx)
    implementation(libs.bundles.lifecycle)

    implementation(libs.bundles.zxing)
    implementation(libs.bundles.googleScanner)

    testImplementation(libs.junit)
}
