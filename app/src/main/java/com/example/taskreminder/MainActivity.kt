package com.example.taskreminder

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.taskreminder.ui.theme.TaskReminderTheme
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TaskReminderTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    TaskInputScreen(
                        modifier = Modifier.padding(innerPadding),
                        sharedPreferences = getSharedPreferences("TaskReminderPrefs", Context.MODE_PRIVATE)
                    )
                }
            }
        }
    }
}

@Composable
fun TaskInputScreen(modifier: Modifier = Modifier, sharedPreferences: SharedPreferences) {
    var task by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        TextField(
            value = task,
            onValueChange = { task = it },
            label = { Text("タスクを入力") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                // 既存のタスクリストを取得
                val taskList = getTaskList(sharedPreferences)
                taskList.add(task)

                // 更新されたタスクリストを保存
                saveTaskList(sharedPreferences, taskList)

                // ウィジェットを更新
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val widgetComponent = ComponentName(context, TaskReminderWidget::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(widgetComponent)
                for (appWidgetId in appWidgetIds) {
                    TaskReminderWidget.updateAppWidget(context, appWidgetManager, appWidgetId) // メソッドを呼び出す
                }

                task = ""
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("タスクを追加")
        }
    }
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

@Preview(showBackground = true)
@Composable
fun TaskInputScreenPreview() {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("TaskReminderPrefs", Context.MODE_PRIVATE)
    TaskReminderTheme {
        TaskInputScreen(sharedPreferences = sharedPreferences)
    }
}
