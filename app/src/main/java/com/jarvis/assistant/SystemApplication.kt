package com.jarvis.assistant

import android.app.Application
import android.content.Intent
import android.util.Log

class SystemApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val stackTrace = Log.getStackTraceString(throwable)
                val intent = Intent(applicationContext, CrashActivity::class.java).apply {
                    putExtra("stack_trace", stackTrace)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
            } catch (e: Exception) {
            }
            android.os.Process.killProcess(android.os.Process.myPid())
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
