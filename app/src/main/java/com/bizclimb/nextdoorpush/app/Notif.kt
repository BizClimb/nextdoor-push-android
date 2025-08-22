package com.bizclimb.nextdoorpush.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object Notif {
  private const val TAG = "NDPush"

  fun ensureChannel(ctx: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val mgr = ctx.getSystemService(NotificationManager::class.java)
    val id = Const.CHANNEL_ID
    val existing = mgr.getNotificationChannel(id)
    if (existing == null) {
      val ch = NotificationChannel(
        id,
        "Nextdoor Push",
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description = "Approve and close actions"
        enableVibration(true)
        setShowBadge(false)
      }
      mgr.createNotificationChannel(ch)
      Log.d(TAG, "Created notification channel $id")
    }
  }

  /** returns true if we can post */
  fun canPost(ctx: Context): Boolean {
    // App wide switch
    val enabled = NotificationManagerCompat.from(ctx).areNotificationsEnabled()
    if (!enabled) {
      Log.w(TAG, "Notifications disabled at app level")
      return false
    }
    // Runtime permission on Android 13+
    if (Build.VERSION.SDK_INT >= 33) {
      val granted = ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
      if (!granted) {
        Log.w(TAG, "POST_NOTIFICATIONS permission not granted on Android 13+")
        return false
      }
    }
    // Channel state on Android O+
    if (Build.VERSION.SDK_INT >= 26) {
      val mgr = ctx.getSystemService(NotificationManager::class.java)
      val ch = mgr.getNotificationChannel(Const.CHANNEL_ID)
      if (ch == null) {
        Log.w(TAG, "Channel ${Const.CHANNEL_ID} missing")
        return false
      }
      if (ch.importance == NotificationManager.IMPORTANCE_NONE) {
        Log.w(TAG, "Channel ${Const.CHANNEL_ID} importance=NONE (blocked)")
        return false
      }
    }
    return true
  }

  /** Centralized notify with guards and logging */
  fun notify(ctx: Context, id: Int, builder: NotificationCompat.Builder) {
    ensureChannel(ctx)
    if (!canPost(ctx)) {
      // Optional: deep link to settings you can open from an activity when user next opens the app
      Log.w(TAG, "notify aborted: cannot post (see warnings above)")
      return
    }
    NotificationManagerCompat.from(ctx).notify(id, builder.build())
  }

  /** Intent to open app notification settings or channel settings */
  fun settingsIntent(ctx: Context): Intent {
    return if (Build.VERSION.SDK_INT >= 26) {
      Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
        .putExtra(Settings.EXTRA_CHANNEL_ID, Const.CHANNEL_ID)
    } else {
      Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
    }
  }
}
