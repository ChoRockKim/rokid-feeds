package dev.chorock.newsfeed.net

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * 연결에 성공한 Wi-Fi를 이 기기에만 저장한다. 값은 Android Keystore의 AES-GCM 키로 암호화해 앱 전용 저장소에 둔다
 * (키는 기기를 떠나지 않고, 백업은 꺼져 있다). 비밀번호는 로그와 화면에 내보내지 않는다.
 */
class WifiStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun load(): WifiConfig? = runCatching {
        val blob = prefs.getString(KEY_BLOB, null) ?: return null
        val (ssid, password, security) = decrypt(blob).split(SEPARATOR, limit = 3)
        WifiConfig(ssid = ssid, password = password, security = WifiSecurity.valueOf(security))
    }.getOrNull()

    fun save(config: WifiConfig) {
        val security = config.effectiveSecurity
        val plain = listOf(config.ssid, config.password, security.name).joinToString(SEPARATOR)
        prefs.edit().putString(KEY_BLOB, encrypt(plain)).apply()
    }

    fun clear() {
        prefs.edit().remove(KEY_BLOB).apply()
    }

    private fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key()) }
        val encrypted = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + ":" + Base64.encodeToString(encrypted, Base64.NO_WRAP)
    }

    private fun decrypt(blob: String): String {
        val (iv, data) = blob.split(":", limit = 2)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)))
        }
        return String(cipher.doFinal(Base64.decode(data, Base64.NO_WRAP)), Charsets.UTF_8)
    }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (store.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val FILE = "wifi_saved"
        const val KEY_BLOB = "blob"
        const val ALIAS = "newsfeed.wifi"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val SEPARATOR = "\u0000"
    }
}
