plugins {
    id("nunchuk.android.library")
    alias(libs.plugins.hilt)
    alias(libs.plugins.androidx.navigation.safeargs)
}

android {
    namespace = "com.nunchuk.android.signer.software"
}

dependencies {
    implementation(project(":nunchuk-arch"))
    implementation(project(":nunchuk-core"))
    implementation(project(":nunchuk-utils"))
    implementation(project(":nunchuk-domain"))
    implementation(project(":nunchuk-widget"))
    implementation(project(":nunchuk-network"))
    debugImplementation(libs.nunchuk.nativesdk)
    releaseImplementation("com.github.nunchuk-io:nunchuk-android-nativesdk-prebuild:1.2.11@aar")

    implementation(libs.bundles.androidx)
    implementation(libs.bundles.lifecycle)
    implementation(libs.bundles.network)

    testImplementation(libs.junit)
}
