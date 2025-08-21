package com.bizclimb.nextdoorpush.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

class App : Application() {
  override fun onCreate() {
    super.onCreate()
    if (Build.VERSION.SDK_INT >= 26) {
      val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      val ch = NotificationChannel(
        Const.CHANNEL_ID,
        getString(R.string.notif_channel_name),
        NotificationManager.IMPORTANCE_HIGH
      )
      ch.description = getString(R.string.notif_channel_desc)
      mgr.createNotificationChannel(ch)
    }
  }
}
