package world.gatepay.sms

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object Api {
    /** সার্ভারে পাঠায়; সফল হলে (code, body) ফেরত দেয়, নেটওয়ার্ক ব্যর্থ হলে null */
    fun post(token: String, payload: JSONObject): Pair<Int, String>? = try {
        val con = URL(BuildConfig.API_URL).openConnection() as HttpURLConnection
        con.requestMethod = "POST"; con.connectTimeout = 15000; con.readTimeout = 20000; con.doOutput = true
        con.setRequestProperty("Authorization", "Bearer $token")
        con.setRequestProperty("Content-Type", "application/json")
        con.outputStream.use { it.write(payload.toString().toByteArray()) }
        val code = con.responseCode
        val body = (if (code < 400) con.inputStream else con.errorStream)?.bufferedReader()?.readText() ?: ""
        con.disconnect(); code to body
    } catch (e: Exception) { null }

    fun heartbeat(token: String) = post(token, JSONObject().put("type", "heartbeat"))
}
