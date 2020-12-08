package ru.fbear.lab7

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.IBinder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL


class MyService : Service() {

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        runBlocking {
            withContext(Dispatchers.IO) {
                var outputStream: FileOutputStream? = null
                try {
                    val urlString = intent.getStringExtra("url")
                    val url = URL(urlString)
                    val inputStream = url.openStream()
                    val img = BitmapFactory.decodeStream(inputStream)
                    outputStream = openFileOutput("test.png", Context.MODE_PRIVATE)
                    img.compress(Bitmap.CompressFormat.PNG, 100, outputStream)

                    val i = Intent().apply {
                        action = "ru.fbear.lab7.DOWNLOAD_COMPLETE"
                        putExtra("ru.fbear.lab7.broadcast.Message", "$filesDir/test.png")
                        addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
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
        stopSelf(startId)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}