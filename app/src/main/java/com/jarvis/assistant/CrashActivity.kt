package com.jarvis.assistant

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class CrashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val stackTrace = intent.getStringExtra("stack_trace") ?: "Nicio eroare capturată."

        val textView = TextView(this).apply {
            text = stackTrace
            setTextColor(android.graphics.Color.WHITE)
            setPadding(24, 24, 24, 24)
            textSize = 12f
        }

        val scrollView = ScrollView(this).apply {
            addView(textView)
        }

        val copyButton = Button(this).apply {
            text = "Copiază eroarea"
            setOnClickListener {
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Eroare System", stackTrace))
                Toast.makeText(this@CrashActivity, "Copiat.", Toast.LENGTH_SHORT).show()
            }
        }

        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setBackgroundColor(android.graphics.Color.parseColor("#05070A"))
            addView(copyButton)
            addView(scrollView, android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT
            ))
        }

        setContentView(layout)
    }
}
