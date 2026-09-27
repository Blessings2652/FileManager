package com.shizuku.filemanager.fs.engine

import org.junit.Test
import kotlin.test.assertEquals

class LsParserTest {

    @Test
    fun testParseListing() {
        val lsOutput = """
            total 24
            drwxr-xr-x 4 root root 4096 2024-01-01 12:00:00.000000000 +0000 .
            drwxr-xr-x 5 root root 4096 2024-01-01 12:00:00.000000000 +0000 ..
            -rw-r--r-- 1 root root  123 2024-01-02 10:30:00.000000000 +0000 test.txt
            drwxr-xr-x 2 root root 4096 2024-01-03 14:15:00.000000000 +0000 my_folder
        """.trimIndent()

        val entries = LsParser.parseListing(lsOutput, "/sdcard")
        assertEquals(2, entries.size)

        val file = entries[1] // sorted alphabetically: my_folder (dir), test.txt (file)
        assertEquals("test.txt", file.name)
        assertEquals("/sdcard/test.txt", file.path)
        assertEquals(false, file.isDirectory)
        assertEquals(123L, file.sizeBytes)

        val dir = entries[0]
        assertEquals("my_folder", dir.name)
        assertEquals(true, dir.isDirectory)
    }

    @Test
    fun testQuote() {
        assertEquals("'hello world'", LsParser.quote("hello world"))
        assertEquals("'it'\\''s me'", LsParser.quote("it's me"))
    }
}
