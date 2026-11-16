package world.gatepay.sms

import org.json.JSONObject

/** এসএমএস থেকে প্রদর্শনের জন্য তথ্য বের করা (শুধু দেখানোর জন্য; যাচাই সার্ভারে হয়) */
data class Trx(val sender: String, val gateway: String, val amount: String, val trxId: String, val status: String, val time: Long, val body: String) {
    fun json() = JSONObject().put("sender", sender).put("gateway", gateway).put("amount", amount)
        .put("trx", trxId).put("status", status).put("time", time).put("body", body)
    companion object {
        fun from(o: JSONObject) = Trx(o.optString("sender"), o.optString("gateway"), o.optString("amount"),
            o.optString("trx"), o.optString("status"), o.optLong("time"), o.optString("body"))
        fun parse(sender: String, body: String, status: String, time: Long): Trx {
            val s = (sender + " " + body).lowercase()
            val gw = when { "bkash" in s -> "bKash"; "nagad" in s -> "Nagad"; "rocket" in s || "16216" in s -> "Rocket"; "upay" in s -> "Upay"; else -> "SMS" }
            val amt = Regex("""(?:tk|৳|bdt)\s?([\d,]+(?:\.\d{1,2})?)""", RegexOption.IGNORE_CASE).find(body)?.groupValues?.get(1)?.replace(",", "") ?: ""
            val trx = Regex("""(?:trxid|txnid|trx id|transaction id)[:\s]*([A-Z0-9]{6,20})""", RegexOption.IGNORE_CASE).find(body)?.groupValues?.get(1) ?: ""
            return Trx(sender, gw, amt, trx, status, time, body)
        }
    }
}
