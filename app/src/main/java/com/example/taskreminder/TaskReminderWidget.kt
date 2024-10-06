package com.example.taskreminder

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName  // 追加
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.Toast

class TaskReminderWidget : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action == "TASK_COMPLETED") {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisAppWidget = ComponentName(context.packageName, javaClass.name)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget)
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId, "タスク完了")
            }

            // タスク完了の通知をユーザーに表示
            Toast.makeText(context, "タスクが完了しました", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {}
    override fun onDisabled(context: Context) {}
}

internal fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, taskStatus: String = "今日のタスク: ミーティング") {
    val views = RemoteViews(context.packageName, R.layout.task_reminder_widget)
    views.setTextViewText(R.id.widgetTaskText, taskStatus)

    // 完了ボタンのPendingIntentを設定
    val intent = Intent(context, TaskReminderWidget::class.java)
    intent.action = "TASK_COMPLETED"
    val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE) // FLAG_IMMUTABLE を追加
    views.setOnClickPendingIntent(R.id.completeTaskButton, pendingIntent)

    appWidgetManager.updateAppWidget(appWidgetId, views)
}
