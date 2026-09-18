package com.example.mmp

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import kotlinx.coroutines.*
import com.example.mmp.theme.MMPTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }

        enableEdgeToEdge()
        setContent {
            MMPTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    App()
                }
            }
        }
    }
}

@Composable
fun MainScreen(onAbout: () -> Unit) {
    val context = LocalContext.current

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("") }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) {
            Toast.makeText(context, "未选择文件", Toast.LENGTH_SHORT).show()
        } else {
            val name = getFileName(context, uri) ?: ""
            val ext = name.substringAfterLast('.', "").lowercase()

            if (ext == "melmod") {
                selectedUri = uri
                fileName = name
                Toast.makeText(context, "已选中：$name", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "请选择 .melmod 文件", Toast.LENGTH_SHORT).show()
            }
        }
    }

    var isProcessing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun doPatch() {
      if (selectedUri == null) {
          Toast.makeText(context, "请先选择文件", Toast.LENGTH_SHORT).show()
          return
      }
      isProcessing = true
      scope.launch(Dispatchers.IO) {
          try {
              val inputBytes = context.contentResolver
                  .openInputStream(selectedUri!!)!!.use { it.readBytes() }

              val py = Python.getInstance()
              val module = py.getModule("main")
              val resultBytes = module.callAttr("EditOne", inputBytes)
                  .toJava(ByteArray::class.java)

              context.contentResolver.openOutputStream(selectedUri!!)?.use {
                  it.write(resultBytes)
              }

              withContext(Dispatchers.Main) {
                  Toast.makeText(context, "适配完成：$fileName", Toast.LENGTH_SHORT).show()
              }
          } catch (e: Exception) {
              withContext(Dispatchers.Main) {
                  Toast.makeText(context, "适配失败：${e.message}", Toast.LENGTH_LONG).show()
              }
          } finally {
              withContext(Dispatchers.Main) {
                  isProcessing = false
              }
          }
      }
}


    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().padding(25.dp)
    ) {
        Text(
            text = "甜瓜模组适配器",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { launcher.launch(arrayOf("*/*")) }) {
            Text("选择 .melmod 文件")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (fileName.isNotEmpty()) {
            Text("已选中：$fileName")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { doPatch() },
            enabled = !isProcessing && selectedUri != null
        ) {
            Text(if (isProcessing) "处理中..." else "开始适配")
        }

        Spacer(modifier = Modifier.weight(1f))

        TextButton(onClick = onAbout) {
          Text("关于")
        }
    }
}

@Composable
fun AboutScreen(onBack: () -> Unit) {
  Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().padding(25.dp)
    ){
    Text(text = "甜瓜模组联机适配器")
    Spacer(modifier = Modifier.height(5.dp))
    Text(text = "Melmod Mutiplayer Patcher | MMP")
    Spacer(modifier = Modifier.height(5.dp))
    Text(text = "开发者：蓝草LanGrass")

    Spacer(modifier = Modifier.weight(1f))

    Text(text = "Realese 1.0", color = Color.Gray)
    Text(text = "Build 2026-9-18-21:07", color = Color.Gray)

    Button(onClick = onBack) {
      Text("返回")
    }
  }
}

fun getFileName(context: Context, uri: Uri): String? {
    var name: String? = null
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (nameIndex >= 0 && cursor.moveToFirst()) {
            name = cursor.getString(nameIndex)
        }
    }
    return name
}

@Composable
fun App() {
    // 页面状态：main / about
    var state by remember { mutableStateOf("main") }

    when (state) {
        "main" -> MainScreen(onAbout = { state = "about" })
        "about" -> AboutScreen(onBack = { state = "main" })
    }
}