package dev.brahmkshatriya.echo.extension

import okhttp3.OkHttpClient
import java.math.BigInteger
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object Utils {
    private const val secret = "g4el58wc0zvf9na1"
    private val secretIvSpec = IvParameterSpec(byteArrayOf(0,1,2,3,4,5,6,7))

    private fun bitwiseXor(firstVal: Char, secondVal: Char, thirdVal: Char): Char {
        return (BigInteger(byteArrayOf(firstVal.code.toByte())) xor
                BigInteger(byteArrayOf(secondVal.code.toByte())) xor
                BigInteger(byteArrayOf(thirdVal.code.toByte()))).toByte().toInt().toChar()
    }

    fun createBlowfishKey(trackId: String): String {
        val trackMd5Hex = trackId.toMD5()
        var blowfishKey = ""

        for (i in 0..15) {
            val nextChar = bitwiseXor(trackMd5Hex[i], trackMd5Hex[i + 16], secret[i])
            blowfishKey += nextChar
        }

        return blowfishKey
    }

    fun decryptBlowfish(chunk: ByteArray, blowfishKey: String): ByteArray {
        val secretKeySpec = SecretKeySpec(blowfishKey.toByteArray(), "Blowfish")
        val thisTrackCipher = Cipher.getInstance("BLOWFISH/CBC/NoPadding")
        thisTrackCipher.init(Cipher.DECRYPT_MODE, secretKeySpec, secretIvSpec)
        return thisTrackCipher.update(chunk)
    }

    fun getContentLength(url: String, client: OkHttpClient): Long {
        var totalLength = 0L
        val request = okhttp3.Request.Builder().url(url).head().build()
        val response = client.newCall(request).execute()
        totalLength += response.header("Content-Length")?.toLong() ?: 0L
        response.close()
        return totalLength
    }
}

private fun bytesToHex(bytes: ByteArray): String {
    var hexString = ""
    for (byte in bytes) {
        hexString += String.format("%02X", byte)
    }
    return hexString
}

fun String.toMD5(): String {
    val bytes = MessageDigest.getInstance("MD5").digest(this.toByteArray(Charsets.ISO_8859_1))
    return bytesToHex(bytes).lowercase()
}