package com.shizuku.filemanager.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shizuku.filemanager.sys.AppDetails
import com.shizuku.filemanager.sys.AppInfo
import com.shizuku.filemanager.sys.AppManager
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailsScreen(
    app: AppInfo,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val locale = LocalLocale.current.platformLocale
    var details by remember { mutableStateOf<AppDetails?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(app.packageName) {
        details = AppManager.getAppDetails(context, app.packageName)
        isLoading = false
    }

    ScreenScaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            ModernTopBar(
                title = "App Details",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            Column(
                Modifier
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    AppHeader(app)
                    Spacer(Modifier.height(24.dp))
                    
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        DetailAction(Icons.AutoMirrored.Filled.Launch, "Launch") {
                            val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                            if (intent != null) context.startActivity(intent)
                        }
                        DetailAction(Icons.Default.Download, "Extract") {
                            scope.launch {
                                val dest = File(android.os.Environment.getExternalStorageDirectory(), "ExtractedAPKs")
                                AppManager.extractApk(context, app, dest).onSuccess {
                                    snackbarHostState.showSnackbar("Extracted to: ${it.absolutePath}")
                                }.onFailure {
                                    snackbarHostState.showSnackbar("Extraction failed: ${it.message}")
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                
                details?.let { d ->
                    SectionHeader("General Information")
                    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
                        InfoRow("Package Name", d.packageName)
                        InfoRow("Category", d.category)
                        InfoRow("Version Name", d.versionName)
                        InfoRow("Version Code", d.versionCode.toString())
                        InfoRow("Min SDK", d.minSdk.toString())
                        InfoRow("Target SDK", d.targetSdk.toString())
                        InfoRow("UID", d.uid.toString())
                        InfoRow("Process Name", d.processName)
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    
                    SectionHeader("Installation Details")
                    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
                        InfoRow("Source Dir", d.sourceDir)
                        InfoRow("Native Lib Dir", d.nativeLibraryDir)
                        InfoRow("Install Time", formatDateTime(d.installTime, locale))
                        InfoRow("Last Update", formatDateTime(d.updateTime, locale))
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    ExpandableInfoSection("Certificates (${d.certificates.size})") {
                        d.certificates.forEach { cert ->
                            Column(Modifier.padding(vertical = 4.dp)) {
                                InfoRow("Subject", cert.subject)
                                InfoRow("Issuer", cert.issuer)
                                InfoRow("Valid From", cert.validFrom)
                                InfoRow("Valid Until", cert.validUntil)
                                InfoRow("Serial Number", cert.serialNumber)
                                InfoRow("SHA-256", cert.fingerprintSha256)
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                        }
                    }

                    ExpandableInfoSection("Used Permissions (${d.usedPermissions.size})") {
                        d.usedPermissions.forEach { perm ->
                            val isDangerous = d.dangerousPermissions.contains(perm)
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                                if (isDangerous) {
                                    Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                }
                                Text(
                                    perm, 
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isDangerous) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    ExpandableInfoSection("Activities (${d.activities.size})") {
                        d.activities.forEach { Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 2.dp)) }
                    }

                    ExpandableInfoSection("Services (${d.services.size})") {
                        d.services.forEach { Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 2.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpandableInfoSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
    ) {
        Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(Modifier.padding(bottom = 12.dp)) {
                    content()
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
}

@Composable
private fun AppHeader(app: AppInfo) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        AsyncAppIcon(app.packageName, Modifier.size(64.dp))
        Spacer(Modifier.width(16.dp))
        Column {
            Text(app.label, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(app.packageName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
            if (app.isSystemApp) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = CircleShape,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        "System App", 
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FilledTonalIconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
            Icon(icon, label)
        }
        Text(label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.padding(vertical = 4.dp)) {
        Text(
            label, 
            style = MaterialTheme.typography.labelMedium, 
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(120.dp)
        )
        Text(
            value, 
            style = MaterialTheme.typography.bodySmall, 
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun formatDateTime(time: Long, locale: Locale): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", locale)
    return sdf.format(Date(time))
}
