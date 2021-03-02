#include <string.h>
#include <jni.h>
#include <syslog.h>
#include <nunchuk.h>

using namespace nunchuk;

extern "C"
JNIEXPORT void JNICALL
Java_com_nunchuk_android_nativelib_LibNunchukAndroid_retrieveData(
        JNIEnv *env,
        jobject thisObj
) {
    syslog(LOG_DEBUG, "retrieveData::start");
    AppSettings settings;
    settings.set_chain(Chain::TESTNET);
    settings.set_hwi_path("bin/hwi");
    settings.enable_proxy(false);
    settings.set_testnet_servers({"testnet.nunchuk.io:50001"});
    settings.set_backend_type(BackendType::ELECTRUM);
    auto nunchuk = MakeNunchuk(settings);

    auto remoteSigners = nunchuk->GetRemoteSigners();
    if (remoteSigners.empty()) {
        syslog(LOG_DEBUG, "No remote signers");
    } else {
        syslog(LOG_DEBUG, "Remote signers found");
    }
    syslog(LOG_DEBUG, "Remote signer create success.");
    syslog(LOG_DEBUG, "retrieveData::end");
}