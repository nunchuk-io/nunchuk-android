plugins {
    id("nunchuk.android.library")
}

android {
    namespace = "com.nunchuk.android.widget"
}

dependencies {
    api(project(":nunchuk-utils"))

    implementation(libs.bundles.zxing)
    implementation(libs.bundles.androidx)
    implementation(libs.timber)
}
