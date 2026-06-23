plugins {
    id("nunchuk.android.library")
    id("nunchuk.android.nativesdk")
    alias(libs.plugins.hilt)
    alias(libs.plugins.androidx.navigation.safeargs)
    alias(libs.plugins.kotlin.parcelize)
}

android {
    namespace = "com.nunchuk.android.main"

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(project(":nunchuk-arch"))
    implementation(project(":nunchuk-core"))
    implementation(project(":nunchuk-contact"))
    implementation(project(":nunchuk-domain"))
    implementation(project(":nunchuk-wallet"))
    implementation(project(":nunchuk-wallet-core"))
    implementation(project(":nunchuk-signer"))
    implementation(project(":nunchuk-widget"))
    implementation(project(":nunchuk-messages"))
    implementation(project(":nunchuk-settings"))
    implementation(project(":nunchuk-notifications"))
    implementation(project(":nunchuk-database"))
    implementation(project(":nunchuk-network"))
    implementation(project(":nunchuk-auth"))
    implementation(libs.matrix.android.sdk2)

    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)

    implementation(libs.bundles.androidx)
    implementation(libs.bundles.lifecycle)
    implementation(libs.bundles.network)

    implementation(project(":nunchuk-transaction"))

    implementation(libs.landscapist.glide)

    implementation(libs.bundles.zxing)

    testImplementation(libs.junit)

    implementation(libs.lottie.compose)
    implementation(libs.reorderable)
    implementation(libs.kotlinx.serialization.json)
}
