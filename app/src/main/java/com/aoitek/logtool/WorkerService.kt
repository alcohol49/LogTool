package com.aoitek.logtool

import android.app.*
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.Uri
import android.os.*
import android.support.v4.app.NotificationCompat
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.storage.FirebaseStorage
import java.io.*
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream


class WorkerService : Service() {

    val TAG = "WorkerService"
    val mNotificationId = 1
    lateinit var mWakeLock: PowerManager.WakeLock
    private var activityMessenger: Messenger? = null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
        startForeground(mNotificationId, buildForegroundNotification())
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand:")

//        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
//        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WorkerService")
//        mWakeLock.acquire()

        activityMessenger = intent.getParcelableExtra(MESSENGER_INTENT_KEY)

        if (intent.getBooleanExtra(UPLOAD_ONLY_KEY, false)) {
            uploadFiles()
        } else {
            PackAsyncTask(this).execute()

            val interval = intent.getIntExtra(ALARM_INTERVAL_KEY, 60 * 60 * 1000)
            scheduleAlarm(applicationContext, activityMessenger, interval)
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
    }

    fun scheduleAlarm(context: Context, messenger: Messenger?, triggerAtMillis: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val workerIntent = Intent(context, WorkerService::class.java)
        workerIntent.putExtra(MESSENGER_INTENT_KEY, messenger)
        workerIntent.putExtra(ALARM_INTERVAL_KEY, triggerAtMillis)

        val pi = PendingIntent.getService(context, 0, workerIntent, PendingIntent.FLAG_CANCEL_CURRENT)
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + triggerAtMillis, pi)
        Log.d(TAG, "setExactAndAllowWhileIdle with triggerAtMillis = " + triggerAtMillis)
    }

    fun buildForegroundNotification(): Notification {
        val mBuilder = NotificationCompat.Builder(this, GENERAL_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_cloud_queue_24dp)
                .setContentTitle("LogTool Running")
        return mBuilder.build()
    }

    @Throws(IOException::class)
    private fun zipFiles(files: Array<File>, zipFile: File) {
        val BUFFER_SIZE = 1024
        var origin: BufferedInputStream?
        val out = ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile)))

        try {
            val data = ByteArray(BUFFER_SIZE)
            for (file in files) {
                val fileInputStream = FileInputStream(file)
                origin = BufferedInputStream(fileInputStream, BUFFER_SIZE)
                val filePath = file.absolutePath
                try {
                    val entry = ZipEntry(filePath.substring(filePath.lastIndexOf("/") + 1))
                    out.putNextEntry(entry)
                    var count = origin.read(data, 0, BUFFER_SIZE)
                    while (count != -1) {
                        out.write(data, 0, count)
                        count = origin.read(data, 0, BUFFER_SIZE)
                    }
                } finally {
                    origin.close()
                }
            }
        } finally {
            out.close()
        }
    }

    private class PackAsyncTask(service: WorkerService) : AsyncTask<Unit, Unit, Unit>() {
        val TAG = "WorkerService"
        val mService: WeakReference<WorkerService> = WeakReference(service)

        override fun doInBackground(vararg params: Unit) {
            val service = mService.get() ?: return
            val files = File(LOGS_FILE_PATH).listFiles()
            if (files != null) {
                val format = SimpleDateFormat("yyyyMMdd-hhmmss", Locale.getDefault()).format(Date()) + ".zip"

                val dir = File(ZIP_FILE_PATH)
                if (!dir.exists()) dir.mkdir()

                val zipFile = File(ZIP_FILE_PATH + File.separator +format)
                service.zipFiles(files, zipFile)

                // Firebase event
                val bundle = Bundle()
                bundle.putString(FILE_NAME_FIREBASE_KEY, format)
                FirebaseAnalytics.getInstance(service.applicationContext).logEvent(FILE_PACKED_FIREBASE_EVENT, bundle)

                Log.d(TAG, "doInBackground: packed logs " + format)
            }
        }

        override fun onPostExecute(result: Unit?) {
            val service = mService.get() ?: return
            service.uploadFiles()
            service.sendMessage(MSG_REFRESH_LIST)
        }
    }

    fun uploadFiles() {
        // check network state
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork: NetworkInfo? = cm.activeNetworkInfo
        val isConnected: Boolean = activeNetwork?.isConnectedOrConnecting == true
        Log.d(TAG, "uploadFile: isConnectedOrConnecting = " + isConnected)
        if (isConnected) {
            val files = File(ZIP_FILE_PATH).listFiles()
            if (files != null) {
                for (file in files) {
                    if (file.isFile) uploadFile(file)
                }
            }
        }
//        mWakeLock.release()
        stopSelf()
    }

    private fun uploadFile(file: File) {

        val storage = FirebaseStorage.getInstance()
        val storageRef = storage.reference

        val fileUri = Uri.fromFile(file)
        val fileRef = storageRef.child(fileUri.lastPathSegment)
        val uploadTask = fileRef.putFile(fileUri)

        uploadTask
                .addOnFailureListener {
                    Log.d(TAG, "onFailurre", it)
                }
                .addOnSuccessListener {
                    val fileName = fileUri.lastPathSegment
                    Log.d(TAG, String.format("onSuccess: %s, upload size %d", fileName, it.metadata?.sizeBytes))
                    buildNotification(fileName)

                    file.delete()

                    // Firebase event
                    val bundle = Bundle()
                    bundle.putString(FILE_NAME_FIREBASE_KEY, fileName)
                    FirebaseAnalytics.getInstance(applicationContext).logEvent(FILE_UPLOADED_FIREBASE_EVENT, bundle)

                    sendMessage(MSG_REFRESH_LIST)
                }
    }

    fun buildNotification(name: String?) {
        val mBuilder = NotificationCompat.Builder(this, GENERAL_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_cloud_queue_24dp)
                .setContentTitle("Log Uploaded")
                .setContentText(name)
        val mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val id = Date().time / 1000
        mNotificationManager.notify(id.toInt(), mBuilder.build())
    }

    private fun sendMessage(messageID: Int) {
        if (activityMessenger == null) {
            Log.d(TAG, "Service is bound, not started. There's no callback to send a message to.")
            return
        }
        val message = Message.obtain()
        message.run {
            what = messageID
        }
        try {
            activityMessenger?.send(message)
        } catch (e: RemoteException) {
            Log.e(TAG, "Error passing service object back to activity.")
        }
    }
}