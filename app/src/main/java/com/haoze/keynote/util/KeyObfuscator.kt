package com.haoze.keynote.util

object KeyObfuscator {
    private val ka = byteArrayOf(
        0x3A.toByte(), 0x5F.toByte(), 0x91.toByte(), 0x2C.toByte(),
        0x7E.toByte(), 0xB4.toByte(), 0x6D.toByte(), 0x18.toByte()
    )
    private val kb = byteArrayOf(
        0x58.toByte(), 0x23.toByte(), 0xE7.toByte(), 0x9A.toByte(),
        0x4C.toByte(), 0x0F.toByte(), 0xB1.toByte(), 0x65.toByte()
    )
    private val kc = byteArrayOf(
        0x83.toByte(), 0xE1.toByte(), 0x2A.toByte(), 0x7D.toByte(),
        0x56.toByte(), 0x91.toByte(), 0x3C.toByte(), 0x0B.toByte()
    )
    private val kaUser = byteArrayOf(
        0x4D.toByte(), 0x1A.toByte(), 0xC8.toByte(), 0x37.toByte(),
        0x62.toByte(), 0x9E.toByte(), 0xFB.toByte(), 0x54.toByte()
    )
    private val kbUser = byteArrayOf(
        0x29.toByte(), 0x6C.toByte(), 0xD3.toByte(), 0x8E.toByte(),
        0x71.toByte(), 0x05.toByte(), 0xBA.toByte(), 0x4F.toByte()
    )
    private val kcUser = byteArrayOf(
        0xBE.toByte(), 0x34.toByte(), 0x65.toByte(), 0x8A.toByte(),
        0x71.toByte(), 0x22.toByte(), 0xD9.toByte(), 0x46.toByte()
    )
    private val defaultZidaipassBlob = byteArrayOf(
        0x92.toByte(), 0xF6.toByte(), 0x71.toByte(), 0xF9.toByte(),
        0x5C.toByte(), 0x49.toByte(), 0x84.toByte(), 0x12.toByte(),
        0xD9.toByte(), 0xAF.toByte(), 0x3F.toByte(), 0xFB.toByte(),
        0x06.toByte(), 0x4E.toByte(), 0x81.toByte(), 0x42.toByte(),
        0x82.toByte(), 0xAF.toByte(), 0x39.toByte(), 0xA9.toByte(),
        0x02.toByte(), 0x1E.toByte(), 0x81.toByte(), 0x40.toByte(),
        0xD6.toByte(), 0xA4.toByte(), 0x6E.toByte(), 0xA9.toByte(),
        0x54.toByte(), 0x1C.toByte(), 0xD4.toByte(), 0x42.toByte(),
        0xD6.toByte(), 0xA5.toByte(), 0x6E.toByte()
    )

    val builtinZidaipass: String by lazy {
        _reconstruct(defaultZidaipassBlob, ka, kb, kc)
    }

    fun sealUserZidaipass(plain: String): String {
        val mixed = _mix(plain.encodeToByteArray(), kaUser, kbUser, kcUser)
        return java.util.Base64.getEncoder().encodeToString(mixed)
    }

    fun openUserZidaipass(sealed: String): String {
        return try {
            val mixed = java.util.Base64.getDecoder().decode(sealed)
            _reconstruct(mixed, kcUser, kbUser, kaUser)
        } catch (_: Exception) {
            ""
        }
    }

    private fun _mix(data: ByteArray, vararg keys: ByteArray): ByteArray {
        var result = data
        for (key in keys) {
            result = ByteArray(result.size) { i ->
                (result[i].toInt() xor key[i % key.size].toInt()).toByte()
            }
        }
        return result
    }

    private fun _reconstruct(data: ByteArray, vararg keys: ByteArray): String {
        return _mix(data, *keys).decodeToString()
    }
}
