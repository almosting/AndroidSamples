package com.almosting.sample.service

import android.annotation.TargetApi
import android.app.Notification.Builder
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.IBinder
import android.text.TextUtils
import android.util.Log
import com.almosting.sample.R.mipmap
import com.almosting.sample.ScreenRecorderActivity
import com.almosting.screenrecorder.R.string
import com.almosting.screenrecorder.media.MediaAudioEncoder
import com.almosting.screenrecorder.media.MediaEncoder
import com.almosting.screenrecorder.media.MediaEncoder.MediaEncoderCallback
import com.almosting.screenrecorder.media.MediaMuxerWrapper
import com.almosting.screenrecorder.media.MediaScreenEncoder
import com.almosting.toolbox.utils.BuildCheck.isLollipop
import com.almosting.toolbox.utils.FileUtils
import java.io.IOException

/**
 * Created by w.feng on 2018/10/11
 * Email: fengweisb@gmail.com
 */
class ScreenRecorderService : Service() {
  companion object {
    private const val TAG = "ScreenRecorderService"
    private const val APP_DIR_NAME = "ScreenRecorder"
    private const val BASE = "com.fengwei23.service.ScreenRecorderService."
    const val ACTION_START = BASE + "ACTION_START"
    const val ACTION_STOP = BASE + "ACTION_STOP"
    const val ACTION_PAUSE = BASE + "ACTION_PAUSE"
    const val ACTION_RESUME = BASE + "ACTION_RESUME"
    const val ACTION_QUERY_STATUS =
      BASE + "ACTION_QUERY_STATUS"
    const val ACTION_QUERY_STATUS_RESULT =
      BASE + "ACTION_QUERY_STATUS_RESULT"
    const val EXTRA_RESULT_CODE =
      BASE + "EXTRA_RESULT_CODE"
    const val EXTRA_QUERY_RESULT_RECORDING =
      BASE + "EXTRA_QUERY_RESULT_RECORDING"
    const val EXTRA_QUERY_RESULT_PAUSING =
      BASE + "EXTRA_QUERY_RESULT_PAUSING"
    private val NOTIFICATION = string.app_name
    private val sSync = Any()
    private var sMuxer: MediaMuxerWrapper? = null

    /**
     * callback methods from encoder
     */
    private val mMediaEncoderListener: MediaEncoderCallback = object : MediaEncoderCallback {
      override fun onPrepared(encoder: MediaEncoder?) {
        Log.v(TAG, "onPrepared:encoder=$encoder")
      }

      override fun onStopped(encoder: MediaEncoder?) {
        Log.v(TAG, "onStopped:encoder=$encoder")
      }
    }

    init {
      FileUtils.DIR_NAME = APP_DIR_NAME
    }
  }

  private lateinit var mMediaProjectionManager: MediaProjectionManager
  private lateinit var mNotificationManager: NotificationManager
  override fun onCreate() {
    super.onCreate()
    Log.v(TAG, "onCreate:")
    if (isLollipop()) {
      mMediaProjectionManager =
        getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }
    mNotificationManager =
      getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    showNotification(TAG)
  }

  override fun onDestroy() {
    Log.v(TAG, "onDestroy:")
    super.onDestroy()
  }

  override fun onBind(intent: Intent): IBinder? {
    return null
  }

  override fun onStartCommand(
    intent: Intent,
    flags: Int,
    startId: Int
  ): Int {
    Log.v(TAG, "onStartCommand:intent=$intent")
    var result = START_STICKY
    when (val action = intent.action) {
      ACTION_START -> {
        startScreenRecord(intent)
        updateStatus()
      }
      ACTION_QUERY_STATUS -> {
        if (!updateStatus()) {
          stopSelf()
          result = START_NOT_STICKY
        }
      }
      ACTION_PAUSE -> {
        pauseScreenRecord()
      }

      ACTION_RESUME -> {
        resumeScreenRecord()
      }
      ACTION_STOP -> {
        stopScreenRecord()
        updateStatus()
        result = START_NOT_STICKY
      }
      else -> {
        if (TextUtils.isEmpty(action)) {
          stopScreenRecord()
          updateStatus()
          result = START_NOT_STICKY
        }
      }
    }
    return result
  }

  private fun updateStatus(): Boolean {
    var isRecording: Boolean
    var isPausing: Boolean
    synchronized(sSync) {
      isRecording = sMuxer != null
      isPausing = isRecording && sMuxer!!.isPaused()
    }
    val result = Intent()
    result.action = ACTION_QUERY_STATUS_RESULT
    result.putExtra(EXTRA_QUERY_RESULT_RECORDING, isRecording)
    result.putExtra(EXTRA_QUERY_RESULT_PAUSING, isPausing)
    Log.v(TAG, "sendBroadcast:isRecording=$isRecording,isPausing=$isPausing")
    sendBroadcast(result)
    return isRecording
  }

  /**
   * start screen recording as .mp4 file
   */
  @TargetApi(value = VERSION_CODES.LOLLIPOP)
  private fun startScreenRecord(intent: Intent) {
    Log.v(
      TAG,
      "startScreenRecord:sMuxer=$sMuxer"
    )
    synchronized(sSync) {
      if (sMuxer == null) {
        val resultCode =
          intent.getIntExtra(EXTRA_RESULT_CODE, 0)
        // get MediaProjection
        val projection = mMediaProjectionManager.getMediaProjection(resultCode, intent)
        if (projection != null) {
          val metrics = resources.displayMetrics
          var width = metrics.widthPixels
          var height = metrics.heightPixels
          if (width > height) {
            // 横屏
            val scaleX = width / 1920f
            val scaleY = height / 1080f
            val scale = Math.max(scaleX, scaleY)
            width = (width / scale).toInt()
            height = (height / scale).toInt()
          } else {
            // 竖屏
            val scaleX = width / 1080f
            val scaleY = height / 1920f
            val scale = Math.max(scaleX, scaleY)
            width = (width / scale).toInt()
            height = (height / scale).toInt()
          }
          Log.v(
            TAG,
            String.format(
              "startRecording:(%d,%d)(%d,%d)",
              metrics.widthPixels,
              metrics.heightPixels,
              width,
              height
            )
          )
          try {
            // if you record audio only, ".m4a" is also OK.
            sMuxer = MediaMuxerWrapper(this, ".mp4")
            // for screen capturing
            MediaScreenEncoder(
              sMuxer,
              mMediaEncoderListener,
              projection,
              width,
              height,
              metrics.densityDpi,
              800 * 1024,
              15
            )
            // for audio capturing
            MediaAudioEncoder(sMuxer, mMediaEncoderListener)
            sMuxer!!.prepare()
            sMuxer!!.startRecording()
          } catch (e: IOException) {
            Log.e(TAG, "startScreenRecord:", e)
          }
        }
      }
    }
  }

  /**
   * stop screen recording
   */
  private fun stopScreenRecord() {
    Log.v(TAG, "stopScreenRecord:sMuxer=$sMuxer")
    synchronized(sSync) {
      if (sMuxer != null) {
        sMuxer!!.stopRecording()
        sMuxer = null
        // you should not wait here
      }
    }
    stopForeground(true)
    mNotificationManager.cancel(NOTIFICATION)
    stopSelf()
  }

  private fun pauseScreenRecord() {
    synchronized(sSync) {
      if (sMuxer != null) {
        sMuxer!!.pauseRecording()
      }
    }
  }

  private fun resumeScreenRecord() {
    synchronized(sSync) {
      if (sMuxer != null) {
        sMuxer!!.resumeRecording()
      }
    }
  }
  //================================================================================
  /**
   * helper method to show/change message on notification area
   * and set this service as foreground service to keep alive as possible as this can.
   */
  private fun showNotification(text: CharSequence) {
    Log.v(TAG, "showNotification:$text")
    // Set the info for the views that show in the notification panel.
    val builder: Builder = Builder(this)
      .setSmallIcon(mipmap.ic_launcher)
      .setTicker(text)
      .setWhen(System.currentTimeMillis())
      .setContentTitle(getText(string.app_name))
      .setContentText(text)
      .setContentIntent(createPendingIntent())
    if (VERSION.SDK_INT >= VERSION_CODES.O) {
      val channel = NotificationChannel(
        "AndroidTest",
        "Channel1", NotificationManager.IMPORTANCE_DEFAULT
      )
      mNotificationManager.createNotificationChannel(channel)
      builder.setChannelId("AndroidTest")
    }
    val notification = builder.build()
    startForeground(NOTIFICATION, notification)
    // Send the notification.
    mNotificationManager.notify(NOTIFICATION, notification)
  }

  protected fun createPendingIntent(): PendingIntent {
    FileUtils.DIR_NAME = APP_DIR_NAME
    return PendingIntent.getActivity(this, 0, Intent(this, ScreenRecorderActivity::class.java), 0)
  }
}