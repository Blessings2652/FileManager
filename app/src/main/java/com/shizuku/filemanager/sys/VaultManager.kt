package com.shizuku.filemanager.sys

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Password-based per-file encryption using AES-256-GCM with a PBKDF2
 * derived key. No new Gradle dependency: everything here is javax.crypto,
 * built into the Android runtime.
 *
 * File format (all big-endian):
 *   MAGIC (4 bytes "FMV1") | salt (16 bytes) | iv (12 bytes) | ciphertext+tag
 *
 * This operates on raw byte arrays so it can sit behind any FileEngine
 * (readBytes/writeBytes), independent of Standard/Root/Shizuku/SAF.
 */
object VaultManager {

    private const val MAGIC = "FMV1"
    private const val ITERATIONS = 150_000
    private const val KEY_LENGTH_BITS = 256
    private const val GCM_TAG_LENGTH_BITS = 128
    const val VAULT_EXTENSION = "vault"

    class WrongPasswordException : Exception("Incorrect password or corrupted file")

    fun encrypt(plaintext: ByteArray, password: CharArray): ByteArray {
        val random = SecureRandom()
        val salt = ByteArray(16).also { random.nextBytes(it) }
        val iv = ByteArray(12).also { random.nextBytes(it) }
        val key = deriveKey(password, salt)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        val ciphertext = cipher.doFinal(plaintext)

        return MAGIC.toByteArray(Charsets.US_ASCII) + salt + iv + ciphertext
    }

    fun decrypt(vaultBytes: ByteArray, password: CharArray): ByteArray {
        if (vaultBytes.size < 4 + 16 + 12) throw WrongPasswordException()
        val magic = String(vaultBytes, 0, 4, Charsets.US_ASCII)
        if (magic != MAGIC) throw WrongPasswordException()

        val salt = vaultBytes.copyOfRange(4, 20)
        val iv = vaultBytes.copyOfRange(20, 32)
        val ciphertext = vaultBytes.copyOfRange(32, vaultBytes.size)
        val key = deriveKey(password, salt)

        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
            cipher.doFinal(ciphertext)
        } catch (e: Exception) {
            throw WrongPasswordException()
        }
    }

    /** Suggested vault filename for [originalName], e.g. "notes.txt" -> "notes.txt.vault". */
    fun vaultName(originalName: String): String = "$originalName.$VAULT_EXTENSION"

    /** Strips the ".vault" suffix to suggest a restore filename. */
    fun originalName(vaultFileName: String): String =
        if (vaultFileName.endsWith(".$VAULT_EXTENSION")) vaultFileName.removeSuffix(".$VAULT_EXTENSION") else vaultFileName

    /** Computes a secure SHA-256 hash for verifying PINs/passwords without storing them in plaintext. */
    fun hashSecret(secret: String, salt: String = "FM_VAULT_SALT_v1"): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val salted = "$salt:$secret:$salt"
        val hash = digest.digest(salted.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    private fun deriveKey(password: CharArray, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH_BITS)
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }
}
