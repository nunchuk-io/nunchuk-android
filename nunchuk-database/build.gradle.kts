plugins {
    id("nunchuk.android.library")
}

android {
    namespace = "com.nunchuk.android.database"
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(project(":nunchuk-domain"))

    implementation(libs.bundles.androidx)
    debugImplementation(libs.nunchuk.nativesdk)
    releaseImplementation("com.github.nunchuk-io:nunchuk-android-nativesdk-prebuild:1.2.11@aar")
    // Room
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)
}
