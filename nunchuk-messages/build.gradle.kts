plugins {
    id("nunchuk.android.library")
    id("nunchuk.android.nativesdk")
    alias(libs.plugins.hilt)
    alias(libs.plugins.androidx.navigation.safeargs)
    alias(libs.plugins.kotlin.parcelize)
}

android {
    namespace = "com.nunchuk.android.messages"
}

dependencies {
    implementation(project(":nunchuk-arch"))
    implementation(project(":nunchuk-core"))
    implementation(project(":nunchuk-domain"))
    implementation(project(":nunchuk-widget"))
    implementation(project(":nunchuk-network"))

    implementation(libs.flexbox)
    implementation(libs.photoview)
    implementation(libs.androidx.documentfile)

    implementation(libs.bundles.media3)

    implementation(libs.androidx.swiperefreshlayout)

    implementation(libs.bundles.androidx)
    implementation(libs.bundles.lifecycle)
    implementation(libs.bundles.network)

    implementation(libs.matrix.android.sdk2)
    implementation(libs.bundles.workmanager)

    testImplementation(libs.junit)
}
