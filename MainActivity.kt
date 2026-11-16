package world.gatepay.sms

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.*
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.hardware.biometrics.BiometricPrompt
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.view.*
import android.view.animation.PathInterpolator
import android.widget.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread

/** ডার্ক ফিনটেক UI: উপরে ⋮ মেনু, নিচে ৩টি ট্যাব (হোম / লেনদেন / নম্বর), স্মুথ পেজ অ্যানিমেশন */
class MainActivity : Activity() {
    private val bg = Color.parseColor("#070F22"); private val card = Color.parseColor("#0F1C3A")
    private val line = Color.parseColor("#1F2E52"); private val cyan = Color.parseColor("#22D3EE")
    private val blue = Color.parseColor("#3B82F6"); private val txt = Color.parseColor("#F1F5F9"); private val sub = Color.parseColor("#94A3B8")
    private val ease = PathInterpolator(0.22f, 1f, 0.36f, 1f)
    private lateinit var content: FrameLayout
    private val tabs = mutableListOf<TextView>()
    private var tab = 0
    private val ui = Handler(Looper.getMainLooper())
    private val updates = object : BroadcastReceiver() { override fun onReceive(c: Context, i: Intent) { show(tab, false) } }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        window.statusBarColor = bg; window.navigationBarColor = card
        if (Prefs.bio(this) && Build.VERSION.SDK_INT >= 28) lock() else build()
    }

    private fun lock() {
        setContentView(FrameLayout(this).apply { setBackgroundColor(bg) })
        BiometricPrompt.Builder(this).setTitle("GatePay SMS আনলক").setSubtitle("ফিঙ্গারপ্রিন্ট দিন")
            .setNegativeButton("বাতিল", mainExecutor) { _, _ -> finish() }.build()
            .authenticate(CancellationSignal(), mainExecutor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(r: BiometricPrompt.AuthenticationResult) = build()
                override fun onAuthenticationError(code: Int, s: CharSequence) { finish() }
            })
    }

    private fun build() {
        Notifier.ensure(this)
        if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.POST_NOTIFICATIONS), 1)
        else requestPermissions(arrayOf(Manifest.permission.RECEIVE_SMS), 1)
        if (Prefs.token(this).isNotEmpty()) ForwarderService.start(this)

        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(bg) }
        // টপ বার
        val top = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(dp(18), dp(14), dp(8), dp(10)) }
        top.addView(TextView(this).apply { text = "Gate›Pay SMS"; textSize = 20f; setTextColor(txt); typeface = Typeface.DEFAULT_BOLD }, LinearLayout.LayoutParams(0, -2, 1f))
        top.addView(TextView(this).apply { text = "●"; setTextColor(if (Prefs.token(context).isEmpty()) sub else Color.parseColor("#22C55E")); setPadding(dp(8), 0, dp(8), 0) })
        val more = TextView(this).apply { text = "⋮"; textSize = 24f; setTextColor(txt); setPadding(dp(14), 0, dp(14), 0); setOnClickListener { menu(it) } }
        press(more); top.addView(more); root.addView(top)
        content = FrameLayout(this); root.addView(content, LinearLayout.LayoutParams(-1, 0, 1f))
        // বটম নেভ
        val nav = LinearLayout(this).apply { setBackgroundColor(card); setPadding(dp(8), dp(8), dp(8), dp(10)) }
        listOf("⌂\nহোম", "⇅\nলেনদেন", "☎\nনম্বর").forEachIndexed { i, s ->
            val t = TextView(this).apply { text = s; gravity = Gravity.CENTER; textSize = 12f; setPadding(0, dp(6), 0, dp(6)); setOnClickListener { show(i, true) } }
            press(t); tabs += t; nav.addView(t, LinearLayout.LayoutParams(0, -2, 1f))
        }
        root.addView(nav)
        setContentView(root)
        show(intent.getIntExtra("tab", 0), false)
    }

    /** পেজ বদলের সময় স্লাইড + ফেড (৩২০ms) */
    private fun show(i: Int, animate: Boolean) {
        val dir = if (i >= tab) 1 else -1; tab = i
        tabs.forEachIndexed { k, t -> t.setTextColor(if (k == i) cyan else sub); t.background = if (k == i) round(Color.parseColor("#132650"), 14) else null }
        val page = when (i) { 0 -> home(); 1 -> history(); else -> numbers() }
        val old = if (content.childCount > 0) content.getChildAt(0) else null
        content.addView(page)
        if (animate && old != null) {
            page.alpha = 0f; page.translationX = dp(36f) * dir
            page.animate().alpha(1f).translationX(0f).setDuration(320).setInterpolator(ease).withLayer().start()
            old.animate().alpha(0f).translationX(-dp(24f) * dir).setDuration(200).withLayer().withEndAction { content.removeView(old) }.start()
        } else old?.let { content.removeView(it) }
    }

    private fun home(): View = scroll {
        val list = Prefs.trxList(this@MainActivity)
        val today = list.count { isToday(it.time) }
        val online = Prefs.token(this@MainActivity).isNotEmpty()
        it.addView(hero(if (online) "অনলাইন • ব্যাকগ্রাউন্ডে চালু" else "টোকেন দিন", "আজ $today টি লেনদেন • অপেক্ষমাণ ${Prefs.queue(this@MainActivity).length()}"))
        val row = LinearLayout(this@MainActivity)
        row.addView(action("⇅  লেনদেন") { show(1, true) }, LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = dp(6) })
        row.addView(action("＋  নম্বর যোগ") { show(2, true) }, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(6) })
        it.addView(row, lp(top = 12))
        it.addView(label("সাম্প্রতিক লেনদেন"))
        if (list.isEmpty()) it.addView(muted("এখনো কোনো লেনদেন নেই"))
        list.take(5).forEachIndexed { k, t -> it.addView(trxCard(t).also { v -> enter(v, k) }, lp(top = 8)) }
        if (!online) { it.addView(label("টোকেন (কপি-পেস্ট)")); it.addView(tokenBox()) }
    }

    private fun history(): View = scroll {
        it.addView(label("সব লেনদেন"))
        val list = Prefs.trxList(this@MainActivity)
        if (list.isEmpty()) it.addView(muted("নতুন পেমেন্ট এসএমএস এলে এখানে দেখাবে"))
        list.forEachIndexed { k, t -> it.addView(trxCard(t).also { v -> if (k < 8) enter(v, k) }, lp(top = 8)) }
    }

    private fun numbers(): View = scroll {
        it.addView(label("সিম ও নম্বর"))
        val sims = Prefs.sims(this@MainActivity)
        for (i in 0 until sims.length()) {
            val o = sims.getJSONObject(i)
            val c = box(); c.addView(title("SIM ${o.getInt("slot") + 1} • ${o.optString("number")}"))
            c.addView(muted("ধরন: " + mapOf("personal" to "Personal", "agent" to "Agent", "merchant" to "Merchant")[o.optString("type")]))
            c.addView(action("মুছুন") { Prefs.removeSim(this@MainActivity, o.getInt("slot")); show(2, false) }, lp(top = 8))
            it.addView(c, lp(top = 8))
        }
        val f = box(); f.addView(title("＋ নতুন নম্বর"))
        val slot = spinner(listOf("SIM 1", "SIM 2")); val type = spinner(listOf("Personal", "Agent", "Merchant"))
        val num = EditText(this@MainActivity).apply { hint = "01XXXXXXXXX"; inputType = android.text.InputType.TYPE_CLASS_PHONE; setTextColor(txt); setHintTextColor(sub); filters = arrayOf(android.text.InputFilter.LengthFilter(11)) }
        f.addView(slot); f.addView(type); f.addView(num)
        f.addView(action("সংরক্ষণ") {
            val n = num.text.toString().trim()
            if (!Regex("^01[3-9]\\d{8}$").matches(n)) toast("সঠিক ১১ ডিজিটের নম্বর দিন")
            else { Prefs.addSim(this@MainActivity, slot.selectedItemPosition, n, type.selectedItem.toString().lowercase()); toast("সংরক্ষিত"); show(2, false) }
        }, lp(top = 10))
        it.addView(f, lp(top = 12))
    }

    private fun menu(anchor: View) {
        val m = PopupMenu(this, anchor)
        m.menu.add(0, 1, 0, (if (Prefs.bio(this)) "✓ " else "") + "ফিঙ্গারপ্রিন্ট লক")
        m.menu.add(0, 2, 0, (if (Prefs.sound(this)) "✓ " else "") + "পপআপ ও সাউন্ড")
        m.menu.add(0, 3, 0, "সার্ভার পিং টেস্ট")
        m.menu.add(0, 4, 0, "ব্যাটারি অপ্টিমাইজেশন বন্ধ")
        m.menu.add(0, 5, 0, "নোটিফিকেশন সাউন্ড সেটিং")
        m.menu.add(0, 6, 0, "টোকেন পরিবর্তন")
        m.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> if (Build.VERSION.SDK_INT < 28) toast("এই ফোনে সমর্থিত নয়") else { Prefs.setBio(this, !Prefs.bio(this)); toast(if (Prefs.bio(this)) "ফিঙ্গারপ্রিন্ট লক চালু" else "বন্ধ") }
                2 -> { Prefs.setSound(this, !Prefs.sound(this)); toast(if (Prefs.sound(this)) "চালু" else "বন্ধ") }
                3 -> ping()
                4 -> batt()
                5 -> if (Build.VERSION.SDK_INT >= 26) startActivity(Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName).putExtra(Settings.EXTRA_CHANNEL_ID, Notifier.CH_TRX))
                6 -> { Prefs.setToken(this, ""); show(0, true) }
            }; true
        }; m.show()
    }

    private fun ping() {
        val t = Prefs.token(this); if (t.isEmpty()) { toast("আগে টোকেন দিন"); return }
        toast("পিং হচ্ছে…")
        thread { val s = SystemClock.elapsedRealtime(); val r = Api.heartbeat(t); val ms = SystemClock.elapsedRealtime() - s
            ui.post { toast(if (r?.first == 200) "${ms}ms • সংযুক্ত" else "সংযোগ ব্যর্থ (${r?.first ?: "নেট নেই"})") } }
    }

    @SuppressLint("BatteryLife")
    private fun batt() {
        val pm = getSystemService(PowerManager::class.java)
        if (pm.isIgnoringBatteryOptimizations(packageName)) toast("ইতিমধ্যে বন্ধ আছে")
        else try { startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName"))) } catch (_: Exception) {}
    }

    private fun tokenBox(): View {
        val c = box()
        val e = EditText(this).apply { hint = "smsd_..."; setTextColor(txt); setHintTextColor(sub) }
        c.addView(e)
        c.addView(action("পেস্ট করুন") { (getSystemService(ClipboardManager::class.java).primaryClip?.getItemAt(0)?.text)?.let { e.setText(it) } }, lp(top = 8))
        c.addView(action("চালু করুন") {
            val v = e.text.toString().trim(); if (!v.startsWith("smsd_")) { toast("সঠিক টোকেন দিন"); return@action }
            Prefs.setToken(this, v); ForwarderService.start(this); toast("চালু হয়েছে"); show(0, true)
        }, lp(top = 8))
        return c
    }

    // ---------- ছোট UI হেল্পার ----------
    private fun trxCard(t: Trx): View {
        val c = box(); val r = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        r.addView(TextView(this).apply { text = t.gateway; setTextColor(cyan); textSize = 12f; typeface = Typeface.DEFAULT_BOLD; background = round(Color.parseColor("#0B2A44"), 10); setPadding(dp(10), dp(4), dp(10), dp(4)) })
        r.addView(TextView(this).apply { text = "   ৳" + t.amount.ifEmpty { "—" }; setTextColor(txt); textSize = 17f; typeface = Typeface.DEFAULT_BOLD }, LinearLayout.LayoutParams(0, -2, 1f))
        r.addView(TextView(this).apply { text = t.status; setTextColor(if (t.status == "সফল") Color.parseColor("#4ADE80") else Color.parseColor("#FBBF24")); textSize = 12f })
        c.addView(r); c.addView(muted("TrxID ${t.trxId.ifEmpty { "—" }} • ${SimpleDateFormat("dd MMM, hh:mm a", Locale.US).format(Date(t.time))}"))
        c.setOnClickListener { android.app.AlertDialog.Builder(this).setTitle("${t.gateway} • ৳${t.amount}").setMessage("প্রেরক: ${t.sender}\nTrxID: ${t.trxId}\nস্ট্যাটাস: ${t.status}\n\n${t.body}").setPositiveButton("ঠিক আছে", null).show() }
        press(c); return c
    }
    private fun hero(a: String, b: String) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(18), dp(18), dp(18))
        background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(Color.parseColor("#0E3A6B"), Color.parseColor("#0B1B3A"))).apply { cornerRadius = dp(20f) }
        addView(TextView(context).apply { text = a; setTextColor(txt); textSize = 18f; typeface = Typeface.DEFAULT_BOLD })
        addView(TextView(context).apply { text = b; setTextColor(Color.parseColor("#BAE6FD")); textSize = 13f; setPadding(0, dp(6), 0, 0) })
        alpha = 0f; scaleX = 0.97f; scaleY = 0.97f; animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(380).setInterpolator(ease).start()
    }
    private fun scroll(f: (LinearLayout) -> Unit): View { val l = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(4), dp(16), dp(24)) }; f(l); return ScrollView(this).apply { addView(l); isVerticalScrollBarEnabled = false; overScrollMode = View.OVER_SCROLL_NEVER } }
    private fun box() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(14), dp(14), dp(14)); background = round(card, 16).apply { setStroke(dp(1), line) } }
    private fun action(s: String, f: () -> Unit) = TextView(this).apply { text = s; gravity = Gravity.CENTER; setTextColor(Color.WHITE); textSize = 14f; typeface = Typeface.DEFAULT_BOLD; setPadding(0, dp(13), 0, dp(13)); background = round(blue, 14); setOnClickListener { it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); f() } }.also { press(it) }
    private fun label(s: String) = TextView(this).apply { text = s; setTextColor(sub); textSize = 13f; setPadding(dp(2), dp(20), 0, dp(4)) }
    private fun title(s: String) = TextView(this).apply { text = s; setTextColor(txt); textSize = 15f; typeface = Typeface.DEFAULT_BOLD }
    private fun muted(s: String) = TextView(this).apply { text = s; setTextColor(sub); textSize = 12f; setPadding(0, dp(6), 0, 0) }
    private fun spinner(items: List<String>) = Spinner(this).apply { adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, items) }
    private fun round(c: Int, r: Int) = GradientDrawable().apply { setColor(c); cornerRadius = dp(r.toFloat()) }
    private fun lp(top: Int) = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(top) }
    /** ট্যাপ করলেই সাথে সাথে 0.97x স্কেল — কোনো দেরি নেই */
    @SuppressLint("ClickableViewAccessibility")
    private fun press(v: View) = v.setOnTouchListener { x, e ->
        when (e.action) { MotionEvent.ACTION_DOWN -> x.animate().scaleX(0.97f).scaleY(0.97f).setDuration(70).start()
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> x.animate().scaleX(1f).scaleY(1f).setDuration(140).setInterpolator(ease).start() }; false }
    private fun enter(v: View, i: Int) { v.alpha = 0f; v.translationY = dp(14f); v.animate().alpha(1f).translationY(0f).setStartDelay(40L * i).setDuration(300).setInterpolator(ease).start() }
    private fun isToday(t: Long) = SimpleDateFormat("yyyyMMdd", Locale.US).run { format(Date(t)) == format(Date()) }
    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun dp(v: Float) = v * resources.displayMetrics.density

    override fun onResume() { super.onResume()
        val f = IntentFilter(ForwarderService.ACTION_UPDATE)
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(updates, f, Context.RECEIVER_NOT_EXPORTED) else registerReceiver(updates, f) }
    override fun onPause() { try { unregisterReceiver(updates) } catch (_: Exception) {}; super.onPause() }
}
