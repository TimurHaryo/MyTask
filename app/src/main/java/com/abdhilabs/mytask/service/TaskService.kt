package com.abdhilabs.mytask.service

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.abdhilabs.mytask.R
import com.abdhilabs.mytask.data.model.Task
import com.abdhilabs.mytask.data.repository.TaskRepository
import com.abdhilabs.mytask.di.component.DaggerAppComponent
import com.abdhilabs.mytask.receiver.TaskReceiver
import com.abdhilabs.mytask.ui.activities.MainActivity
import com.abdhilabs.mytask.utils.DAILY_NOTIFICATION
import com.abdhilabs.mytask.utils.DateTimeFormatter
import com.abdhilabs.mytask.utils.UPDATE_NOTIFICATION
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

private const val FOREGROUND_ID = 1
private const val REQUEST_UPDATE_ID = 5


class TaskService : LifecycleService() {

    @Inject
    lateinit var repository: TaskRepository

    private var job = Job()
    private val coroutineScope = CoroutineScope(job + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        DaggerAppComponent.builder()
            .applicationContext(application)
            .build()
            .inject(this)
        scheduleUpdate()
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        foregroundSetup()
        getTaskUpdate()
        stopSelf()
        return START_STICKY
    }

    private fun foregroundSetup() {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0, notificationIntent, 0
        )
        val notification =
            NotificationCompat.Builder(this, getString(R.string.channel_foreground_id))
                .setContentTitle("Pemberitahuan")
                .setContentText("Sedang memeriksa tugas anda")
                .setSmallIcon(R.drawable.ic_document)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(pendingIntent)
                .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                getString(R.string.channel_foreground_id),
                getString(R.string.channel_foreground_name),
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(serviceChannel)
        }
        startForeground(FOREGROUND_ID, notification)
    }

    private fun getTaskUpdate() {
        val alarmTime = listOf(
            "07:00",
            "19:00"
        )
        coroutineScope.launch {
            repository.getTask().collect { task ->
                if (task.isNotEmpty()) {
                    for (i in task.indices) {
                        try {
                            var index = 0
                            alarmTime.forEach {
                                val alarm = getTimeTimestamp(it)
                                setupNotification(
                                    index + i,
                                    DAILY_NOTIFICATION, alarm, task[i]
                                )
                                index++
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                } else {
                    var index = 0
                    alarmTime.forEach {
                        val alarm = getTimeTimestamp(it)
                        setupNotification(
                            index + 1,
                            DAILY_NOTIFICATION,
                            alarm,
                            null
                        )
                        index++
                    }
                }
            }
        }
    }

    private fun getTimeTimestamp(time: String): Calendar {
        val splitTime = time.split(":")
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, splitTime[0].toInt())
        cal.set(Calendar.MINUTE, splitTime[1].toInt())
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal
    }

    private fun scheduleUpdate() {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        setupNotification(REQUEST_UPDATE_ID, UPDATE_NOTIFICATION, cal, null)
    }

    private fun setupNotification(
        requestCode: Int,
        type: String,
        calendar: Calendar,
        task: Task?
    ) {
        val notifyIntent = Intent(applicationContext, TaskReceiver::class.java)
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        //Add Message here
        notifyIntent.also {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            it.putExtra("notifyId", "$requestCode")
            it.putExtra("title", task?.title ?: "Empty")
            it.putExtra("deadline", task?.deadline ?: "You not have any task todo")
            it.putExtra("validateTime", DateTimeFormatter.timeOutput.format(calendar.time))
            it.putExtra("type", type)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            10 + requestCode,
            notifyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        // For testing
//        val selectedInterval = 1_000L * 5
//        val triggerTime = SystemClock.elapsedRealtime() + selectedInterval
//
//        alarmManager.setExactAndAllowWhileIdle(
//            AlarmManager.ELAPSED_REALTIME_WAKEUP,
//            triggerTime,
//            pendingIntent
//        )

        // Repeating Alarm
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )

        // Intermediate Alarm
//        alarmManager.setAlarmClock(
//            AlarmManager.AlarmClockInfo(
//                calendar.timeInMillis,
//                pendingIntent
//            ), pendingIntent
//        )
    }
}