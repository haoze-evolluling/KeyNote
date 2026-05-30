package com.haoze.keynote.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class KeyObfuscatorTest {
    @Test
    fun builtinZidaipass_resolves_correctly() {
        assertEquals("sk-28cdd82c0bda4c2ebf4a6792b0644782", KeyObfuscator.builtinZidaipass)
    }

    @Test
    fun sealUserZidaipass_actually_changes_input() {
        val plain = "my-api-key-12345"
        val sealed = KeyObfuscator.sealUserZidaipass(plain)
        assertNotEquals("sealUserZidaipass should not return the same string", plain, sealed)
    }

    @Test
    fun sealUserZidaipass_openUserZidaipass_are_inverses() {
        val plain = "my-api-key-12345"
        val sealed = KeyObfuscator.sealUserZidaipass(plain)
        val opened = KeyObfuscator.openUserZidaipass(sealed)
        assertEquals("openUserZidaipass(sealUserZidaipass(x)) should equal x", plain, opened)
    }

    @Test
    fun sealUserZidaipass_openUserZidaipass_are_inverses_special_chars() {
        val plain = "sk-" + "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6"
        val sealed = KeyObfuscator.sealUserZidaipass(plain)
        val opened = KeyObfuscator.openUserZidaipass(sealed)
        assertEquals(plain, opened)
    }
}


