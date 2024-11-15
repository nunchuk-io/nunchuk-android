FROM docker.io/debian:stable-20240722-slim

RUN set -ex; \
    apt-get update; \
    DEBIAN_FRONTEND=noninteractive apt-get install --yes -o APT::Install-Suggests=false --no-install-recommends \
        bzip2 make automake ninja-build g++-multilib libtool binutils-gold \
        bsdmainutils pkg-config python3 patch bison curl unzip git openjdk-17-jdk sudo nano; \
    rm -rf /var/lib/apt/lists/*; \
    useradd -ms /bin/bash appuser; \
    usermod -aG sudo appuser; \
    echo "appuser ALL=(ALL) NOPASSWD:ALL" >> /etc/sudoers

USER appuser
WORKDIR /home/appuser
RUN mkdir -p /home/appuser/app
ENV ANDROID_SDK_ROOT=/home/appuser/app/sdk
ENV ANDROID_SDK=/home/appuser/app/sdk
ENV ANDROID_HOME=/home/appuser/app/sdk
ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
WORKDIR /home/appuser/app/nunchuk

ENV ANDROID_SDK_URL https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
ENV ANDROID_BUILD_TOOLS_VERSION 34.0.0
ENV ANDROID_VERSION 34
ENV ANDROID_NDK_VERSION 25.1.8937393
ENV ANDROID_CMAKE_VERSION 3.18.1
ENV ANDROID_NDK_HOME ${ANDROID_HOME}/ndk/${ANDROID_NDK_VERSION}/
ENV PATH ${PATH}:${ANDROID_HOME}/tools:${ANDROID_HOME}/platform-tools

RUN set -ex; \
    mkdir -p "$ANDROID_HOME/cmdline-tools" && \
    cd "$ANDROID_HOME/cmdline-tools" && \
    curl -o sdk.zip $ANDROID_SDK_URL && \
    unzip sdk.zip && \
    mv cmdline-tools latest && \
    rm sdk.zip

RUN chmod +x ${ANDROID_HOME}/cmdline-tools/latest/bin/sdkmanager

RUN ls -l ${ANDROID_HOME}/cmdline-tools/latest/bin/

RUN yes | ${ANDROID_HOME}/cmdline-tools/latest/bin/sdkmanager --sdk_root=$ANDROID_HOME --licenses
RUN ${ANDROID_HOME}/cmdline-tools/latest/bin/sdkmanager --sdk_root=$ANDROID_HOME --update

RUN $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --sdk_root=$ANDROID_HOME \
    "build-tools;${ANDROID_BUILD_TOOLS_VERSION}" \
    "platforms;android-${ANDROID_VERSION}" \
    "cmake;$ANDROID_CMAKE_VERSION" \
    "platform-tools" \
    "ndk;$ANDROID_NDK_VERSION"

ENV PATH ${ANDROID_NDK_HOME}:$PATH
ENV PATH ${ANDROID_NDK_HOME}/prebuilt/linux-x86_64/bin/:$PATH

ENV GRADLE_USER_HOME=/home/appuser/app/nunchuk/.gradle-home

