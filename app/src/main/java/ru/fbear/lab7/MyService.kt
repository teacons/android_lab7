package ru.fbear.lab7

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.webkit.URLUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL


class MyService : Service() {

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        GlobalScope.launch {
            intent.getStringExtra("url")?.let { downloadImage(it) }
        }
        stopSelf(startId)
        return START_NOT_STICKY
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