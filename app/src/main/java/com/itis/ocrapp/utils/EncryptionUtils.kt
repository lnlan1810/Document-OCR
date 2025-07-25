package com.itis.ocrapp.utils

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import androidx.security.crypto.MasterKey
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import java.security.SecureRandom
import javax.crypto.KeyGenerator
import android.content.SharedPreferences

object EncryptionUtils {
    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/ECB/PKCS5Padding"
    private const val KEY_SIZE = 256

    private fun getEncryptedSharedPreferences(context: Context): SharedPreferences {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
       // val masterKey = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        return EncryptedSharedPreferences.create(
          //  context,
            "secure_app_prefs",
            masterKeyAlias,
         //   masterKey,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun generateAndStoreKey(context: Context): ByteArray {
        val keyGenerator = KeyGenerator.getInstance(ALGORITHM)
        keyGenerator.init(KEY_SIZE)
        val secretKey = keyGenerator.generateKey()
        val keyBytes = secretKey.encoded
        getEncryptedSharedPreferences(context)
            .edit()
            .putString("encryption_key", keyBytes.joinToString(",") { it.toString() })
            .apply()
        return keyBytes
    }

    private fun getStoredKey(context: Context): ByteArray {
        val keyString = getEncryptedSharedPreferences(context)
            .getString("encryption_key", null)
        return if (keyString != null) {
            keyString.split(",").map { it.toByte() }.toByteArray()
        } else {
            generateAndStoreKey(context)
        }
    }

    fun encryptFile(context: Context, inputFile: File, outputFile: File) {
        val key = SecretKeySpec(getStoredKey(context), ALGORITHM)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        FileInputStream(inputFile).use { input ->
            FileOutputStream(outputFile).use { output ->
                val buffer = ByteArray(1024)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    val encrypted = cipher.update(buffer, 0, bytesRead)
                    if (encrypted != null) {
                        output.write(encrypted)
                    }
                }
                val finalData = cipher.doFinal()
                if (finalData != null) {
                    output.write(finalData)
                }
            }
        }
    }

    fun decryptFile(context: Context, inputFile: File, outputFile: File) {
        val key = SecretKeySpec(getStoredKey(context), ALGORITHM)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key)
        FileInputStream(inputFile).use { input ->
            FileOutputStream(outputFile).use { output ->
                val buffer = ByteArray(1024)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    val decrypted = cipher.update(buffer, 0, bytesRead)
                    if (decrypted != null) {
                        output.write(decrypted)
                    }
                }
                val finalData = cipher.doFinal()
                if (finalData != null) {
                    output.write(finalData)
                }
            }
        }
    }
}
