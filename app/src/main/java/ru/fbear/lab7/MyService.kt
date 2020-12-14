package ru.fbear.lab7

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.webkit.URLUtil
import kotlinx.coroutines.*
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL
import java.util.concurrent.atomic.AtomicInteger


class MyService : Service() {
    private val coroutineJob = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.Default + coroutineJob)

    private val count = AtomicInteger()

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        count.incrementAndGet()
        coroutineScope.launch {
            intent.getStringExtra("url")?.let { downloadImage(it) }
        }.invokeOnCompletion { count.decrementAndGet() }
        if (count.get() == 0) stopSelf(startId)
        return START_REDELIVER_INTENT
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun downloadImage(urlString: String) {
        withContext(Dispatchers.IO) {
            var outputStream: FileOutputStream? = null
            try {
                val url = URL(urlString)
                val connect = url.openConnection()
                val mimeType = connect.contentType
                val fileName = URLUtil.guessFileName(connect.url.toString(), null, mimeType)
                val inputStream = connect.getInputStream()
                outputStream = openFileOutput(fileName, Context.MODE_PRIVATE)
                outputStream.write(inputStream.readBytes())

                val i = Intent().apply {
                    action = "ru.fbear.lab7.DOWNLOAD_COMPLETE"
                    putExtra("ru.fbear.lab7.broadcast.Message", "$filesDir/$fileName")
                }
                sendBroadcast(i)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try {
                    outputStream?.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}