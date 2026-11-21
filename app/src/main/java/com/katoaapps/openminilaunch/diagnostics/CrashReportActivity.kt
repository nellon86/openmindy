package com.katoaapps.openminilaunch.diagnostics

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.katoaapps.openminilaunch.R

/** Plain-views screen (no Compose, separate process) that shows the last recorded crash. */
class CrashReportActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val report = runCatching { OpenMindyApplication.crashFile(this).readText() }
            .getOrDefault("")
        val padding = (16 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding * 2, padding, padding)
        }
        root.addView(TextView(this).apply {
            text = getString(R.string.crash_report_title)
            textSize = 20f
            setTypeface(typeface, Typeface.BOLD)
        })
        root.addView(TextView(this).apply {
            text = getString(R.string.crash_report_description)
            setPadding(0, padding / 2, 0, padding / 2)
        })
        val buttons = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        buttons.addView(Button(this).apply {
            text = getString(R.string.crash_report_copy)
            setOnClickListener {
                getSystemService(ClipboardManager::class.java)
                    ?.setPrimaryClip(ClipData.newPlainText("OpenMindy crash", report))
                Toast.makeText(this@CrashReportActivity, R.string.crash_report_copied, Toast.LENGTH_SHORT).show()
            }
        })
        buttons.addView(Button(this).apply {
            text = getString(R.string.crash_report_share)
            setOnClickListener {
                runCatching {
                    startActivity(
                        Intent.createChooser(
                            Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, report),
                            null,
                        ),
                    )
                }
            }
        })
        root.addView(buttons)
        root.addView(
            ScrollView(this).apply {
                addView(TextView(this@CrashReportActivity).apply {
                    text = report
                    typeface = Typeface.MONOSPACE
                    textSize = 11f
                    setTextIsSelectable(true)
                })
            },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f),
        )
        setContentView(root)
    }
}
