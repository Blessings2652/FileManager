package com.shizuku.filemanager.sys

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class AutomationWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = Result.success()
}
