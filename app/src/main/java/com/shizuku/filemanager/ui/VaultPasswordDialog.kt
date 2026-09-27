package com.shizuku.filemanager.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation

/**
 * Shared password prompt for both encrypting (VaultManager.encrypt) and
 * decrypting (VaultManager.decrypt) a selected file. The caller decides
 * what "confirm" means for the mode it's in.
 */
@Composable
fun VaultPasswordDialog(
    title: String,
    confirmLabel: String,
    requireConfirmation: Boolean,
    errorText: String?,
    onDismiss: () -> Unit,
    onConfirm: (password: CharArray) -> Unit,
) {
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    val mismatch = requireConfirmation && confirm.isNotEmpty() && password != confirm

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                if (requireConfirmation) {
                    OutlinedTextField(
                        value = confirm,
                        onValueChange = { confirm = it },
                        label = { Text("Confirm password") },
                        visualTransformation = PasswordVisualTransformation(),
                        isError = mismatch,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (mismatch) Text("Passwords don't match")
                if (errorText != null) Text(errorText)
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(password.toCharArray()) },
                enabled = password.isNotEmpty() && !mismatch && (!requireConfirmation || confirm.isNotEmpty())
            ) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
