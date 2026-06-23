plugins {
    id("nunchuk.android.library")
    id("nunchuk.android.nativesdk")
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
    // Room
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)
}
