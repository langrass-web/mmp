package com.example.mmp

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.provider.OpenableColumns
import android.widget.Toast
import android.content.Intent
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

import androidx.core.content.FileProvider
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import kotlinx.coroutines.*
import com.example.mmp.theme.MMPTheme

import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }
        
        requestFilesPermission(this)

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
fun MainScreen() {
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

            val patchedDir = File(Environment.getExternalStorageDirectory(), "Download/mmp-patch")
            if (!patchedDir.exists()) patchedDir.mkdirs()
            val outFile = File(patchedDir, fileName)
            outFile.writeBytes(resultBytes)


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
        Spacer(modifier = Modifier.height(5.dp))

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
    }
}

@Composable
fun AboutScreen() {
  Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().padding(25.dp)
    ){
        Spacer(modifier = Modifier.height(5.dp))
        Text(text = "甜瓜模组联机适配器")
        Spacer(modifier = Modifier.height(5.dp))
        Text(text = "Melmod Mutiplayer Patcher | MMP")
        Spacer(modifier = Modifier.height(5.dp))
        Text(text = "开发者：蓝草LanGrass")

        Spacer(modifier = Modifier.weight(1f))

        Text(text = "Realese 1.1", color = Color.Gray)
        Text(text = "Build 2026-9-19-11:46", color = Color.Gray)
    }
}

@Composable
fun PatchedScreen() {
    val context = LocalContext.current
    var files by remember { mutableStateOf<List<File>>(emptyList()) }

    LaunchedEffect(Unit) {
        val dir = File(Environment.getExternalStorageDirectory(), "Download/mmp-patch")
        files = dir.listFiles()?.filter { it.extension.lowercase() == "melmod" } ?: emptyList()
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().padding(25.dp)
    ) {
        Spacer(modifier = Modifier.height(5.dp))
        Text(text = "已适配模组", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        if (files.isEmpty()) {
            Text("还没有适配过的模组", color = Color.Gray)
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(files) { file ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = file.name,
                            modifier = Modifier.weight(1f)
                        )
                        Button(onClick = { importToMelon(context, file.name) }) {
                            Text("导入")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun App() {
    // 页面状态：main / about
    var state by remember { mutableStateOf("main") }

    Scaffold(
    bottomBar = {
        NavigationBar {
            NavigationBarItem(
                selected = state == "main",
                onClick = { state = "main" },
                icon = { Icon(Icons.Default.Home, "主页") },
                label = { Text("主页") }
            )
            NavigationBarItem(
                selected = state == "patched",
                onClick = { state = "patched" },
                icon = { Icon(Icons.Default.Folder, "文件") },
                label = { Text("文件") }
            )
            NavigationBarItem(
                selected = state == "about",
                onClick = { state = "about" },
                icon = { Icon(Icons.Default.Info, "关于") },
                label = { Text("关于") }
            )
        }
        }
    ) { innerPadding ->
        // 内容区，用 innerPadding 避免被底栏遮挡
        Box(Modifier.padding(innerPadding)) {
            when (state) {
                "main" -> MainScreen()
                "patched" -> PatchedScreen()
                "about" -> AboutScreen()
            }
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


fun importToMelon(context: Context, fileName: String) {
    // 1. 拿到 patched/ 下的文件
    val patchedDir = File(Environment.getExternalStorageDirectory(), "Download/mmp-patch")
    val file = File(patchedDir, fileName)

    if (!file.exists()) {
        Toast.makeText(context, "文件不存在：$fileName", Toast.LENGTH_SHORT).show()
        return
    }

    // 2. FileProvider 生成 content:// URI
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )

    // 3. 强制指定甜瓜包名
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/octet-stream")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        setPackage("com.studio27.MelonPlayground")
    }

    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "无法打开甜瓜：${e.message}", Toast.LENGTH_LONG).show()
    }
}

fun requestFilesPermission(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        if (!Environment.isExternalStorageManager()) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
        }
    }
}