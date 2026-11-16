package world.gatepay.sms

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

/** ওয়েবসাইটের মতো Gate›Pay অ্যানিমেশন, নিচে "SMS" */
class SplashActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setBackgroundColor(Color.parseColor("#0B1B3A")) }
        val s = SpannableString("Gate›Pay")
        s.setSpan(ForegroundColorSpan(Color.WHITE), 0, 4, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        s.setSpan(ForegroundColorSpan(Color.parseColor("#22D3EE")), 4, 8, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        val word = TextView(this).apply { text = s; textSize = 40f; typeface = Typeface.DEFAULT_BOLD; alpha = 0f; scaleX = .85f; scaleY = .85f }
        val sms = TextView(this).apply { text = "SMS"; textSize = 18f; letterSpacing = .5f; setTextColor(Color.parseColor("#93C5FD")); alpha = 0f; gravity = Gravity.CENTER }
        box.addView(word); box.addView(sms); setContentView(box)
        word.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(600).start()
        sms.animate().alpha(1f).setStartDelay(450).setDuration(500).start()
        Handler(Looper.getMainLooper()).postDelayed({ startActivity(Intent(this, MainActivity::class.java)); finish() }, 1600)
    }
}
