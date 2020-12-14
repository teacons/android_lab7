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
import android.webkit.URLUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.ref.WeakReference
import java.net.URL


class MyService : Service() {

    private lateinit var mMessenger: Messenger

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        runBlocking {
            val path = downloadImage(intent.getStringExtra(URL))
            path?.let { broadcast(it) }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        mMessenger = Messenger(IncomingHandler(WeakReference(this)))
        return mMessenger.binder
    }

    companion object {
        private const val DOWNLOAD_URL = 1
        private const val URL = "url"
    }
    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun downloadImage(urlString: String?): String? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(urlString)
                val connect = url.openConnection()
                val filename =
                    URLUtil.guessFileName(connect.url.toString(), null, connect.contentType)
                val inputStream = connect.getInputStream()
                saveBitmap(inputStream, connect.contentType, filename)
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

    class IncomingHandler(linkToService: WeakReference<MyService>) :
        Handler(Looper.getMainLooper()) {
        private val service = linkToService.get()
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                DOWNLOAD_URL -> {
                    runBlocking {
                        val path = service?.downloadImage(msg.data.getString(URL))
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
        inputStream: InputStream,
        mimeType: String?,
        displayName: String
    ): String {
        val relativeLocation = Environment.DIRECTORY_PICTURES
        val contentValues = ContentValues()
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, relativeLocation)
        var stream: OutputStream? = null
        var uri: Uri? = null
        try {
            val contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            uri = contentResolver.insert(contentUri, contentValues)
            if (uri == null) {
                throw IOException("Failed to create new MediaStore record.")
            }
            stream = contentResolver.openOutputStream(uri)
            if (stream == null) {
                throw IOException("Failed to get output stream.")
            }
            stream.write(inputStream.readBytes())
            return uri.toString()
        } catch (e: IOException) {
            if (uri != null) {
                contentResolver.delete(uri, null, null)
            }
            throw e
        } finally {
            stream?.close()
        }
    }
}