# Shared builder image for nunchuk-android and nunchuk-android-nativesdk.
# This file MUST stay byte-identical in both repos:
#   nunchuk-android/reproducible-builds/Dockerfile
#   nunchuk-android-nativesdk/Dockerfile
# The app's reproducible-build guide builds the native SDK in this same image, so
# any drift between the two copies breaks verification of the prebuilt .aar.
FROM docker.io/debian:trixie-20250811-slim

# python-is-python3 + python3-setuptools: libwally-core's ./configure (AX_PYTHON_DEVEL)
# requires a `python` binary with a working `distutils`. Debian trixie ships Python 3.13
# (distutils removed) and no `python` symlink; setuptools' vendored _distutils satisfies
# `import distutils`.
RUN set -ex; \
    apt-get update; \
    DEBIAN_FRONTEND=noninteractive apt-get install --yes -o APT::Install-Suggests=false --no-install-recommends \
        bzip2 make automake ninja-build g++-multilib libtool binutils-gold \
        bsdextrautils pkg-config python3 python-is-python3 python3-setuptools patch bison curl unzip git \
        openjdk-25-jdk disorderfs; \
    rm -rf /var/lib/apt/lists/*;

ENV JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64
ENV PATH=${JAVA_HOME}/bin:${PATH}
ENV ANDROID_SDK_ROOT=/sdk
ENV ANDROID_SDK=/sdk
ENV ANDROID_HOME=/sdk
ENV ANDROID_SDK_URL=https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
ENV ANDROID_SDK_SHA256=2d2d50857e4eb553af5a6dc3ad507a17adf43d115264b1afc116f95c92e5e258
ENV ANDROID_COMMAND_LINE_TOOLS_VERSION=12.0
ENV ANDROID_BUILD_TOOLS_VERSION=36.0.0
ENV ANDROID_PLATFORM_TOOLS_VERSION=36.0.0
ENV ANDROID_PLATFORM_TOOLS_SHA256=0ead642c943ffe79701fccca8f5f1c69c4ce4f43df2eefee553f6ccb27cbfbe8
ENV ANDROID_VERSION=36
ENV ANDROID_NDK_VERSION=27.2.12479018
ENV ANDROID_CMAKE_VERSION=3.22.1
ENV ANDROID_NDK_HOME=${ANDROID_HOME}/ndk/${ANDROID_NDK_VERSION}
ENV ANDROID_NDK_ROOT=${ANDROID_NDK_HOME}
ENV PATH=${ANDROID_HOME}/cmdline-tools/${ANDROID_COMMAND_LINE_TOOLS_VERSION}/bin:${ANDROID_HOME}/build-tools/${ANDROID_BUILD_TOOLS_VERSION}:${ANDROID_HOME}/platform-tools:${PATH}
ENV PATH=${ANDROID_NDK_HOME}:$PATH
ENV PATH=${ANDROID_NDK_HOME}/prebuilt/linux-x86_64/bin/:$PATH

RUN set -ex; \
    mkdir -p "$ANDROID_HOME/cmdline-tools" && \
    cd "$ANDROID_HOME" && \
    curl -fL --retry 3 -o sdk.zip "$ANDROID_SDK_URL" && \
    echo "$ANDROID_SDK_SHA256  sdk.zip" | sha256sum -c - && \
    unzip -q sdk.zip -d /tmp/android-tools && \
    mv /tmp/android-tools/cmdline-tools "cmdline-tools/$ANDROID_COMMAND_LINE_TOOLS_VERSION" && \
    rmdir /tmp/android-tools && \
    rm sdk.zip

RUN yes | sdkmanager --sdk_root=$ANDROID_HOME --licenses
# Do not run sdkmanager --update: it replaces the pinned command-line tools.
RUN sdkmanager --sdk_root=$ANDROID_HOME \
    "build-tools;${ANDROID_BUILD_TOOLS_VERSION}" \
    "platforms;android-${ANDROID_VERSION}" \
    "cmake;$ANDROID_CMAKE_VERSION" \
    "ndk;$ANDROID_NDK_VERSION"

RUN set -ex; \
    cd "$ANDROID_HOME"; \
    curl -fL --retry 3 -o platform-tools.zip \
        "https://dl.google.com/android/repository/platform-tools_r${ANDROID_PLATFORM_TOOLS_VERSION}-linux.zip"; \
    echo "$ANDROID_PLATFORM_TOOLS_SHA256  platform-tools.zip" | sha256sum -c -; \
    rm -rf platform-tools; \
    unzip -q platform-tools.zip; \
    rm platform-tools.zip

# Pinned tools for APK generation and comparison in this image.
ENV BUNDLETOOL_VERSION=1.18.3
ENV BUNDLETOOL_SHA256=a099cfa1543f55593bc2ed16a70a7c67fe54b1747bb7301f37fdfd6d91028e29
RUN set -ex; \
    mkdir -p /opt/bundletool; \
    curl -fL --retry 3 -o /opt/bundletool/bundletool.jar \
        "https://github.com/google/bundletool/releases/download/${BUNDLETOOL_VERSION}/bundletool-all-${BUNDLETOOL_VERSION}.jar"; \
    echo "$BUNDLETOOL_SHA256  /opt/bundletool/bundletool.jar" | sha256sum -c -; \
    printf '%s\n' '#!/bin/sh' 'exec java -jar /opt/bundletool/bundletool.jar "$@"' > /usr/local/bin/bundletool; \
    chmod 755 /usr/local/bin/bundletool; \
    bundletool version; \
    aapt2 version; \
    apkanalyzer --help > /dev/null; \
    adb version
