#include <cstring>
#include <jni.h>
#include <syslog.h>
#include <nunchuk.h>

using namespace nunchuk;

/**
 * Used to convert C++ Classes into Java models
 */
class Deserializer {

public:

    static jobject convert2JBoolean(JNIEnv *env, const bool value);

    static jobject convert2JListString(JNIEnv *env, const std::vector<std::string> &values);

    static jobject convert2JSignersMap(JNIEnv *env, const std::map<std::string, bool> signersMap);

    static void convert2JException(JNIEnv *env, const char *msg);

    static jobject convert2JDevice(JNIEnv *env, const Device &device);

    static jobject convert2JAmount(JNIEnv *env, const Amount amount);

    static jobject convert2JTxInput(JNIEnv *env, const TxInput input);

    static jobject convert2JTxInputs(JNIEnv *env, const std::vector<TxInput> inputs);

    static jobject convert2JTxOutput(JNIEnv *env, const TxOutput output);

    static jobject convert2JTxOutputs(JNIEnv *env, const std::vector<TxOutput> outputs);

    static jobject convert2JAddressType(JNIEnv *env, const AddressType &type);

    static jobject convert2JTransactionStatus(JNIEnv *env, const TransactionStatus &status);

    static jobject convert2JMasterSigner(JNIEnv *env, const MasterSigner &signer);

    static jobject convert2JMasterSigners(JNIEnv *env, const std::vector<MasterSigner> &signers);

    static jobject convert2JSigner(JNIEnv *env, const SingleSigner &signer);

    static jobject convert2JSigners(JNIEnv *env, const std::vector<SingleSigner> &signers);

    static jobject convert2JWallet(JNIEnv *env, const Wallet &wallet);

    static jobject convert2JWallets(JNIEnv *env, const std::vector<Wallet> &wallets);

    static jobject convert2JTransaction(JNIEnv *env, const Transaction &transaction);

    static jobject convert2JTransactions(JNIEnv *env, const std::vector<Transaction> &transactions);

};