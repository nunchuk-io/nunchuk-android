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
    AddressType address_type = AddressType::NATIVE_SEGWIT;

    // Nunchuk supports multisig, singlesig and escrow wallets
    WalletType wallet_type = WalletType::MULTI_SIG;

    syslog(LOG_DEBUG, "wallet_type::${wallet_type}");
    auto wallet = nunchuk->ImportWalletConfigFile("/data/user/0/com.nunchuk.android/files/nunchuk/Config.txt");
    // Log wallet.get_id()
    nunchuk->AddBalanceListener([](std::string wid, Amount amount) {
        syslog(LOG_DEBUG, "Receive callback");
    });
    auto wallets = nunchuk->GetWallets();
    // Check if wallets contain wallet

    syslog(LOG_DEBUG, "retrieveData::end");
}