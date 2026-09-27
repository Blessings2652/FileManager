package com.shizuku.filemanager.sys

import org.junit.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class VaultManagerTest {

    @Test
    fun testEncryptDecrypt() {
        val plaintext = "Hello, Secure Vault World!".toByteArray(Charsets.UTF_8)
        val password = "StrongPassword123".toCharArray()

        val encrypted = VaultManager.encrypt(plaintext, password)
        val decrypted = VaultManager.decrypt(encrypted, password)

        assertContentEquals(plaintext, decrypted)
    }

    @Test
    fun testWrongPassword() {
        val plaintext = "Secret Data".toByteArray(Charsets.UTF_8)
        val password = "CorrectPassword".toCharArray()
        val wrongPassword = "WrongPassword".toCharArray()

        val encrypted = VaultManager.encrypt(plaintext, password)

        assertFailsWith<VaultManager.WrongPasswordException> {
            VaultManager.decrypt(encrypted, wrongPassword)
        }
    }

    @Test
    fun testHashSecret() {
        val hash1 = VaultManager.hashSecret("1234")
        val hash2 = VaultManager.hashSecret("1234")
        val hash3 = VaultManager.hashSecret("5678")

        assertEquals(hash1, hash2)
        org.junit.Assert.assertNotEquals(hash1, hash3)
    }

    @Test
    fun testVaultNaming() {
        assertEquals("document.txt.vault", VaultManager.vaultName("document.txt"))
        assertEquals("document.txt", VaultManager.originalName("document.txt.vault"))
        assertEquals("image.png", VaultManager.originalName("image.png"))
    }
}
