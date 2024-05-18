package dev.brahmkshatriya.echo.extension

import android.content.ContentValues.TAG
import android.util.Log
import java.math.BigInteger
import java.net.CookieManager
import java.nio.charset.Charset
import java.security.MessageDigest
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class Decryptor {
    // Misc crypto junk
    private val secret = "g4el5" + "8wc0z" + "vf9na1"
    private val urlCryptKeeper = ("jo6ae" + "y6hai" + "d2Teih").toByteArray()
    private val urlSecretKeySpec = SecretKeySpec(urlCryptKeeper, "AES")
    private val secretIvSpec = IvParameterSpec(byteArrayOf(0,1,2,3,4,5,6,7))

    // Mmm cookies
    private val cookieManager = CookieManager()

    // Blowjob
    fun blowJob(chunk: ByteArray, blowfishKey: String): ByteArray {
        val secretKeySpec = SecretKeySpec(blowfishKey.toByteArray(), "Blowfish")
        val thisTrackCipher = Cipher.getInstance("BLOWFISH/CBC/NoPadding")
        thisTrackCipher.init(Cipher.DECRYPT_MODE, secretKeySpec, secretIvSpec)
        return thisTrackCipher.update(chunk)
    }

    // Hex and MD5 utils
    private fun bytesToHex(bytes: ByteArray): String {
        var hexString = ""

        for (byte in bytes) {
            hexString += String.format("%02X", byte)
        }

        return hexString
    }

    private fun String.toMd5Hex(): String {
        val bytes = MessageDigest.getInstance("MD5").digest(this.toByteArray(Charsets.ISO_8859_1))
        return bytesToHex(bytes).toLowerCase(Locale.ROOT)
    }

    // Download URL generation
    private fun md5(data: String, type: Charset = Charsets.US_ASCII): String {
        val md = MessageDigest.getInstance("MD5")
        md.update(data.toByteArray(type))
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    private fun getBlowfishKey(trackId: String): String {
        val SECRET = "g4el58wc0zvf9na1"
        val idMd5 = md5(trackId)
        val bfKey = StringBuilder()
        for (i in 0 until 16) {
            bfKey.append(
                (idMd5[i].toInt() xor idMd5[i + 16].toInt() xor SECRET[i].toInt()).toChar()
            )
        }
        return bfKey.toString()
    }

    private fun decryptChunk(chunk: ByteArray, blowFishKey: String): ByteArray {
        val cipher = Cipher.getInstance("Blowfish/CBC/NoPadding")
        val keySpec = SecretKeySpec(blowFishKey.toByteArray(Charsets.US_ASCII), "Blowfish")
        val iv = IvParameterSpec(byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7))
        cipher.init(Cipher.DECRYPT_MODE, keySpec, iv)
        return cipher.doFinal(chunk)
    }

    private fun getSongFileName(md5: String, trackId: String, mediaVerison: String, quality: Int): String {
        val sngId = trackId.toInt()
        val mediaVersion = mediaVerison.toInt()

        val step1 = listOf(md5, quality, sngId, mediaVersion).joinToString("¤")
        var step2 = md5(step1) + "¤" + step1 + "¤"
        while (step2.length % 16 > 0) step2 += " "

        val cipher = Cipher.getInstance("AES/ECB/NoPadding")
        val keySpec = SecretKeySpec("jo6aey6haid2Teih".toByteArray(Charsets.US_ASCII), "AES")
        cipher.init(Cipher.ENCRYPT_MODE, keySpec)
        return cipher.doFinal(step2.toByteArray(Charsets.US_ASCII)).joinToString("") { "%02x".format(it) }
    }

    fun decryptDownload(source: ByteArray, trackId: String): ByteArray {
        val chunkSize = 2048
        val blowFishKey = getBlowfishKey(trackId)
        var i = 0
        var position = 0

        val destBuffer = ByteArray(source.size)
        while (position < source.size) {
            val size = if (source.size - position >= chunkSize) chunkSize else source.size - position
            val chunk = ByteArray(size)
            System.arraycopy(source, position, chunk, 0, size)

            val chunkString = if (i % 3 > 0 || size < chunkSize) {
                String(chunk, Charsets.ISO_8859_1)
            } else {
                String(decryptChunk(chunk, blowFishKey), Charsets.ISO_8859_1)
            }

            System.arraycopy(chunkString.toByteArray(Charsets.ISO_8859_1), 0, destBuffer, position, chunkString.length)
            position += size
            i++
        }
        return destBuffer
    }

    fun getTrackDownloadUrl(md5: String, trackId: String, mediaVerison: String, quality: Int): String {
        val cdn = (md5)[0]
        val filename = getSongFileName(md5, trackId, mediaVerison, quality)
        return "http://e-cdn-proxy-$cdn.deezer.com/mobile/1/$filename"
    }

    // Blowfish key generation
    private fun bitwiseXor(firstVal: Char, secondVal: Char, thirdVal: Char): Char {
        return (BigInteger(byteArrayOf(firstVal.toByte())) xor
                BigInteger(byteArrayOf(secondVal.toByte())) xor
                BigInteger(byteArrayOf(thirdVal.toByte()))).toByte().toChar()
    }

    fun createBlowfishKey(trackId: Long): String {
        val trackMd5Hex = trackId.toString().toMd5Hex()
        var blowfishKey = ""

        Log.d(TAG, trackMd5Hex)

        for (i in 0..15) {
            val nextChar = bitwiseXor(trackMd5Hex[i], trackMd5Hex[i + 16], secret[i])
            blowfishKey += nextChar
        }

        Log.d(TAG, blowfishKey)

        return blowfishKey
    }
}

