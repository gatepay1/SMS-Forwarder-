package world.gatepay.sms

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build

/** নতুন ট্রানজ্যাকশনে স্ক্রিনের ওপরে পপআপ (heads-up) + সাউন্ড + ভাইব্রেশন */
object Notifier {
    const val CH_TRX = "gatepay_trx_v2"

    fun ensure(c: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = c.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CH_TRX) != null) return
        val ch = NotificationChannel(CH_TRX, "নতুন ট্রানজ্যাকশন", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "পেমেন্ট এসএমএস এলে পপআপ ও সাউন্ড"
            enableVibration(true); vibrationPattern = longArrayOf(0, 180, 90, 180)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
        }
        nm.createNotificationChannel(ch)
    }

    fun transaction(c: Context, t: Trx) {
        ensure(c)
        val pi = PendingIntent.getActivity(c, t.time.toInt(), Intent(c, MainActivity::class.java).putExtra("tab", 1),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val title = "${t.gateway} • ৳${t.amount.ifEmpty { "—" }}"
        val text = "TrxID ${t.trxId.ifEmpty { "—" }} • ${t.status}"
        val b = (if (Build.VERSION.SDK_INT >= 26) Notification.Builder(c, CH_TRX) else Notification.Builder(c)
            .setPriority(Notification.PRIORITY_HIGH).setDefaults(Notification.DEFAULT_ALL))
            .setSmallIcon(android.R.drawable.stat_notify_more)
            .setContentTitle(title).setContentText(text)
            .setStyle(Notification.BigTextStyle().bigText("$text\nপ্রেরক: ${t.sender}"))
            .setCategory(Notification.CATEGORY_MESSAGE)
            .setAutoCancel(true).setContentIntent(pi).setShowWhen(true)
        c.getSystemService(NotificationManager::class.java).notify(1000 + (t.time % 100000).toInt(), b.build())
    }
}
