package com.jarvis.assistant

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import org.json.JSONObject

/**
 * Serviciul care dă control real lui Jarvis pe telefon: poate deschide aplicații,
 * naviga (back/home), și citi textul vizibil pe ecran ca să dea context modelului.
 *
 * Trebuie activat manual de utilizator din Setări > Accesibilitate, o singură dată.
 */
class JarvisAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        JarvisActions.serviceInstance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Momentan nu reacționăm automat la fiecare eveniment de ecran.
        // Aici se poate adăuga logică (ex: notificare -> trimite context la Claude).
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        if (JarvisActions.serviceInstance == this) {
            JarvisActions.serviceInstance = null
        }
    }

    fun executeAction(actionJson: JSONObject) {
        when (actionJson.optString("type")) {
            "open_app" -> openApp(actionJson.optString("package"))
            "go_home" -> performGlobalAction(GLOBAL_ACTION_HOME)
            "go_back" -> performGlobalAction(GLOBAL_ACTION_BACK)
            "open_notifications" -> performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
        }
    }

    private fun openApp(packageName: String) {
        if (packageName.isBlank()) return
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName) ?: return
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(launchIntent)
    }

    /**
     * Extrage tot textul vizibil de pe ecran, util când utilizatorul cere ceva
     * de tipul "ce e pe ecranul ăsta" sau "răspunde la mesajul ăsta".
     */
    fun getScreenText(): String {
        val root = rootInActiveWindow ?: return ""
        val builder = StringBuilder()
        collectText(root, builder)
        return builder.toString()
    }

    private fun collectText(node: AccessibilityNodeInfo, builder: StringBuilder) {
        if (!node.text.isNullOrBlank()) {
            builder.append(node.text).append("\n")
        }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { collectText(it, builder) }
        }
    }
}
