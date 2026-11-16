package world.gatepay.sms

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread

/** স্থায়ী নোটিফিকেশনসহ ফোরগ্রাউন্ড সার্ভিস: কিউ পাঠায়, প্রতি মিনিটে হার্টবিট দেয় */
class ForwarderService : Service() {
    @Volatile private var running = false

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) nm.createNotificationChannel(NotificationChannel(CH, "GatePay SMS সক্রিয়", NotificationManager.IMPORTANCE_LOW))
        val pi = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val n = (if (Build.VERSION.SDK_INT >= 26) Notification.Builder(this, CH) else Notification.Builder(this))
            .setContentTitle("GatePay SMS চালু আছে").setContentText("ব্যাকগ্রাউন্ডে পেমেন্ট এসএমএস যাচাই হচ্ছে")
            .setSmallIcon(android.R.drawable.stat_notify_chat).setOngoing(true).setContentIntent(pi).build()
        if (Build.VERSION.SDK_INT >= 29) startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC) else startForeground(1, n)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!running) { running = true; thread(name = "gatepay-loop") { loop() } }
        return START_STICKY // সিস্টেম মেরে ফেললে আবার চালু করবে
    }

    private fun loop() {
        var tick = 0
        while (running) {
            val token = Prefs.token(this)
            if (token.isNotEmpty()) {
                flush(token)
                if (tick % 12 == 0) {
                    val r = Api.heartbeat(token)
                    Prefs.addLog(this, "${now()} হার্টবিট: " + (if (r?.first == 200) "অনলাইন" else "অফলাইন"))
                }
            }
            tick++; Thread.sleep(5000)
        }
    }

    private fun flush(token: String) {
        val q = Prefs.queue(this); if (q.length() == 0) return
        val left = JSONArray()
        for (i in 0 until q.length()) {
            val item = q.getJSONObject(i)
            val r = Api.post(token, item)
            when {
                r == null || r.first == 429 || r.first >= 500 -> left.put(item) // পরে আবার চেষ্টা
                r.first == 401 -> { left.put(item); Prefs.addLog(this, "${now()} টোকেন অবৈধ"); }
                else -> {
                    val st = summary(r.second)
                    Prefs.addLog(this, "${now()} ${item.optString("sender")}: $st")
                    val t = Trx.parse(item.optString("sender"), item.optString("message"), if (r.first in 200..299 && st != "উপেক্ষিত") "সফল" else st, System.currentTimeMillis())
                    if (st != "উপেক্ষিত") { Prefs.addTrx(this, t); if (Prefs.sound(this)) Notifier.transaction(this, t); sendBroadcast(Intent(ACTION_UPDATE).setPackage(packageName)) }
                }
            }
        }
        // এর মধ্যে নতুন এসএমএস এলে সেগুলোও রাখা
        val latest = Prefs.queue(this)
        for (i in q.length() until latest.length()) left.put(latest.get(i))
        Prefs.setQueue(this, left)
    }

    private fun summary(b: String) = when {
        b.contains("auto_approved\":true") -> "পেমেন্ট স্বয়ংক্রিয়ভাবে অনুমোদিত"
        b.contains("stored") -> "জমা হয়েছে"
        b.contains("duplicate") -> "আগেই জমা"
        else -> "উপেক্ষিত"
    }
    private fun now() = SimpleDateFormat("dd/MM HH:mm", Locale.US).format(Date())

    override fun onTaskRemoved(rootIntent: Intent?) { start(this); super.onTaskRemoved(rootIntent) }
    override fun onDestroy() { running = false; super.onDestroy() }
    override fun onBind(i: Intent?): IBinder? = null

    companion object {
        const val CH = "gatepay_sms"
        const val ACTION_UPDATE = "world.gatepay.sms.UPDATE"
        fun start(c: Context) {
            val i = Intent(c, ForwarderService::class.java)
            try { if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(i) else c.startService(i) } catch (_: Exception) {}
        }
    }
}
