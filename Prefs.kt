package world.gatepay.sms

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** টোকেন, সিম তালিকা ও অপেক্ষমাণ (offline) এসএমএস সংরক্ষণ */
object Prefs {
    private fun p(c: Context) = c.getSharedPreferences("gatepay_sms", Context.MODE_PRIVATE)
    fun token(c: Context) = p(c).getString("token", "") ?: ""
    fun setToken(c: Context, t: String) = p(c).edit().putString("token", t.trim()).apply()

    /** সিম: slot (0/1), number (১১ ডিজিট), type (personal/agent/merchant) */
    fun sims(c: Context): JSONArray = JSONArray(p(c).getString("sims", "[]"))
    fun addSim(c: Context, slot: Int, number: String, type: String) {
        val arr = sims(c); val out = JSONArray()
        for (i in 0 until arr.length()) if (arr.getJSONObject(i).getInt("slot") != slot) out.put(arr.get(i))
        out.put(JSONObject().put("slot", slot).put("number", number).put("type", type))
        p(c).edit().putString("sims", out.toString()).apply()
    }
    fun simFor(c: Context, slot: Int): JSONObject? {
        val a = sims(c); for (i in 0 until a.length()) if (a.getJSONObject(i).getInt("slot") == slot) return a.getJSONObject(i)
        return if (a.length() == 1) a.getJSONObject(0) else null
    }

    @Synchronized fun queue(c: Context): JSONArray = JSONArray(p(c).getString("queue", "[]"))
    @Synchronized fun enqueue(c: Context, item: JSONObject) {
        val q = queue(c); q.put(item)
        while (q.length() > 500) q.remove(0)
        p(c).edit().putString("queue", q.toString()).apply()
    }
    @Synchronized fun setQueue(c: Context, q: JSONArray) = p(c).edit().putString("queue", q.toString()).apply()

    fun log(c: Context): String = p(c).getString("log", "") ?: ""
    fun addLog(c: Context, line: String) {
        val lines = (line + "\n" + log(c)).lines().take(30).joinToString("\n")
        p(c).edit().putString("log", lines).apply()
    }

    fun trxList(c: Context): List<Trx> { val a = JSONArray(p(c).getString("trx", "[]")); return (0 until a.length()).map { Trx.from(a.getJSONObject(it)) } }
    @Synchronized fun addTrx(c: Context, t: Trx) {
        val out = JSONArray().put(t.json()); trxList(c).take(199).forEach { out.put(it.json()) }
        p(c).edit().putString("trx", out.toString()).putInt("today_ok", p(c).getInt("today_ok", 0) + if (t.status == "সফল") 1 else 0).apply()
    }
    fun bio(c: Context) = p(c).getBoolean("bio", false)
    fun setBio(c: Context, v: Boolean) = p(c).edit().putBoolean("bio", v).apply()
    fun sound(c: Context) = p(c).getBoolean("sound", true)
    fun setSound(c: Context, v: Boolean) = p(c).edit().putBoolean("sound", v).apply()
    fun removeSim(c: Context, slot: Int) { val a = sims(c); val o = JSONArray(); for (i in 0 until a.length()) if (a.getJSONObject(i).getInt("slot") != slot) o.put(a.get(i)); p(c).edit().putString("sims", o.toString()).apply() }
}
