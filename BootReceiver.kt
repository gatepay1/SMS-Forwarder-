package world.gatepay.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) { if (Prefs.token(c).isNotEmpty()) ForwarderService.start(c) }
}
