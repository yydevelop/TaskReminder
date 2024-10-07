package com.example.taskreminder

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.widget.RemoteViews
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class TaskReminderWidget : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action == "TASK_COMPLETED") {
            val taskIndex = intent.getIntExtra("taskIndex", -1)
            if (taskIndex != -1) {
                val sharedPreferences = context.getSharedPreferences("TaskReminderPrefs", Context.MODE_PRIVATE)
                val taskList = getTaskList(sharedPreferences)
                taskList.removeAt(taskIndex) // タスクをリストから削除
                saveTaskList(sharedPreferences, taskList)

                val appWidgetManager = AppWidgetManager.getInstance(context)
                val thisAppWidget = ComponentName(context.packageName, javaClass.name)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget)
                for (appWidgetId in appWidgetIds) {
                    updateAppWidget(context, appWidgetManager, appWidgetId)
                }

                Toast.makeText(context, "タスクが完了しました", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {}
    override fun onDisabled(context: Context) {}

    companion object {
        internal fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.task_reminder_widget)

            // SharedPreferencesからタスクリストを取得
            val sharedPreferences = context.getSharedPreferences("TaskReminderPrefs", Context.MODE_PRIVATE)
            val taskList = getTaskList(sharedPreferences)

            // タスクリストを表示
            val taskDisplay = taskList.joinToString(separator = "\n")
            views.setTextViewText(R.id.widgetTaskText, taskDisplay)

            // 完了ボタンのPendingIntentを設定（各タスクごと）
            taskList.forEachIndexed { index, _ ->
                val intent = Intent(context, TaskReminderWidget::class.java)
                intent.action = "TASK_COMPLETED"
                intent.putExtra("taskIndex", index)
                val pendingIntent = PendingIntent.getBroadcast(context, index, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                views.setOnClickPendingIntent(R.id.completeTaskButton, pendingIntent)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        // タスクリストを取得
        fun getTaskList(sharedPreferences: SharedPreferences): MutableList<String> {
            val gson = Gson()
            val json = sharedPreferences.getString("taskList", null)
            val type = object : TypeToken<MutableList<String>>() {}.type
            return if (json != null) {
                gson.fromJson(json, type)
            } else {
                mutableListOf()
            }
        }

        // タスクリストを保存
        fun saveTaskList(sharedPreferences: SharedPreferences, taskList: MutableList<String>) {
            val gson = Gson()
            val json = gson.toJson(taskList)
            val editor = sharedPreferences.edit()
            editor.putString("taskList", json)
            editor.apply()
        }
    }
}
