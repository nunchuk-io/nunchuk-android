set -e

if [ -z "$ANDROID_NDK_HOME" ]; then
  echo "export the ANDROID_NDK environment variable"
  exit 1
fi

export TOOLCHAIN=$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/darwin-x86_64

cd $TOOLCHAIN/bin/
for source in arm-linux-androideabi-*; do
  dest=${source/arm/armv7a}
  ln -sf $source $dest
done
cd -

export API=24

pwd=$(pwd)
echo "pwd::$pwd"

export ANDROID_ABI_ARMEABI_V7A="armeabi-v7a"
export ANDROID_ABI_ARM64_V8A="arm64-v8a"
export ANDROID_ABI_X86_64="x86_64"
export ANDROID_ABI_X86="x86"

export ANDROID_TARGET_ARMEABI_V7A="armv7a-linux-androideabi"
export ANDROID_TARGET_ARM64_V8A="aarch64-linux-android"
export ANDROID_TARGET_X86_64="x86_64-linux-android"
export ANDROID_TARGET_X86="i686-linux-android"

export ANDROID_ABI=$1
ANDROID_TARGET=""

parseArgs() {
  if [ "$ANDROID_ABI" == $ANDROID_ABI_ARMEABI_V7A ]; then
    ANDROID_TARGET=$ANDROID_TARGET_ARMEABI_V7A
  elif [ "$ANDROID_ABI" == $ANDROID_ABI_ARM64_V8A ]; then
    ANDROID_TARGET=$ANDROID_TARGET_ARM64_V8A
  elif [ "$ANDROID_ABI" == $ANDROID_ABI_X86_64 ]; then
    ANDROID_TARGET=$ANDROID_TARGET_X86_64
  elif [ "$ANDROID_ABI" == $ANDROID_ABI_X86 ]; then
    ANDROID_TARGET=$ANDROID_TARGET_X86
  else
    echo "Invalid ABI argument $ANDROID_ABI"
    exit 1
  fi
}
parseArgs
#########################################################################################
####                                Bitcoin Deps                                     ####
#########################################################################################
installBitcoinDeps() {
  abi=$ANDROID_ABI
  target=$ANDROID_TARGET

  echo "-------------------------------------------------------------------------------"
  echo "                     Installing deps for $abi $target                          "
  echo "-------------------------------------------------------------------------------"

  export TARGET=$target

  export AR=$TOOLCHAIN/bin/$TARGET-ar
  export AS=$TOOLCHAIN/bin/$TARGET-as
  export CC=$TOOLCHAIN/bin/$TARGET$API-clang
  export CXX=$TOOLCHAIN/bin/$TARGET$API-clang++
  export LD=$TOOLCHAIN/bin/$TARGET-ld
  export RANLIB=$TOOLCHAIN/bin/$TARGET-ranlib
  export STRIP=$TOOLCHAIN/bin/$TARGET-strip

  ANDROID_SDK=$ANDROID_SDK ANDROID_NDK=$ANDROID_NDK make HOST=$TARGET ANDROID_TOOLCHAIN_BIN=$TOOLCHAIN ANDROID_API_LEVEL=$API NO_QT=1 NO_ZMQ=1 NO_QR=1 NO_UPNP=1
}
#
pushd libnunchuk/contrib/bitcoin/depends || exit
installBitcoinDeps
popd || exit

#########################################################################################
####                                 Bitcoin Core                                    ####
#########################################################################################
installBitcoinCore() {
  abi=$ANDROID_ABI
  target=$ANDROID_TARGET
  echo "-------------------------------------------------------------------------------"
  echo "                        Installing core for $abi $target                       "
  echo "-------------------------------------------------------------------------------"

  export TARGET=$target

  export AR=$TOOLCHAIN/bin/$TARGET-ar
  export AS=$TOOLCHAIN/bin/$TARGET-as
  export CC=$TOOLCHAIN/bin/$TARGET$API-clang
  export CXX=$TOOLCHAIN/bin/$TARGET$API-clang++
  export LD=$TOOLCHAIN/bin/$TARGET-ld
  export RANLIB=$TOOLCHAIN/bin/$TARGET-ranlib
  export STRIP=$TOOLCHAIN/bin/$TARGET-strip
  sh ./autogen.sh
  CFLAGS="-fPIC" CXXFLAGS="-fPIC" ./configure --prefix=$PWD/depends/$TARGET --without-gui --disable-zmq --with-miniupnpc=no --with-incompatible-bdb --disable-bench --disable-tests
  make
}

pushd libnunchuk/contrib/bitcoin || exit
installBitcoinCore
popd || exit

#########################################################################################
####                                 OpenSSL Lib                                     ####
#########################################################################################

installOpenSSL() {
  if [ "$ANDROID_ABI" == $ANDROID_ABI_ARM64_V8A ]; then
    abi="arm64"
  elif [ "$ANDROID_ABI" == $ANDROID_ABI_ARMEABI_V7A ]; then
    abi="arm"
  else
    abi="$ANDROID_ABI"
  fi

  target=$ANDROID_TARGET
  echo "-------------------------------------------------------------------------------"
  echo "                    Installing OpenSSL for $abi $target                        "
  echo "-------------------------------------------------------------------------------"

  export TARGET=$target

  export AR=$TOOLCHAIN/bin/$TARGET-ar
  export AS=$TOOLCHAIN/bin/$TARGET-as
  export CC=$TOOLCHAIN/bin/$TARGET$API-clang
  export CXX=$TOOLCHAIN/bin/$TARGET$API-clang++
  export LD=$TOOLCHAIN/bin/$TARGET-ld
  export RANLIB=$TOOLCHAIN/bin/$TARGET-ranlib
  export STRIP=$TOOLCHAIN/bin/$TARGET-strip
  PATH=$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/darwin-x86_64/bin:$PATH
  ./Configure android-$abi -D__ANDROID_API__=$API
  make
}

pushd libnunchuk/contrib/openssl || exit
installOpenSSL
popd || exit

echo "done"
