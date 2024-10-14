package com.example.taskreminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.taskreminder.ui.theme.TaskReminderTheme
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Calendar

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
    val taskList = remember { mutableStateOf(getTaskList(sharedPreferences)) }
    var selectedHour by remember { mutableStateOf(0) }
    var selectedMinute by remember { mutableStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        // 時間設定ボタン
        Button(
            onClick = {
                TimePickerDialog(
                    context,
                    { _, hour, minute ->
                        selectedHour = hour
                        selectedMinute = minute

                        // アラームを設定して、その時間に全タスクのチェックをクリアする
                        setClearAllTasksAlarm(context, hour, minute)
                    },
                    selectedHour,
                    selectedMinute,
                    true
                ).show()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("時間を設定")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 水平線を追加
        Divider(
            color = Color.Gray,
            thickness = 1.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )

        // タスクの入力フィールドと追加ボタンを横に配置
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = task,
                onValueChange = { task = it },
                label = { Text("タスクを入力") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (task.isNotEmpty()) {
                        // 既存のタスクリストを取得し、タスクを追加
                        val updatedTaskList = taskList.value.toMutableList()
                        updatedTaskList.add(task)

                        // 更新されたタスクリストを保存
                        saveTaskList(sharedPreferences, updatedTaskList)
                        taskList.value = updatedTaskList

                        // ウィジェットを更新
                        val appWidgetManager = AppWidgetManager.getInstance(context)
                        val widgetComponent = ComponentName(context, TaskReminderWidget::class.java)
                        val appWidgetIds = appWidgetManager.getAppWidgetIds(widgetComponent)
                        for (appWidgetId in appWidgetIds) {
                            TaskReminderWidget.updateAppWidget(context, appWidgetManager, appWidgetId)
                        }

                        task = ""
                    }
                },
                modifier = Modifier.wrapContentWidth()
            ) {
                Text("追加")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // タスクのリストを表示
        LazyColumn(
            modifier = Modifier.fillMaxWidth()
        ) {
            items(taskList.value) { taskItem ->
                TaskItem(
                    task = taskItem,
                    onDeleteClick = {
                        // タスクを削除する処理
                        val updatedTaskList = taskList.value.toMutableList()
                        updatedTaskList.remove(taskItem)
                        saveTaskList(sharedPreferences, updatedTaskList)
                        taskList.value = updatedTaskList

                        // ウィジェットを更新
                        val appWidgetManager = AppWidgetManager.getInstance(context)
                        val widgetComponent = ComponentName(context, TaskReminderWidget::class.java)
                        val appWidgetIds = appWidgetManager.getAppWidgetIds(widgetComponent)
                        for (appWidgetId in appWidgetIds) {
                            TaskReminderWidget.updateAppWidget(context, appWidgetManager, appWidgetId)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun TaskItem(task: String, onDeleteClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = task,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = { onDeleteClick() }) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "タスクを削除"
                )
            }
        }
    }
}

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

fun saveTaskList(sharedPreferences: SharedPreferences, taskList: MutableList<String>) {
    val gson = Gson()
    val json = gson.toJson(taskList)
    val editor = sharedPreferences.edit()
    editor.putString("taskList", json)
    editor.apply()
}

fun setClearAllTasksAlarm(context: Context, hour: Int, minute: Int) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, TaskReminderWidget::class.java).apply {
        action = "CLEAR_ALL_TASKS"
    }
    val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    val calendar = Calendar.getInstance().apply {
        timeInMillis = System.currentTimeMillis()
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)

        // 設定した時間が過去なら翌日に設定
        if (before(Calendar.getInstance())) {
            add(Calendar.DAY_OF_MONTH, 1)
        }
    }

    alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
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
