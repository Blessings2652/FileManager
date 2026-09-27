package com.shizuku.filemanager.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.shizuku.filemanager.sys.InstallStatus
import com.shizuku.filemanager.sys.InternalApkInstaller
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InternalInstallerScreen(
    apkPath: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var currentPath by remember { mutableStateOf(apkPath) }
    val status by InternalApkInstaller.status.collectAsState()
    
    val pickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val tempFile = File(context.cacheDir, "temp_picker.apk")
            context.contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            currentPath = tempFile.absolutePath
        }
    }

    LaunchedEffect(currentPath) {
        InternalApkInstaller.reset()
    }

    val apkFile = remember(currentPath) { if (currentPath.isNotEmpty()) File(currentPath) else null }
    
    val packageInfo = remember(currentPath) {
        if (currentPath.isEmpty()) return@remember null
        context.packageManager.getPackageArchiveInfo(currentPath, 0)?.apply {
            applicationInfo?.let {
                it.sourceDir = currentPath
                it.publicSourceDir = currentPath
            }
        }
    }
    
    val appLabel = packageInfo?.applicationInfo?.loadLabel(context.packageManager)?.toString() ?: apkFile?.name ?: "No APK Selected"
    val appIcon = packageInfo?.applicationInfo?.loadIcon(context.packageManager)

    ScreenScaffold(
        topBar = {
            ModernTopBar(
                title = "App Installer",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                modifier = Modifier.size(120.dp)
            ) {
                if (appIcon != null) {
                    Image(
                        bitmap = appIcon.toBitmap().asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.padding(20.dp).fillMaxSize()
                    )
                } else {
                    Icon(
                        Icons.Default.SystemUpdate,
                        contentDescription = null,
                        modifier = Modifier.padding(32.dp).fillMaxSize(),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(Modifier.height(32.dp))
            
            Text(
                text = appLabel,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            Text(
                text = packageInfo?.packageName ?: "Unknown Package",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(48.dp))

            AnimatedContent(targetState = status, label = "InstallStatus") { currentStatus ->
                when (currentStatus) {
                    is InstallStatus.Idle -> {
                        if (currentPath.isEmpty()) {
                            GradientButton(
                                text = "Select APK to Install",
                                onClick = { pickerLauncher.launch("application/vnd.android.package-archive") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Column {
                                GradientButton(
                                    text = "Install",
                                    onClick = { apkFile?.let { InternalApkInstaller.install(context, it) } },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(12.dp))
                                TextButton(
                                    onClick = { currentPath = "" },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Change APK")
                                }
                            }
                        }
                    }
                    is InstallStatus.Initializing -> {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                    is InstallStatus.Installing -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CarProgressIndicator(
                                progress = currentStatus.progress,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(8.dp))
                            Text("Installing...", fontWeight = FontWeight.Medium)
                        }
                    }
                    is InstallStatus.Success -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(64.dp))
                            Spacer(Modifier.height(16.dp))
                            Text("App installed successfully!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(24.dp))
                            Button(
                                onClick = onBack,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) { Text("Done") }
                        }
                    }
                    is InstallStatus.Failure -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(64.dp))
                            Spacer(Modifier.height(16.dp))
                            Text("Installation failed", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text(currentStatus.message, style = MaterialTheme.typography.bodyMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            Spacer(Modifier.height(24.dp))
                            GradientButton(
                                text = "Retry",
                                onClick = { apkFile?.let { InternalApkInstaller.install(context, it) } },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}
