plugins {
    id("nunchuk.android.library")
    alias(libs.plugins.kotlin.parcelize)
}

android {
    namespace = "com.nunchuk.android.nativelib"
}

dependencies {
    implementation(project(":nunchuk-utils"))
    debugImplementation(libs.nunchuk.nativesdk)
    releaseImplementation("com.github.nunchuk-io:nunchuk-android-nativesdk-prebuild:1.2.11@aar")

    api(libs.hilt.android)

    implementation(libs.bundles.network)
}
