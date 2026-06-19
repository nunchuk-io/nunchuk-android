plugins {
    id("nunchuk.android.library")
}

android {
    namespace = "com.nunchuk.android.core.network"

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(libs.bundles.network)
}
