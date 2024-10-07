package com.example.taskreminder

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.Toast

class TaskReminderWidget : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action == "TASK_COMPLETED") {
            // タスク完了時に SharedPreferences に完了状態を保存
            val sharedPreferences = context.getSharedPreferences("TaskReminderPrefs", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putBoolean("taskCompleted", true) // タスクが完了したことを保存
            editor.apply()

            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisAppWidget = ComponentName(context.packageName, javaClass.name)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget)
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
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

    override fun onEnabled(context: Context) {
        // 初回ウィジェット追加時の処理
    }

    override fun onDisabled(context: Context) {
        // 最後のウィジェットが削除された時の処理
    }
}

internal fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
    val views = RemoteViews(context.packageName, R.layout.task_reminder_widget)

    // SharedPreferencesからタスクと完了状態を取得
    val sharedPreferences = context.getSharedPreferences("TaskReminderPrefs", Context.MODE_PRIVATE)
    val task = sharedPreferences.getString("task", "今日のタスク: 未設定")
    val taskCompleted = sharedPreferences.getBoolean("taskCompleted", false)

    // 完了状態に応じて表示を変更
    if (taskCompleted) {
        views.setTextViewText(R.id.widgetTaskText, "$task - 完了")
    } else {
        views.setTextViewText(R.id.widgetTaskText, task)
    }

    // 完了ボタンのPendingIntentを設定
    val intent = Intent(context, TaskReminderWidget::class.java)
    intent.action = "TASK_COMPLETED"
    val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    views.setOnClickPendingIntent(R.id.completeTaskButton, pendingIntent)

    appWidgetManager.updateAppWidget(appWidgetId, views)
}
