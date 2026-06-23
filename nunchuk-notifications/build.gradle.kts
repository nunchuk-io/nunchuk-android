plugins {
    id("nunchuk.android.library")
    id("nunchuk.android.nativesdk")
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.nunchuk.android.notifications"
}

dependencies {
    implementation(project(":nunchuk-arch"))
    implementation(project(":nunchuk-core"))
    implementation(project(":nunchuk-widget"))
    implementation(project(":nunchuk-domain"))
    implementation(project(":nunchuk-network"))

    implementation(libs.bundles.androidx)
    implementation(libs.bundles.lifecycle)
    implementation(libs.bundles.network)
    implementation(libs.matrix.android.sdk2)
    implementation(platform(libs.firebase.bom))

    implementation(libs.hilt.android)

    implementation(libs.play.services.base)
    implementation(libs.firebase.messaging.ktx)

    testImplementation(libs.junit)
}
