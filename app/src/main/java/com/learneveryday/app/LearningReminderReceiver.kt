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

class LearningReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val prefsManager = PreferencesManager(context)
        
        // Check if notifications are enabled
        if (!prefsManager.isNotificationsEnabled()) {
            return
        }

        // Get current topic
        val topicId = prefsManager.getCurrentTopicId() ?: return
        
        // Show notification
        showNotification(context)
    }

    private fun showNotification(context: Context) {
        createNotificationChannel(context)

        val prefsManager = PreferencesManager(context)
        val currentTopicId = prefsManager.getCurrentTopicId()
        
        // Try to get the current learning topic to determine the next lesson
        val topic = currentTopicId?.let { prefsManager.getGeneratedTopic(it) }
        val progress = currentTopicId?.let { prefsManager.getUserProgress(it) }
        
        // Build intent with deep linking
        val pendingIntent: PendingIntent
        val notificationTitle: String
        val notificationText: String
        
        if (topic != null && progress != null && topic.lessons.isNotEmpty()) {
            // Find the next lesson to learn
            val nextLessonIndex = progress.currentLessonIndex.coerceIn(0, topic.lessons.size - 1)
            val nextLesson = topic.lessons.getOrNull(nextLessonIndex)
            
            if (nextLesson != null) {
                // Build back stack: MainActivity -> CurriculumDetailActivity -> LessonReaderActivity
                val stackBuilder = androidx.core.app.TaskStackBuilder.create(context)
                
                // Add MainActivity as the parent
                val mainIntent = Intent(context, MainActivity::class.java)
                stackBuilder.addNextIntent(mainIntent)
                
                // Add CurriculumDetailActivity with curriculum ID
                val detailIntent = Intent(context, com.learneveryday.app.presentation.detail.CurriculumDetailActivity::class.java).apply {
                    putExtra(com.learneveryday.app.presentation.detail.CurriculumDetailActivity.EXTRA_CURRICULUM_ID, topic.id)
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
                
                notificationTitle = "Time to learn!"
                notificationText = "Next lesson: ${nextLesson.title}"
            } else {
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
        } else {
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

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(notificationTitle)
            .setContentText(notificationText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
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

        with(NotificationManagerCompat.from(context)) {
            notify(NOTIFICATION_ID, notification)
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
