pluginManagement {
    includeBuild("build-logic")
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://maven.google.com") }
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots/") }
        mavenLocal()
    }
}

include(":nunchuk-app")
include(":nunchuk-auth")
include(":nunchuk-arch")
include(":nunchuk-core")
include(":nunchuk-database")
include(":nunchuk-main")
include(":nunchuk-domain")
include(":nunchuk-network")
include(":nunchuk-widget")
include(":nunchuk-signer")
include(":nunchuk-utils")
include(":nunchuk-transaction")
include(":nunchuk-messages")
include(":nunchuk-contact")
include(":nunchuk-wallet")
include(":nunchuk-wallet-personal")
include(":nunchuk-wallet-shared")
include(":nunchuk-wallet-core")
include(":nunchuk-signer-software")
include(":nunchuk-settings")
include(":nunchuk-notifications")
include(":baselineprofile")
