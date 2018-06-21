package com.aoitek.logtool

import android.os.Environment
import java.io.File

@JvmField
val MSG_SCHEDULING_START = 0
@JvmField
val MSG_SCHEDULING_STOP = 1
@JvmField
val MSG_REFRESH_LIST = 2

@JvmField
val GENERAL_NOTIFICATION_CHANNEL_ID = "general"

@JvmField
val ALARM_SETUP_ACTION = "${BuildConfig.APPLICATION_ID}.ALARM_SETUP"

@JvmField
val MESSENGER_INTENT_KEY = "${BuildConfig.APPLICATION_ID}.MESSENGER_INTENT_KEY"
@JvmField
val ZIP_FILE_SERIAL_KEY = "${BuildConfig.APPLICATION_ID}.ZIP_FILE_SERIAL_KEY"
@JvmField
val ALARM_INTERVAL_KEY = "${BuildConfig.APPLICATION_ID}.ALARM_INTERVAL_KEY"
@JvmField
val ALARM_INTERVAL_POSITION_KEY = "${BuildConfig.APPLICATION_ID}.ALARM_INTERVAL_POSITION_KEY"

@JvmField
val LAST_UPLOADED_FILE_PREFS_KEY = "${BuildConfig.APPLICATION_ID}.LAST_UPLOADED_FILE_KEY"
@JvmField
val PACKED_FILE_PREFS_KEY = "${BuildConfig.APPLICATION_ID}.PACKED_FILE_KEY"

@JvmField
val FILE_PACKED_FIREBASE_EVENT = "file_packed"
@JvmField
val FILE_UPLOADED_FIREBASE_EVENT = "file_uploaded"
@JvmField
val FILE_NAME_FIREBASE_KEY = "file_name"

@JvmField
val UPLOAD_ONLY_KEY = "upload_only"

@JvmField
val LOGS_FILE_PATH = "/mnt/sdcard/sdk_logs/"

@JvmField
val ZIP_FILE_PATH = Environment.getExternalStorageDirectory().path + File.separator + "aoitek_logs"