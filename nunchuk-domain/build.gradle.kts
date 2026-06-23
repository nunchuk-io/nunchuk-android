plugins {
    id("nunchuk.android.library")
    id("nunchuk.android.nativesdk")
    alias(libs.plugins.kotlin.parcelize)
}

android {
    namespace = "com.nunchuk.android.nativelib"
}

dependencies {
    implementation(project(":nunchuk-utils"))

    api(libs.hilt.android)

    implementation(libs.bundles.network)
}
