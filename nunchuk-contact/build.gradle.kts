plugins {
    id("nunchuk.android.library")
    id("nunchuk.android.nativesdk")
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.parcelize)
}

android {
    namespace = "com.nunchuk.android.contact"
}

dependencies {
    implementation(project(":nunchuk-arch"))
    implementation(project(":nunchuk-core"))
    implementation(project(":nunchuk-domain"))
    implementation(project(":nunchuk-widget"))
    implementation(project(":nunchuk-network"))
    implementation(project(":nunchuk-database"))

    implementation(libs.flexbox)

    implementation(libs.bundles.androidx)
    implementation(libs.bundles.lifecycle)
    implementation(libs.bundles.network)

    implementation(libs.matrix.android.sdk2)
    implementation(libs.bundles.workmanager)

    testImplementation(libs.junit)
}
