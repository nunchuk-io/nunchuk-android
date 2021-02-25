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

export API=21

pwd=$(pwd)
echo "pwd::$pwd"

#########################################################################################
####                                Bitcoin Deps                                     ####
#########################################################################################
installBitcoinDeps() {
  abi=$1
  target=$2

  echo "-------------------------------------------------------------------------------"
  echo "                          Installing deps for $abi                              "
  echo "-------------------------------------------------------------------------------"

  export TARGET=$target

  export AR=$TOOLCHAIN/bin/$TARGET-ar
  export AS=$TOOLCHAIN/bin/$TARGET-as
  export CC=$TOOLCHAIN/bin/$TARGET$API-clang
  export CXX=$TOOLCHAIN/bin/$TARGET$API-clang++
  export LD=$TOOLCHAIN/bin/$TARGET-ld
  export RANLIB=$TOOLCHAIN/bin/$TARGET-ranlib
  export STRIP=$TOOLCHAIN/bin/$TARGET-strip

  ANDROID_NDK=$ANDROID_NDK ANDROID_NDK=$ANDROID_NDK make HOST=$TARGET ANDROID_TOOLCHAIN_BIN=$TOOLCHAIN ANDROID_API_LEVEL=$API NO_QT=1 NO_ZMQ=1 NO_QR=1 NO_UPNP=1
}
#
pushd libnunchuk/contrib/bitcoin/depends || exit
#installBitcoinDeps arm64-v8a aarch64-linux-android
installBitcoinDeps x86-64 x86_64-linux-android
#installBitcoinDeps x86         i686-linux-android
#installBitcoinDeps armeabi-v7a armv7a-linux-androideabi
popd || exit

#########################################################################################
####                                 Bitcoin Core                                    ####
#########################################################################################
installBitcoinCore() {
  abi=$1
  target=$2
  echo "-------------------------------------------------------------------------------"
  echo "                           Installing core for $abi                             "
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
#installBitcoinCore arm64-v8a aarch64-linux-android
installBitcoinCore x86-64 x86_64-linux-android
#installBitcoinCore x86 i686-linux-android
#installBitcoinCore armeabi-v7a armv7a-linux-androideabi
popd || exit

#########################################################################################
####                                 OpenSSL Lib                                     ####
#########################################################################################

installOpenSSL() {
  abi=$1
  target=$2
  echo "-------------------------------------------------------------------------------"
  echo "                       Installing OpenSSL for $abi                             "
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
#installOpenSSL arm64-v8a aarch64-linux-android
installOpenSSL x86_64 x86_64-linux-android
#installOpenSSL x86 i686-linux-android
#installOpenSSL armeabi-v7a armv7a-linux-androideabi
popd || exit

echo "done"
