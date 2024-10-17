package com.example.taskreminder

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Paint
import android.widget.RemoteViews
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class TaskReminderWidget : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action == "ADD_TASK") {
            val addTaskIntent = Intent(context, MainActivity::class.java)
            addTaskIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(addTaskIntent)
        } else if (intent.action == "TASK_COMPLETED") {
            val taskIndex = intent.getIntExtra("taskIndex", -1)
            if (taskIndex != -1) {
                val sharedPreferences = context.getSharedPreferences("TaskReminderPrefs", Context.MODE_PRIVATE)
                val completedTasks = getCompletedTasks(sharedPreferences)
                if (completedTasks.contains(taskIndex)) {
                    completedTasks.remove(taskIndex) // 取り消し線を解除
                } else {
                    completedTasks.add(taskIndex) // 取り消し線を追加
                }
                saveCompletedTasks(sharedPreferences, completedTasks)

                val appWidgetManager = AppWidgetManager.getInstance(context)
                val thisAppWidget = ComponentName(context.packageName, javaClass.name)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget)
                for (appWidgetId in appWidgetIds) {
                    updateAppWidget(context, appWidgetManager, appWidgetId)
                }
            }
        } else if (intent.action == "CLEAR_ALL_TASKS") {
            val sharedPreferences = context.getSharedPreferences("TaskReminderPrefs", Context.MODE_PRIVATE)
            saveCompletedTasks(sharedPreferences, mutableSetOf()) // 完了済みタスクリストをクリア

            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisAppWidget = ComponentName(context.packageName, javaClass.name)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget)
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
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
            val completedTasks = getCompletedTasks(sharedPreferences)

            // タスクコンテナをクリアして、チェックボックスとタスクを追加
            views.removeAllViews(R.id.taskContainer)

            taskList.forEachIndexed { index, task ->
                // 各タスクごとにレイアウトを設定
                val taskView = RemoteViews(context.packageName, R.layout.task_item)
                taskView.setTextViewText(R.id.taskNameText, task)

                // タスクが完了済みの場合は取り消し線を追加
                if (completedTasks.contains(index)) {
                    taskView.setInt(R.id.taskNameText, "setPaintFlags", Paint.STRIKE_THRU_TEXT_FLAG)
                    taskView.setTextViewText(R.id.taskCompleteButton, "戻す")
                } else {
                    taskView.setInt(R.id.taskNameText, "setPaintFlags", 0)
                    taskView.setTextViewText(R.id.taskCompleteButton, "完了")
                }

                // 完了/戻すボタンのPendingIntentを設定
                val intent = Intent(context, TaskReminderWidget::class.java)
                intent.action = "TASK_COMPLETED"
                intent.putExtra("taskIndex", index)
                val pendingIntent = PendingIntent.getBroadcast(context, index, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                taskView.setOnClickPendingIntent(R.id.taskCompleteButton, pendingIntent)

                // タスクコンテナに追加
                views.addView(R.id.taskContainer, taskView)
            }

            // + ボタンのインテントを設定
            val addTaskIntent = Intent(context, TaskReminderWidget::class.java)
            addTaskIntent.action = "ADD_TASK"
            val addTaskPendingIntent = PendingIntent.getBroadcast(context, 0, addTaskIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.addTaskButton, addTaskPendingIntent)

            // クリアボタンのインテントを設定
            val clearAllTasksIntent = Intent(context, TaskReminderWidget::class.java)
            clearAllTasksIntent.action = "CLEAR_ALL_TASKS"
            val clearAllTasksPendingIntent = PendingIntent.getBroadcast(context, 0, clearAllTasksIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.clearAllTasksButton, clearAllTasksPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        // タスクリストを取得
        fun getTaskList(sharedPreferences: SharedPreferences): MutableList<String> {
            val gson = Gson()
            val json = sharedPreferences.getString("taskList", null)
            val type = object : TypeToken<MutableList<String>>() {}.type
            return if (json != null) {
                gson.fromJson<MutableList<String>>(json, type)
            } else {
                mutableListOf()
            }
        }

        // 完了済みタスクリストを取得
        fun getCompletedTasks(sharedPreferences: SharedPreferences): MutableSet<Int> {
            return sharedPreferences.getStringSet("completedTasks", mutableSetOf())?.map { it.toInt() }?.toMutableSet() ?: mutableSetOf()
        }

        // 完了済みタスクリストを保存
        fun saveCompletedTasks(sharedPreferences: SharedPreferences, completedTasks: MutableSet<Int>) {
            val editor = sharedPreferences.edit()
            editor.putStringSet("completedTasks", completedTasks.map { it.toString() }.toSet())
            editor.apply()
        }
    }
}
