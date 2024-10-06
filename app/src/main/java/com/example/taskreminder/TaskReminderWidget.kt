package com.example.taskreminder

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class TaskReminderWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {}
    override fun onDisabled(context: Context) {}
}

internal fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
    val views = RemoteViews(context.packageName, R.layout.task_reminder_widget)
    views.setTextViewText(R.id.widgetTaskText, "今日のタスク: ミーティング")

    // タスク完了ボタンを設定
    val intent = Intent(context, TaskReminderWidget::class.java)
    intent.action = "TASK_COMPLETED"
    val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    views.setOnClickPendingIntent(R.id.completeTaskButton, pendingIntent)

    appWidgetManager.updateAppWidget(appWidgetId, views)
}
