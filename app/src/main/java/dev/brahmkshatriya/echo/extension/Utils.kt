package dev.brahmkshatriya.echo.extension

import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.common.models.StreamableAudio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.math.BigInteger
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object Utils {
    private const val SECRET = "g4el58wc0zvf9na1"
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
            val nextChar = bitwiseXor(trackMd5Hex[i], trackMd5Hex[i + 16], SECRET[i])
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

fun getByteStreamAudio(streamable: Streamable, client: OkHttpClient): StreamableAudio {
    val url = streamable.id
    val contentLength = Utils.getContentLength(url, client)
    val key = streamable.extra["key"]!!

    val request = Request.Builder().url(url).build()
    var decChunk = ByteArray(0)

    runBlocking {
        withContext(Dispatchers.IO) {
            val response = client.newCall(request).execute()
            val byteStream = response.body?.byteStream()

            // Read the entire byte stream into memory
            val completeStream = ByteArrayOutputStream()
            val buffer = ByteArray(2 * 1024 * 1024) // Increased buffer size
            var bytesRead: Int
            while (byteStream?.read(buffer).also { bytesRead = it ?: -1 } != -1) {
                completeStream.write(buffer, 0, bytesRead)
            }

            // Ensure complete stream is read
            val completeStreamBytes = completeStream.toByteArray()
            println("Total bytes read: ${completeStreamBytes.size}")

            // Determine chunk size based on decryption block size
            val decryptionBlockSize = 2048 * 1536 // Increased decryption block size
            val numChunks = (completeStreamBytes.size + decryptionBlockSize - 1) / decryptionBlockSize
            println("Number of chunks: $numChunks")

            // Measure decryption time
            val startTime = System.nanoTime()

            // Decrypt the chunks concurrently
            val deferredChunks = (0 until numChunks).map { i ->
                val start = i * decryptionBlockSize
                val end = minOf((i + 1) * decryptionBlockSize, completeStreamBytes.size)
                println("Chunk $i: start $start, end $end")
                async(Dispatchers.Default) { decryptStreamChunk(completeStreamBytes.copyOfRange(start, end), key) }
            }

            // Wait for all decryption tasks to complete and concatenate the results
            deferredChunks.forEach { deferred ->
                decChunk += deferred.await()
            }

            val endTime = System.nanoTime()
            val duration = endTime - startTime
            println("Decryption took ${duration / 1_000_000} milliseconds")

            response.close()
        }
    }

    return StreamableAudio.ByteStreamAudio(
        stream = decChunk.inputStream(),
        totalBytes = contentLength
    )
}

private fun decryptStreamChunk(chunk: ByteArray, key: String): ByteArray {
    val decryptedStream = ByteArrayOutputStream()
    var place = 0

    while (place < chunk.size) {
        val remainingBytes = chunk.size - place
        val currentChunkSize = if (remainingBytes > 2048 * 3) 2048 * 3 else remainingBytes
        val decryptingChunk = chunk.copyOfRange(place, place + currentChunkSize)
        place += currentChunkSize

        if (decryptingChunk.size > 2048) {
            val decryptedChunk = Utils.decryptBlowfish(decryptingChunk.copyOfRange(0, 2048), key)
            decryptedStream.write(decryptedChunk)
            decryptedStream.write(decryptingChunk, 2048, decryptingChunk.size - 2048)
        } else {
            decryptedStream.write(decryptingChunk)
        }
    }

    val decryptedBytes = decryptedStream.toByteArray()
    println("Decrypted chunk size: ${decryptedBytes.size}")
    return decryptedBytes
}