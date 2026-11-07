package cn.ws9.daoyi.data

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
 * API Key 优先用 Android Keystore 中不可导出的 AES-256-GCM 密钥加密后存入本机。
 *
 * 崩溃修复：密钥以 setRandomizedEncryptionRequired(true) 创建时，Android Keystore
 * 在 ENCRYPT_MODE 下禁止调用方自带 IV（会抛 InvalidAlgorithmParameterException:
 * Caller-provided IV not permitted），旧实现却显式传了 GCMParameterSpec(iv)，且没有
 * 任何异常兜底，导致「保存设置」在主线程直接崩溃退出。
 * 现在加密改为由系统生成 IV、再用 Cipher.getIV() 取回；解密仍可显式传 IV。
 *
 * 兼容性：若设备密钥库损坏或不可用，降级为应用私有 SharedPreferences 明文保存
 * （其他应用无权限读取），并返回明确的安全提示，绝不让保存动作崩溃。
 */
class CryptoStore(context: Context) {

    companion object {
        private const val ALIAS = "daoyi_llm_api_key_v1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val K_ENC = "encrypted_key"
        private const val K_PLAIN = "plain_key"
        private const val K_FALLBACK = "key_fallback"
        private const val K_ERROR = "key_error"
    }

    private val prefs = context.applicationContext.getSharedPreferences("daoyi_secure", Context.MODE_PRIVATE)

    /** 需要长期展示给用户的安全提示（降级存储时非空）。 */
    val fallbackNotice: String
        get() = if (prefs.getBoolean(K_FALLBACK, false))
            "⚠ 本机密钥库不可用，API Key 现以应用私有区明文保存（其他应用无法读取）。" else ""

    /** 上次密钥库错误，仅用于排查。 */
    val lastError: String get() = prefs.getString(K_ERROR, "").orEmpty()

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val spec = KeyGenParameterSpec.Builder(
            ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
            .build()
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(spec)
        return gen.generateKey()
    }

    /** 不传 IV：由 Android Keystore 随机生成，随后用 cipher.iv 取回并随密文一起保存。 */
    private fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val iv = cipher.iv ?: throw IllegalStateException("密钥库未返回 IV")
        val data = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(iv, Base64.NO_WRAP) + ":" +
                Base64.encodeToString(data, Base64.NO_WRAP)
    }

    private fun decrypt(payload: String): String {
        val parts = payload.split(":")
        if (parts.size != 2) return ""
        val iv = Base64.decode(parts[0], Base64.NO_WRAP)
        val data = Base64.decode(parts[1], Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
        return String(cipher.doFinal(data), Charsets.UTF_8)
    }

    /** @return 空串表示正常保存；非空为需要展示给用户的安全提示。 */
    fun save(payload: String): String {
        if (payload.isBlank()) {
            prefs.edit().remove(K_ENC).remove(K_PLAIN).remove(K_ERROR)
                .putBoolean(K_FALLBACK, false).apply()
            return ""
        }
        return runCatching { encrypt(payload) }.fold(
            onSuccess = { value ->
                runCatching {
                    prefs.edit().putString(K_ENC, value).remove(K_PLAIN).remove(K_ERROR)
                        .putBoolean(K_FALLBACK, false).apply()
                }
                ""
            },
            onFailure = { e ->
                runCatching {
                    prefs.edit().putString(K_PLAIN, payload).remove(K_ENC)
                        .putBoolean(K_FALLBACK, true)
                        .putString(K_ERROR, "${e.javaClass.simpleName}: ${e.message.orEmpty()}")
                        .apply()
                }
                fallbackNotice.ifBlank {
                    "⚠ 密钥库不可用（${e.javaClass.simpleName}），Key 已降级存入应用私有区。"
                }
            }
        )
    }

    fun read(): String = runCatching {
        val enc = prefs.getString(K_ENC, null)
        val plain = if (enc.isNullOrBlank()) "" else runCatching { decrypt(enc) }.getOrDefault("")
        plain.ifBlank { prefs.getString(K_PLAIN, null).orEmpty() }
    }.getOrDefault("")
}
