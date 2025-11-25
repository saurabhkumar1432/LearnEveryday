package com.learneveryday.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.learneveryday.app.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LearningReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val prefsManager = PreferencesManager(context)
        
        // Check if notifications are enabled
        if (!prefsManager.isNotificationsEnabled()) {
            return
        }

        // Use coroutine to access Room database
        CoroutineScope(Dispatchers.IO).launch {
            showNotificationWithRoomData(context, prefsManager)
        }
    }

    private suspend fun showNotificationWithRoomData(context: Context, prefsManager: PreferencesManager) {
        createNotificationChannel(context)
        
        val database = AppDatabase.getInstance(context)
        val curriculumDao = database.curriculumDao()
        val lessonDao = database.lessonDao()
        
        // Build intent with deep linking
        var pendingIntent: PendingIntent? = null
        var notificationTitle: String = ""
        var notificationText: String = ""
        
        try {
            // Get the most recently accessed curriculum that's not completed
            val currentCurriculumId = prefsManager.getCurrentTopicId()
            val curriculum = if (currentCurriculumId != null) {
                curriculumDao.getCurriculumByIdSync(currentCurriculumId)
            } else {
                // Fall back to most recent curriculum
                curriculumDao.getMostRecentCurriculumSync()
            }
            
            if (curriculum != null && !curriculum.isCompleted) {
                // Get the next incomplete lesson
                val lessons = lessonDao.getLessonsByCurriculumSync(curriculum.id)
                val nextLesson = lessons
                    .sortedBy { it.orderIndex }
                    .firstOrNull { !it.isCompleted }
                
                if (nextLesson != null) {
                    // Build back stack: MainActivity -> CurriculumDetailActivity -> LessonReaderActivity
                    val stackBuilder = androidx.core.app.TaskStackBuilder.create(context)
                    
                    // Add MainActivity as the parent
                    val mainIntent = Intent(context, MainActivity::class.java)
                    stackBuilder.addNextIntent(mainIntent)
                    
                    // Add CurriculumDetailActivity with curriculum ID
                    val detailIntent = Intent(context, com.learneveryday.app.presentation.detail.CurriculumDetailActivity::class.java).apply {
                        putExtra(com.learneveryday.app.presentation.detail.CurriculumDetailActivity.EXTRA_CURRICULUM_ID, curriculum.id)
                    }
                    stackBuilder.addNextIntent(detailIntent)
                    
                    // Add the LessonReaderActivity
                    val lessonIntent = Intent(context, com.learneveryday.app.presentation.reader.LessonReaderActivity::class.java).apply {
                        putExtra(com.learneveryday.app.presentation.reader.LessonReaderActivity.EXTRA_LESSON_ID, nextLesson.id)
                    }
                    stackBuilder.addNextIntent(lessonIntent)
                    
                    pendingIntent = stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )!!
                    
                    // Calculate progress
                    val completedCount = lessons.count { it.isCompleted }
                    val totalCount = lessons.size
                    
                    notificationTitle = "Time to Learn"
                    notificationText = "Continue: ${nextLesson.title} ($completedCount/$totalCount completed)"
                } else {
                    // All lessons completed, show congratulations
                    val intent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    pendingIntent = PendingIntent.getActivity(
                        context, 0, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    notificationTitle = "Congratulations!"
                    notificationText = "You've completed \"${curriculum.title}\". Start a new learning path?"
                }
            } else {
                // No active curriculum, prompt to create one
                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                pendingIntent = PendingIntent.getActivity(
                    context, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                notificationTitle = context.getString(R.string.notification_title)
                notificationText = "Start a new learning journey today"
            }
        } catch (e: Exception) {
            // Fallback to MainActivity
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            notificationTitle = context.getString(R.string.notification_title)
            notificationText = context.getString(R.string.notification_text)
        }

        // Ensure pendingIntent is not null
        if (pendingIntent == null) {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            notificationTitle = context.getString(R.string.notification_title)
            notificationText = context.getString(R.string.notification_text)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_book)
            .setContentTitle(notificationTitle)
            .setContentText(notificationText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notificationText))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        // Check for notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Permission not granted, cannot show notification
                return
            }
        }

        // Post notification on main thread
        CoroutineScope(Dispatchers.Main).launch {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.channel_name)
            val descriptionText = context.getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "learning_reminders"
        private const val NOTIFICATION_ID = 1001
    }
}
