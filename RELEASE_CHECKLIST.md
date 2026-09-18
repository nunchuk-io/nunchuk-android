# Release checklist

## Bumping the native SDK

Release builds do **not** compile the crypto layer from source — every BIP32/39, secp256k1, PSBT
and signing operation ships as bytes from a prebuilt AAR that JitPack serves:

```
com.github.nunchuk-io:nunchuk-android-nativesdk-prebuild:<prebuildNativeSdk>@aar
```

`prebuildNativeSdk` in `gradle/libs.versions.toml` is therefore pinned to a **commit**, not a tag.
A git tag is mutable; nothing else in the build records a checksum, and JitPack's `.sha1` sidecar
comes from the same origin as the AAR, so a moved tag would go unnoticed. A commit hash cannot be
re-pointed.

When bumping to a new native SDK version:

**1. Resolve the tag to its commit.**

```bash
curl -s https://api.github.com/repos/nunchuk-io/nunchuk-android-nativesdk-prebuild/git/ref/tags/<tag> \
  | grep '"sha"'
```

**2. Update both keys in `gradle/libs.versions.toml`,** keeping the tag in the trailing comment so
the version stays readable:

```toml
nativeSdk = "<tag>"
prebuildNativeSdk = "<commit sha>" # tag <tag>
```

`nativeSdk` is the mavenLocal artifact used by debug builds (built from source per developer);
`prebuildNativeSdk` is the release-only prebuilt AAR. They must refer to the same SDK version.

**3. Build a release variant** and confirm the new coordinate resolves:

```bash
./gradlew :nunchuk-app:assembleProductionRelease
```

If JitPack has not built that commit before it will build it on demand; a resolution failure here
means the commit is wrong or the build failed upstream, not that the pin is broken.

## LibPortal

```
com.github.Nunchuk1:LibPortal:97c9ef136ba17e65c773a9fc915cbd89cebccf84@aar  # v5
```

Declared inline in `nunchuk-core/build.gradle.kts` and `nunchuk-signer/build.gradle.kts`, pinned
the same way and for a stronger reason: it is served from **`Nunchuk1`, a personal GitHub account
outside the `nunchuk-io` org**, and it sits on the signer path — it handles wallet configuration
and seed material exported to and imported from the Portal NFC device.

This one is not expected to change between releases. If you do bump it, resolve the new tag the
same way:

```bash
curl -s https://api.github.com/repos/Nunchuk1/LibPortal/git/ref/tags/<tag> | grep '"sha"'
```

Both files must be updated together, or the two modules will pull different builds of the same
library.

## Known checksums

Recorded when the pins were introduced, in case a pin ever has to be reconstructed or an artifact
audited after the fact. Verifying these is not part of the normal release flow — the commit pins
above are what enforce integrity.

| Artifact | sha256 |
|---|---|
| `nunchuk-android-nativesdk-prebuild` @ `f084d1e3…` (tag 1.2.20) | `6c6a2ceb83375fc3bd8934b2c072c780cadd1e968c2b0c39b0e537ce9364d892` |
| `LibPortal` @ `97c9ef13…` (tag v5) | `cd4be0277dae2a24e120285aae763f0d6954cae172901005611cdef99bcc423e` |

```bash
shasum -a 256 ~/.gradle/caches/modules-2/files-2.1/com.github.nunchuk-io/nunchuk-android-nativesdk-prebuild/*/*/*.aar
shasum -a 256 ~/.gradle/caches/modules-2/files-2.1/com.github.Nunchuk1/LibPortal/*/*/*.aar
```

## What the pins do not cover

A commit pin stops a tag from being moved. It does **not** stop JitPack from serving different
bytes for the same commit — JitPack builds and hosts the artifact, and no checksum is enforced at
build time.

Closing that would take Gradle dependency verification
(`gradle/verification-metadata.xml`), which is all-or-nothing: it verifies every artifact in the
build, has to be regenerated on every dependency change, and needs a `<trusted-artifacts>`
exemption for `io.nunchuk.android:nativesdk`, which comes from mavenLocal and so has a different
checksum on every developer's machine. That cost was judged higher than the risk it removes.

The durable fix is to build the native SDK from source in the `reproducible-builds/` pipeline,
which would take JitPack out of the trust chain entirely and make the reproducible-build claim
actually cover the crypto layer.
