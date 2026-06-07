package com.example.service.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val db = AppDatabase.getDatabase(context)
            val alarmDao = db.alarmDao()
            val scheduler = AlarmSchedulerImpl(context)

            CoroutineScope(Dispatchers.IO).launch {
                val alarms = alarmDao.getAllAlarms().first()
                alarms.forEach { alarm ->
                    if (alarm.isEnabled && alarm.timeInMillis > System.currentTimeMillis()) {
                        scheduler.schedule(alarm)
                    }
                }
            }
        }
    }
}
