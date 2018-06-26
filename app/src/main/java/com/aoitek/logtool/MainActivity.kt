package com.aoitek.logtool

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Build
import android.os.Bundle
import android.os.FileObserver
import android.preference.PreferenceManager
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.*
import java.io.File
import com.crashlytics.android.Crashlytics
import io.fabric.sdk.android.Fabric


class MainActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {

    val TAG = "MainActivity"
    val MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 0

    lateinit var filesListTextView: TextView

    var alarmInterval = 60 * 60 * 1000

    val observer = object : FileObserver(ZIP_FILE_PATH, (FileObserver.CREATE or FileObserver.DELETE)) {
        override fun onEvent(event: Int, path: String?) {
            if (path?.endsWith(".zip") == true) {
                runOnUiThread({ refreshList() })
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Fabric.with(this, Crashlytics())

        setContentView(R.layout.activity_main)
        createNotificationChannel()
        filesListTextView = findViewById(R.id.filesList)

        findViewById<Button>(R.id.refreshButton).setOnClickListener { refreshList() }
        findViewById<Button>(R.id.uploadButton).setOnClickListener { forceUpload() }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                finish()
            } else {
                ActivityCompat.requestPermissions(this,
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE)
            }
        } else {
            setupSpinner()
        }
    }

    override fun onStart() {
        super.onStart()
        refreshList()
        observer.startWatching()
    }

    override fun onStop() {
        super.onStop()
        observer.stopWatching()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    setupSpinner()
                } else {
                    finish()
                }
            }
        }
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        Log.d(TAG, "onItemSelected $position")
        when (position) {
            0 -> alarmInterval = 15 * 60 * 1000 // / 60 / 5
            1 -> alarmInterval = 30 * 60 * 1000
            2 -> alarmInterval = 60 * 60 * 1000
        }

        val intent = Intent(applicationContext, WorkerService::class.java)
        intent.putExtra(ALARM_INTERVAL_KEY, alarmInterval)

        val preference = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        if (preference.getInt(ALARM_INTERVAL_POSITION_KEY, 2) != position) {
            startWorkerService(intent)
        } else {
            if (PendingIntent.getService(applicationContext, 0, intent, PendingIntent.FLAG_NO_CREATE) == null) {
                startWorkerService(intent)
            }
        }

        preference.edit().putInt(ALARM_INTERVAL_POSITION_KEY, position).apply()
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        Log.d(TAG, "onNothingSelected")
    }

    fun setupSpinner() {
        val spinner = findViewById<Spinner>(R.id.spinner)
        val adapter = ArrayAdapter.createFromResource(applicationContext, R.array.interval_array, android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        spinner.onItemSelectedListener = this
        spinner.setSelection(PreferenceManager.getDefaultSharedPreferences(applicationContext).getInt(ALARM_INTERVAL_POSITION_KEY, 2))
    }

    fun refreshList() {
        var text = ""

        val files = File(ZIP_FILE_PATH).listFiles()
        if (files != null) {
            for (file in files) {
                if (file.isFile) {
                    text += file.name + "\n"
                }
            }
        }
        filesListTextView.setText(text)
    }

    fun forceUpload() {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork: NetworkInfo? = cm.activeNetworkInfo
        if (activeNetwork?.isConnectedOrConnecting == true) {
            val intent = Intent(applicationContext, WorkerService::class.java)
            intent.putExtra(UPLOAD_ONLY_KEY, true)
            startService(intent)
        } else {
            Toast.makeText(applicationContext, "No Network", Toast.LENGTH_SHORT).show()
        }
    }

    fun startWorkerService(intent: Intent) {
        startService(intent)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "general"
            val description = "general"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(GENERAL_NOTIFICATION_CHANNEL_ID, name, importance)
            channel.description = description
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
