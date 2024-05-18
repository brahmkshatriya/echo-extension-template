package dev.brahmkshatriya.echo.extension.network

import fi.iki.elonen.NanoHTTPD
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.InputStream

class MultiStreamServer(private val urls: List<String>, port: Int) : NanoHTTPD(port) {

    private val client = OkHttpClient()

    override fun serve(session: IHTTPSession): Response {
        val contentLength = getContentLength(urls)
        return newFixedLengthResponse(Response.Status.OK, "audio/mpeg", CombinedAudioInputStream(urls, client), contentLength)
    }

    private fun getContentLength(urls: List<String>): Long {
        var totalLength = 0L
        urls.forEach { url ->
            val request = Request.Builder().url(url).head().build()
            val response = client.newCall(request).execute()
            totalLength += response.header("Content-Length")?.toLong() ?: 0L
            response.close()
        }
        return totalLength
    }

    class CombinedAudioInputStream(private val urls: List<String>, private val client: OkHttpClient) : InputStream() {
        private var currentStreamIndex = 0
        private var currentStream: InputStream? = null

        init {
            openNextStream()
        }

        override fun read(): Int {
            while (currentStream != null) {
                val result = currentStream!!.read()
                if (result != -1) return result
                openNextStream()
            }
            return -1
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            while (currentStream != null) {
                val result = currentStream!!.read(b, off, len)
                if (result != -1) return result
                openNextStream()
            }
            return -1
        }

        private fun openNextStream() {
            currentStream?.close()
            if (currentStreamIndex < urls.size) {
                val request = Request.Builder().url(urls[currentStreamIndex]).build()
                val response = client.newCall(request).execute()
                currentStream = response.body?.byteStream()
                currentStreamIndex++
            } else {
                currentStream = null
            }
        }
    }
}