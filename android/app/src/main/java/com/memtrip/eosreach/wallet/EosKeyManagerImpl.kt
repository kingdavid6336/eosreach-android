package com.memtrip.eosreach.wallet

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.memtrip.eos.core.crypto.EosPrivateKey
import com.memtrip.eosreach.R
import com.memtrip.eosreach.utils.RxSchedulers
import io.reactivex.Single
import javax.inject.Inject

class EosKeyManagerImpl @Inject constructor(
    private val keyStoreWrapper: KeyStoreWrapper,
    private val cipherWrapper: CipherWrapper,
    private val rxSchedulers: RxSchedulers,
    application: Application
) : EosKeyManager {

    private val sharedPreferences: SharedPreferences = application.getSharedPreferences(
        application.getString(R.string.app_keys_shared_preferences_package),
        Context.MODE_PRIVATE)

    override fun importPrivateKey(eosPrivateKey: EosPrivateKey): Single<String> {
        return Single.create<String> { single ->
            val keyAlias = eosPrivateKey.publicKey.toString()
            keyStoreWrapper.createAndroidKeyStoreAsymmetricKey(keyAlias)
            encryptAndSavePrivateKey(keyAlias, eosPrivateKey)
            single.onSuccess(keyAlias)
        }.observeOn(rxSchedulers.main()).subscribeOn(rxSchedulers.background())
    }

    private fun encryptAndSavePrivateKey(keyAlias: String, eosPrivateKey: EosPrivateKey) {
        val keyPair = keyStoreWrapper.getAndroidKeyStoreAsymmetricKeyPair(keyAlias)
        val encryptedData = cipherWrapper.encrypt(eosPrivateKey.bytes, keyPair?.public)
        sharedPreferences
            .edit()
            .putString(keyAlias, encryptedData)
            .apply()
    }

    override fun getPrivateKey(eosPublicKey: String): Single<EosPrivateKey> {
        return Single.create<EosPrivateKey> { single ->
            single.onSuccess(EosPrivateKey(getPrivateKeyBytes(eosPublicKey)))
        }
    }

    override fun publicKeyExists(eosPublicKey: String): Boolean {
        return sharedPreferences.getString(eosPublicKey, null) != null
    }

    override fun getAllPublicKeys(): List<String> {
        return sharedPreferences.all.entries.map { it.key }
    }

    override fun getPrivateKeys(): Single<List<EosPrivateKey>> {
        return Single.create<List<EosPrivateKey>> { single ->
            val entries = sharedPreferences.all.entries
            if (entries.isNotEmpty()) {
                single.onSuccess(entries.map { EosPrivateKey(getPrivateKeyBytes(it.key)) })
            } else {
                single.onError(EosKeyManager.NotFoundException())
            }
        }
    }

    private fun getPrivateKeyBytes(keyAlias: String): ByteArray {

        val encodedEncryptedPrivateKey = sharedPreferences.getString(keyAlias, null)

        if (encodedEncryptedPrivateKey != null) {
            val keyPair = keyStoreWrapper.getAndroidKeyStoreAsymmetricKeyPair(keyAlias)
            return cipherWrapper.decrypt(encodedEncryptedPrivateKey, keyPair?.private)
        } else {
            throw EosKeyManager.NotFoundException()
        }
    }
}