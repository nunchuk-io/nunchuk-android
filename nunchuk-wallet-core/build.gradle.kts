plugins {
    id("nunchuk.android.library")
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.parcelize)
}

android {
    namespace = "com.nunchuk.android.wallet.core"
}

dependencies {
    implementation(project(":nunchuk-arch"))
    implementation(project(":nunchuk-core"))
    implementation(project(":nunchuk-domain"))
    implementation(project(":nunchuk-widget"))

    debugImplementation(libs.nunchuk.nativesdk)
    releaseImplementation("com.github.nunchuk-io:nunchuk-android-nativesdk-prebuild:1.2.11@aar")

    implementation(libs.bundles.androidx)
    implementation(libs.bundles.lifecycle)

    implementation(libs.bundles.zxing)

    testImplementation(libs.junit)
}
