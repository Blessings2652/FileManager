package com.shizuku.filemanager.ui

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shizuku.filemanager.fs.FileEntry
import com.shizuku.filemanager.fs.engine.*
import com.shizuku.filemanager.sys.BiometricPromptManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafeFolderScreen(
    engineType: EngineType,
    activeEngine: FileEngine?,
    onBack: () -> Unit,
    onSafOpen: () -> Unit,
    onSwitchEngine: (EngineType?) -> Unit,
    onOpenFile: (FileEntry, FileEngine) -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val lifecycleOwner = LocalLifecycleOwner.current

    var isUnlocked by remember { mutableStateOf(false) }
    val isSetup by remember { mutableStateOf(EnginePrefs.isVaultEnabled(context)) }

    // Enforce FLAG_SECURE: Blocks screen capture, recording, mirroring, and task switcher previews
    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    // Auto-lock when switching away from the app or pausing
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                isUnlocked = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (!isUnlocked) {
        UnlockScreen(
            isSetup = isSetup,
            onUnlocked = { isUnlocked = true },
            onBack = onBack
        )
    } else {
        SafeFolderContent(
            engineType = engineType,
            activeEngine = activeEngine,
            onBack = { isUnlocked = false },
            onSafOpen = onSafOpen,
            onSwitchEngine = onSwitchEngine,
            onOpenFile = onOpenFile
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnlockScreen(
    isSetup: Boolean,
    onUnlocked: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val savedType = remember(isSetup) { if (isSetup) EnginePrefs.getVaultType(context) else "PIN" }
    var lockType by remember { mutableStateOf(savedType) }

    var secret by remember { mutableStateOf("") }
    var confirmSecret by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var showSecret by remember { mutableStateOf(false) }

    val canBiometric = remember(context) { BiometricPromptManager.canAuthenticate(context) }

    val fragmentActivity = activity as? FragmentActivity

    val triggerBiometric = {
        if (fragmentActivity != null) {
            BiometricPromptManager.showBiometricPrompt(
                activity = fragmentActivity,
                title = "Unlock Safe Folder",
                subtitle = "Touch fingerprint sensor or scan face",
                onSuccess = onUnlocked,
                onError = { err -> error = err }
            )
        }
    }

    LaunchedEffect(isSetup) {
        if (isSetup && canBiometric) {
            triggerBiometric()
        }
    }

    ScreenScaffold(
        topBar = {
            ModernTopBar(
                title = "Safe Folder",
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
                .fillMaxSize()
                .padding(padding)
                .padding(32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.size(120.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Lock,
                        null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(Modifier.height(32.dp))
            Text(
                text = if (isSetup) "Unlock Safe Folder" else "Set up Safe Folder",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = if (isSetup) "Enter your ${lockType.lowercase()} or use biometrics to unlock" else "Choose how you want to secure your files",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            if (!isSetup) {
                Spacer(Modifier.height(32.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    FilterChip(
                        selected = lockType == "PIN",
                        onClick = { lockType = "PIN"; secret = ""; confirmSecret = "" },
                        label = { Text("PIN") }
                    )
                    Spacer(Modifier.width(16.dp))
                    FilterChip(
                        selected = lockType == "PASSWORD",
                        onClick = { lockType = "PASSWORD"; secret = ""; confirmSecret = "" },
                        label = { Text("Password") }
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            if (isSetup && canBiometric) {
                OutlinedButton(
                    onClick = triggerBiometric,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "Biometrics",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Unlock with Fingerprint / Face ID",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.height(24.dp))
            }

            OutlinedTextField(
                value = secret,
                onValueChange = {
                    if (lockType == "PIN") {
                        if (it.length <= 8 && it.all { c -> c.isDigit() }) secret = it
                    } else {
                        secret = it
                    }
                },
                label = { Text(if (lockType == "PIN") "PIN" else "Password") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (lockType == "PIN") KeyboardType.NumberPassword else KeyboardType.Password
                ),
                visualTransformation = if (showSecret) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showSecret = !showSecret }) {
                        Icon(if (showSecret) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )

            if (!isSetup) {
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = confirmSecret,
                    onValueChange = {
                        if (lockType == "PIN") {
                            if (it.length <= 8 && it.all { c -> c.isDigit() }) confirmSecret = it
                        } else {
                            confirmSecret = it
                        }
                    },
                    label = { Text("Confirm ${if (lockType == "PIN") "PIN" else "Password"}") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (lockType == "PIN") KeyboardType.NumberPassword else KeyboardType.Password
                    ),
                    visualTransformation = if (showSecret) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
            }

            if (error != null) {
                Text(
                    error!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(Modifier.height(32.dp))

            val canSubmit = if (isSetup) {
                secret.isNotEmpty()
            } else {
                if (lockType == "PIN") secret.length >= 4 && secret == confirmSecret
                else secret.length >= 8 && secret == confirmSecret
            }

            GradientButton(
                text = if (isSetup) "Unlock with $lockType" else "Set $lockType",
                onClick = {
                    if (isSetup) {
                        if (EnginePrefs.verifyVaultPin(context, secret)) {
                            onUnlocked()
                        } else {
                            error = "Incorrect $lockType"
                        }
                    } else {
                        EnginePrefs.setVaultType(context, lockType)
                        EnginePrefs.setVaultPin(context, secret)
                        EnginePrefs.setVaultEnabled(context, true)
                        onUnlocked()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = canSubmit
            )
        }
    }
}

@Composable
private fun SafeFolderContent(
    engineType: EngineType,
    activeEngine: FileEngine?,
    onBack: () -> Unit,
    onSafOpen: () -> Unit,
    onSwitchEngine: (EngineType?) -> Unit,
    onOpenFile: (FileEntry, FileEngine) -> Unit
) {
    var safePath by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val engine = when (engineType) {
        EngineType.SHIZUKU -> remember { ShizukuFileEngine() }
        EngineType.STANDARD -> remember { StandardFileEngine(context) }
        EngineType.ROOT -> remember { RootFileEngine() }
        EngineType.SAF -> activeEngine
    }

    LaunchedEffect(engine) {
        if (engine == null && engineType == EngineType.SAF) {
            // Handled by return below
        } else if (engine != null) {
            engine.mkdir(engine.rootPath, ".safe").onSuccess {
                safePath = it
                error = null
            }.onFailure {
                error = it.message ?: "Could not create safe folder"
            }
        }
    }

    if (engine == null && engineType == EngineType.SAF) {
        SafPickerScreen(onPickFolder = onSafOpen, onPickDifferentEngine = { onSwitchEngine(null) })
        return
    }

    if (error != null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Error: $error", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(24.dp))
        }
        return
    }

    if (safePath == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    FileBrowserScreen(
        engine = engine!!,
        onOpenDrawer = onBack,
        initialPath = safePath,
        onOpenFile = onOpenFile,
        lockedRoot = safePath,
        vm = viewModel(
            key = "safe_" + engine.type.name + safePath,
            factory = FileBrowserViewModel.Factory(
                LocalContext.current.applicationContext as Application,
                engine,
                safePath,
                forceShowHidden = true,
                lockedRoot = safePath
            )
        )
    )
}

private fun Context.findActivity(): ComponentActivity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is ComponentActivity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
