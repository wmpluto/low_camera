package com.banyunlai.low_camera

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview as CameraPreview // 给 CameraX Preview 起别名
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.banyunlai.low_camera.ui.theme.Low_cameraTheme
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.ui.unit.*
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Low_cameraTheme {
                Low_cameraApp()
            }
        }
    }
}

@PreviewScreenSizes
@Composable
fun Low_cameraApp() {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.CAMERA) }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            it.icon,
                            contentDescription = it.label
                        )
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
        }
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) {
            when (currentDestination) {
                AppDestinations.CAMERA -> CameraScreen(modifier = Modifier.padding(it))
                AppDestinations.LIBRARY -> LibraryScreen(modifier = Modifier.padding(it))
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    CAMERA("Camera", Icons.Default.Home),
    LIBRARY("Library", Icons.Default.AccountBox),
}

@Composable
fun CameraScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var fileName by remember { mutableStateOf(TextFieldValue("photo_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis()))) }
    var hasCameraPermission by remember { mutableStateOf(false) }

    // 相机权限请求
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted: Boolean ->
            hasCameraPermission = isGranted
            if (!isGranted) {
                Toast.makeText(context, "需要相机权限才能使用相机", Toast.LENGTH_SHORT).show()
            }
        }
    )

    // 检查相机权限
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
        requestPermissionLauncher.launch(Manifest.permission.CAMERA)
    } else {
        hasCameraPermission = true
    }

    Column(modifier = modifier.fillMaxSize()) {
        // 相机预览区域（70%高度）
        Box(
            modifier = Modifier.weight(0.7f).fillMaxWidth()
                .background(Color.Black)
        ) {
            if (hasCameraPermission) {
                CameraPreview()
            } else {
                Text(
                    text = "请允许相机权限",
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White
                )
            }
        }

        // 按键（10%高度）
        Box(modifier = Modifier.weight(0.1f).fillMaxWidth().padding(horizontal = 16.dp)) {
            Button(
                onClick = {
                    Toast.makeText(context, "拍照按钮被点击", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxSize()
            ) {
                Text(text = "拍照")
            }
        }

        // 文字输入框（20%高度）
        Box(modifier = Modifier.weight(0.2f).fillMaxWidth().padding(16.dp)) {
            OutlinedTextField(
                value = fileName,
                onValueChange = { fileName = it },
                label = { Text("输入文件名") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun CameraPreview() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    
    // 初始化相机
    remember {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            
            // 配置预览
            val preview = CameraPreview.Builder().build()
            preview.setSurfaceProvider(previewView.surfaceProvider)
            
            // 选择相机（后置）
            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()
            
            try {
                // 解绑并重新绑定相机
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }
    
    // 使用AndroidView包装PreviewView
    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun LibraryScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Library Screen")
        Text(text = "Photo library functionality will be implemented here")
    }
}

@Preview(showBackground = true)
@Composable
fun CameraScreenPreview() {
    Low_cameraTheme {
        CameraScreen()
    }
}

@Preview(showBackground = true)
@Composable
fun LibraryScreenPreview() {
    Low_cameraTheme {
        LibraryScreen()
    }
}