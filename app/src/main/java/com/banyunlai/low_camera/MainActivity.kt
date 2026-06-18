package com.banyunlai.low_camera

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.widget.Toast
import java.io.FileOutputStream
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview as CameraPreview // 给 CameraX Preview 起别名
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.TextStyle
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    // 将文件名拆分为地址和目的
    var address by remember { mutableStateOf(TextFieldValue("")) }
    var purpose by remember { mutableStateOf(TextFieldValue("")) }
    var hasCameraPermission by remember { mutableStateOf(false) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    
    // 年月日选择状态
    val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(System.currentTimeMillis())
    val year = currentDate.substring(0, 4).toInt()
    val month = currentDate.substring(5, 7).toInt()
    val day = currentDate.substring(8, 10).toInt()
    
    var selectedYear by remember { mutableStateOf(year) }
    var selectedMonth by remember { mutableStateOf(month) }
    var selectedDay by remember { mutableStateOf(day) }
    
    // 金额输入状态
    var amount by remember { mutableStateOf(TextFieldValue("")) }
    
    // 生成年份选项（近10年）
    val years = (year - 1)..(year + 1)
    // 生成月份选项（1-12）
    val months = 1..12
    // 生成日期选项（根据月份和年份确定天数）
    val daysInMonth = when (selectedMonth) {
        4, 6, 9, 11 -> 30
        2 -> if (selectedYear % 4 == 0 && (selectedYear % 100 != 0 || selectedYear % 400 == 0)) 29 else 28
        else -> 31
    }
    val days = 1..daysInMonth
    
    // 确保选择的日期在有效范围内
    if (selectedDay > daysInMonth) {
        selectedDay = daysInMonth
    }

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

    // 检查相机权限（在LaunchedEffect中执行副作用操作）
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            hasCameraPermission = true
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // 相机预览区域（70%高度）
        Box(
            modifier = Modifier.weight(0.7f).fillMaxWidth()
                .background(Color.Black)
        ) {
            if (hasCameraPermission) {
                imageCapture = CameraPreview()
            } else {
                Text(
                    text = "请允许相机权限",
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White
                )
            }
        }

        // 年月日下拉选单区域（8%高度）
        Box(modifier = Modifier.weight(0.1f).fillMaxWidth().padding(8.dp)) {
            Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceEvenly) {
                // 年份选择
                YearDropdown(
                    selectedYear = selectedYear, 
                    onYearSelected = { selectedYear = it }, 
                    years = years,
                    modifier = Modifier.weight(1f)
                )
                // 月份选择
                MonthDropdown(
                    selectedMonth = selectedMonth, 
                    onMonthSelected = { selectedMonth = it }, 
                    months = months,
                    modifier = Modifier.weight(1f)
                )
                // 日期选择
                DayDropdown(
                    selectedDay = selectedDay, 
                    onDaySelected = { selectedDay = it }, 
                    days = days,
                    modifier = Modifier.weight(1f)
                )

            }
        }

        // 地址和目的输入框（12%高度）
        Box(modifier = Modifier.weight(0.12f).fillMaxWidth().padding(8.dp)) {
            Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceEvenly) {
                // 地址输入框
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("起始地址") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                )
                // 目的输入框
                OutlinedTextField(
                    value = purpose,
                    onValueChange = { purpose = it },
                    label = { Text("目的地址") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                )
                // 金额输入框
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("金额")},
                    modifier = Modifier.weight(0.5f).padding(horizontal = 4.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        }

        // 按键（10%高度）
        Box(modifier = Modifier.weight(0.08f).fillMaxWidth().padding(horizontal = 8.dp)) {
            Button(
                onClick = {
                    if (imageCapture != null) {
                        // 获取当前的分钟和秒数
                        val currentTime = SimpleDateFormat("mmss", Locale.US).format(System.currentTimeMillis())
                        // 组合文件名：选择年月日_当前的分钟秒数_地址_目的_金额
                        val formattedDate = "${selectedYear}${selectedMonth.toString().padStart(2, '0')}${selectedDay.toString().padStart(2, '0')}"
                        val combinedFileName = "${formattedDate}${currentTime}_${address.text.replace(" ", "_")}_${purpose.text}_${amount.text}"
                        takePhoto(imageCapture, combinedFileName.replace(" ", ""), context)
                    } else {
                        Toast.makeText(context, "无法拍照，请检查相机权限", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) {
                Text(text = "拍照")
            }
        }
    }
}

// 年份下拉选择器
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YearDropdown(
    selectedYear: Int, 
    onYearSelected: (Int) -> Unit, 
    years: IntRange,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier.padding(horizontal = 4.dp)
    ) {
        OutlinedTextField(
            value = selectedYear.toString(),
            onValueChange = {},
            readOnly = true,
            label = { Text("年", fontSize = 12.sp) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxSize().menuAnchor(),
            textStyle = TextStyle(fontSize = 14.sp)
        )
        
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            years.forEach { year ->
                DropdownMenuItem(
                    text = { Text(year.toString(), fontSize = 14.sp) },
                    onClick = {
                        onYearSelected(year)
                        expanded = false
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// 月份下拉选择器
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthDropdown(
    selectedMonth: Int, 
    onMonthSelected: (Int) -> Unit, 
    months: IntRange,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier.padding(horizontal = 4.dp)
    ) {
        OutlinedTextField(
            value = selectedMonth.toString().padStart(2, '0'),
            onValueChange = {},
            readOnly = true,
            label = { Text("月", fontSize = 12.sp) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxSize().menuAnchor(),
            textStyle = TextStyle(fontSize = 14.sp)
        )
        
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            months.forEach { month ->
                DropdownMenuItem(
                    text = { Text(month.toString().padStart(2, '0'), fontSize = 14.sp) },
                    onClick = {
                        onMonthSelected(month)
                        expanded = false
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// 日期下拉选择器
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDropdown(
    selectedDay: Int, 
    onDaySelected: (Int) -> Unit, 
    days: IntRange,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier.padding(horizontal = 4.dp)
    ) {
        OutlinedTextField(
            value = selectedDay.toString().padStart(2, '0'),
            onValueChange = {},
            readOnly = true,
            label = { Text("日", fontSize = 12.sp) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxSize().menuAnchor(),
            textStyle = TextStyle(fontSize = 14.sp)
        )
        
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            days.forEach { day ->
                DropdownMenuItem(
                    text = { Text(day.toString().padStart(2, '0'), fontSize = 14.sp) },
                    onClick = {
                        onDaySelected(day)
                        expanded = false
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private fun takePhoto(imageCapture: ImageCapture?, fileName: String, context: Context) {
    imageCapture?.let {
        // 创建输出选项
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/LowCamera")
            }
        }

        // 创建输出文件
        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            context.contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()

        // 执行拍照
        it.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    // 获取保存的照片Uri
                    val imageUri = outputFileResults.savedUri
                    if (imageUri != null) {
                        try {
                            // 使用ParcelFileDescriptor直接操作文件
                            val parcelFileDescriptor = context.contentResolver.openFileDescriptor(imageUri, "rw")
                            parcelFileDescriptor?.use { pfd ->
                                // 打开输入流加载原始照片
                                val inputStream = context.contentResolver.openInputStream(imageUri)
                                inputStream?.use { isStream ->
                                    val originalBitmap = BitmapFactory.decodeStream(isStream)
                                    
                                    // 计算缩小后的尺寸（1/4长1/4宽）
                                    val scaledWidth = originalBitmap.width / 2
                                    val scaledHeight = originalBitmap.height / 2
                                    
                                    // 创建缩小后的Bitmap
                                    val scaledBitmap = Bitmap.createScaledBitmap(
                                        originalBitmap,
                                        scaledWidth,
                                        scaledHeight,
                                        true // 使用双线性滤波提升质量
                                    )
                                    
                                    // 创建旋转Matrix
                                    val matrix = Matrix()
                                    matrix.postRotate(90f) // 顺时针旋转90度
                                    
                                    // 创建旋转后的Bitmap
                                    val rotatedBitmap = Bitmap.createBitmap(
                                        scaledBitmap,
                                        0,
                                        0,
                                        scaledWidth,
                                        scaledHeight,
                                        matrix,
                                        true
                                    )
                                    
                                    // 关闭输入流
                                    isStream.close()
                                    
                                    // 打开输出流并截断文件
                                    val fileOutputStream = FileOutputStream(pfd.fileDescriptor)
                                    fileOutputStream.use {
                                        // 截断文件到0长度
                                        it.channel.truncate(0)
                                        
                                        // 压缩并写入旋转后的图片
                                        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 65, it)
                                        it.flush()
                                    }
                                    
                                    // 释放旋转后的Bitmap资源
                                    rotatedBitmap.recycle()
                                    
                                    // 释放资源
                                    originalBitmap.recycle()
                                    scaledBitmap.recycle()
                                    
                                    Toast.makeText(
                                        context,
                                        "照片已保存为: $fileName (${scaledWidth}x${scaledHeight})",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(
                                context,
                                "缩放照片失败: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        Toast.makeText(
                            context,
                            "照片已保存，但无法获取文件信息",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(
                        context,
                        "拍照失败: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    exception.printStackTrace()
                }
            }
        )
    } ?: run {
        Toast.makeText(context, "相机未初始化", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun CameraPreview(): ImageCapture? {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val imageCapture = remember { ImageCapture.Builder().build() }
    
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
                    preview,
                    imageCapture
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
    
    return imageCapture
}

@Composable
fun LibraryScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        // 使用ACTION_OPEN_DOCUMENT_TREE来打开文件管理器
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        
        // 设置初始目录为Pictures/LowCamera
        val initialUri = Uri.parse("content://com.android.externalstorage.documents/tree/primary:Pictures/LowCamera")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, initialUri)
        }
        
        // 添加必要的标志
        intent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_GRANT_READ_URI_PERMISSION or
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        
        // 尝试打开系统文件管理器
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // 如果失败，尝试使用另一种方式
            val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                this.data = initialUri
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(fallbackIntent)
            } catch (fallbackException: Exception) {
                Toast.makeText(context, "无法打开文件管理器", Toast.LENGTH_SHORT).show()
            }
        }
    }
    // 占位布局（跳转后当前页面无需显示内容）
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "正在打开文件管理器...")
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