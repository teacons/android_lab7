package ru.fbear.lab7

import android.annotation.SuppressLint
import android.app.Service
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStream
import java.net.URL


class MyService : Service() {

    private lateinit var mMessenger: Messenger

    private var count = 0

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        runBlocking {
            val path = downloadImage(intent.getStringExtra(URL), "$count.png")
            count++
            path?.let { broadcast(it) }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        mMessenger = Messenger(IncomingHandler())
        return mMessenger.binder
    }

    companion object {
        private const val DOWNLOAD_URL = 1
        private const val URL = "url"
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun downloadImage(urlString: String?, filename: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(urlString)
                val inputStream = url.openStream()
                val img = BitmapFactory.decodeStream(inputStream)
                saveBitmap(img, CompressFormat.PNG, filename)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    private fun broadcast(path: String) {
        val i = Intent().apply {
            action = "ru.fbear.lab7.DOWNLOAD_COMPLETE"
            putExtra("ru.fbear.lab7.broadcast.Message", path)
            addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
        }
        sendBroadcast(i)
    }

    @SuppressLint("HandlerLeak")
    inner class IncomingHandler : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                DOWNLOAD_URL -> {
                    runBlocking {
                        val path = downloadImage(msg.data.getString(URL), "$count.png")
                        count++
                        path?.let {
                            val bundle = Bundle().apply {
                                putString(URL, it)
                            }
                            val mes: Message = Message.obtain(null, DOWNLOAD_URL).apply {
                                data = bundle
                            }
                            try {
                                val repMessenger = msg.replyTo
                                repMessenger.send(mes)
                            } catch (e: RemoteException) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
                else -> super.handleMessage(msg)
            }
        }
    }

    private fun saveBitmap(
        bitmap: Bitmap,
        format: CompressFormat,
        displayName: String
    ): String {
        val relativeLocation = Environment.DIRECTORY_PICTURES
        val contentValues = ContentValues()
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, relativeLocation)
        val resolver = contentResolver
        var stream: OutputStream? = null
        var uri: Uri? = null
        try {
            val contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            uri = resolver.insert(contentUri, contentValues)
            if (uri == null) {
                throw IOException("Failed to create new MediaStore record.")
            }
            stream = resolver.openOutputStream(uri)
            if (stream == null) {
                throw IOException("Failed to get output stream.")
            }
            if (!bitmap.compress(format, 95, stream)) {
                throw IOException("Failed to save bitmap.")
            }
            return uri.toString()
        } catch (e: IOException) {
            if (uri != null) {
                resolver.delete(uri, null, null)
            }
            throw e
        } finally {
            stream?.close()
        }
    }
}