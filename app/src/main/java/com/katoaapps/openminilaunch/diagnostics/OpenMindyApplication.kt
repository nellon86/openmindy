package com.katoaapps.openminilaunch.diagnostics

import android.app.Application
import android.content.Intent
import android.os.Build
import android.os.Process
import com.katoaapps.openminilaunch.BuildConfig
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.system.exitProcess

/**
 * Records any crash of the launcher process and shows it in [CrashReportActivity], which runs in
 * its own process, so the error can be read and copied even when Home cannot start.
 */
class OpenMindyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (isCrashReporterProcess()) return
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching {
                val trace = StringWriter().also { error.printStackTrace(PrintWriter(it)) }.toString()
                val report = buildString {
                    appendLine("OpenMindy ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                    appendLine("Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}) · ${Build.MANUFACTURER} ${Build.MODEL}")
                    appendLine("Thread: ${thread.name}")
                    appendLine()
                    append(trace)
                }
                crashFile(this).writeText(report)
                startActivity(
                    Intent(this, CrashReportActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
                )
            }
            if (previous != null && thread.name != "main") {
                previous.uncaughtException(thread, error)
            } else {
                Process.killProcess(Process.myPid())
                exitProcess(10)
            }
        }
    }

    private fun isCrashReporterProcess(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && getProcessName().endsWith(":crash")

    companion object {
        fun crashFile(context: android.content.Context): File = File(context.filesDir, "last_crash.txt")
    }
}
