package world.gatepay.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import org.json.JSONObject

/** অ্যাপ বন্ধ/রিসেন্ট থেকে সরানো থাকলেও Android এই রিসিভার চালায় */
class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val parts = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (parts.isEmpty()) return
        val sender = parts[0].originatingAddress ?: return
        val body = parts.joinToString("") { it.messageBody ?: "" }
        val slot = intent.extras?.let { it.getInt("slot", it.getInt("simSlot", it.getInt("android.telephony.extra.SLOT_INDEX", 0))) } ?: 0
        val sim = Prefs.simFor(c, slot)
        val item = JSONObject().put("sender", sender).put("message", body)
            .put("sim_number", sim?.optString("number") ?: "")
            .put("account_type", sim?.optString("type") ?: "")
            .put("received_at", System.currentTimeMillis())
        Prefs.enqueue(c, item)
        ForwarderService.start(c) // সার্ভিস কিউ খালি করবে
    }
}
