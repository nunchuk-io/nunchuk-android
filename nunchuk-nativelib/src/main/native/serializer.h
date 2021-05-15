#include <cstring>
#include <jni.h>
#include <syslog.h>
#include <nunchuk.h>

using namespace nunchuk;

/**
 * Used to translate Java properties into C++ equivalents
 */
class Serializer {

public:

    static Device convert2CDevice(JNIEnv *env, jobject device);

    static WalletType convert2CWalletType(jint ordinal);

    static AddressType convert2CAddressType(jint ordinal);

    static Chain convert2CChain(jint ordinal);

    static BackendType convert2CBackendType(jint ordinal);

    static ExportFormat convert2CExportFormat(jint ordinal);

    static SingleSigner convert2CSigner(JNIEnv *env, jobject signer);

    static MasterSigner convert2CMasterSigner(JNIEnv *env, jobject signer);

    static std::vector<SingleSigner> convert2CSigners(JNIEnv *env, jobject signers);

    static Wallet convert2CWallet(JNIEnv *env, jobject wallet);

    static TxInput convert2CTxInput(JNIEnv *env, jobject input);

    static std::vector<TxInput> convert2CTxInputs(JNIEnv *env, jobject inputs);

    static TxOutput convert2CTxOutput(JNIEnv *env, jobject input);

    static std::vector<TxOutput> convert2CTxOutputs(JNIEnv *env, jobject outputs);

    static UnspentOutput convert2CUnspentOutput(JNIEnv *env, jobject unspentOutput);

    static std::vector<UnspentOutput> convert2CUnspentOutputs(JNIEnv *env, jobject unspentOutputs);

    static std::map<std::string, Amount> convert2CAmountsMap(JNIEnv *pEnv, jobject amountsMap);

    static Amount convert2CAmount(JNIEnv *env, jobject amount);
};