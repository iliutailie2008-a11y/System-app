package com.jarvis.assistant

import android.Manifest
import android.app.AlarmManager
import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.util.Base64
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.jarvis.assistant.databinding.ActivityMainBinding
import org.json.JSONObject
import java.io.File
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var tts: TextToSpeech
    private lateinit var memoryStore: MemoryStore
    private var apiClient: ClaudeApiClient? = null
    private var photoUri: Uri? = null
    private val history = mutableListOf<Pair<String, String>>()
    private val conversationLog = StringBuilder()

    private val actionRegex = Regex("""\[ACTION:(\{.*?})]""")
    private val rememberRegex = Regex("""\[REMEMBER:([^|\]]+)\|([^]]*)]""")
    private val visualRegex = Regex("""\[VISUAL:([^|\]]+)\|([^]]*)]""")
    private val noteRegex = Regex("""\[NOTE:([^\]]+)]([\s\S]*?)\[/NOTE]""")

    // --- Voce ---
    private val speechLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spoken.isNullOrBlank()) sendMessage(spoken)
        }
    }

    private val micPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startVoiceInput() else toast("Am nevoie de acces la microfon pentru comenzi vocale.")
    }

    // --- Cameră ---
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) photoUri?.let { sendImageMessage(it) }
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchCamera() else toast("Am nevoie de acces la cameră pentru diagnoză foto.")
    }

    // --- Notificări (reminder) ---
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) enableReminder() else toast("Am nevoie de permisiunea de notificări pentru reminder.")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tts = TextToSpeech(this) { }
        tts.language = Locale("ro", "RO")
        memoryStore = MemoryStore(this)

        loadApiKeyOrPrompt()
        updateReminderButtonLabel()

        binding.sendButton.setOnClickListener {
            val text = binding.inputText.text.toString().trim()
            if (text.isNotEmpty()) sendMessage(text)
        }
        binding.micButton.setOnClickListener { checkMicPermissionAndListen() }
        binding.settingsButton.setOnClickListener { promptForApiKey() }
        binding.accessibilityButton.setOnClickListener {
            startActivity(Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        binding.memoryButton.setOnClickListener { showMemoryDialog() }
        binding.notesButton.setOnClickListener { showNotesDialog() }
        binding.cameraButton.setOnClickListener { checkCameraPermissionAndLaunch() }
        binding.reportButton.setOnClickListener {
            sendMessage("Fă-mi un raport scurt de progres, separat pe eCommerce și pe auto, bazat pe ce știi despre mine până acum.")
        }
        binding.reminderButton.setOnClickListener { toggleReminder() }
    }

    // ---------- API KEY ----------

    private fun loadApiKeyOrPrompt() {
        val prefs = getSharedPreferences("jarvis_prefs", MODE_PRIVATE)
        val savedKey = prefs.getString("api_key", null)
        if (savedKey.isNullOrBlank()) promptForApiKey() else apiClient = ClaudeApiClient(savedKey)
    }

    private fun promptForApiKey() {
        val input = AppCompatEditText(this)
        input.hint = "sk-ant-..."
        AlertDialog.Builder(this)
            .setTitle("Cheia API Anthropic")
            .setMessage("Introdu cheia ta API (din console.anthropic.com). Se salvează doar local, pe telefon.")
            .setView(input)
            .setPositiveButton("Salvează") { _, _ ->
                val key = input.text.toString().trim()
                if (key.isNotEmpty()) {
                    getSharedPreferences("jarvis_prefs", MODE_PRIVATE).edit().putString("api_key", key).apply()
                    apiClient = ClaudeApiClient(key)
                    toast("Cheie salvată.")
                }
            }
            .setCancelable(false)
            .show()
    }

    // ---------- MEMORIE ----------

    private fun showMemoryDialog() {
        val memories = memoryStore.getAllMemories()
        val message = if (memories.isEmpty()) {
            "Nu am reținut încă nimic. Vorbește-mi despre afacerea ta sau despre proiectul auto și voi reține automat ce e important."
        } else {
            memories.joinToString("\n\n") { "[${it.first}] ${it.second}" }
        }
        AlertDialog.Builder(this)
            .setTitle("Memoria lui System")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .setNeutralButton("Adaugă manual") { _, _ -> promptManualMemory() }
            .setNegativeButton("Șterge tot") { _, _ ->
                memoryStore.clearAll()
                toast("Memorie ștearsă.")
            }
            .show()
    }

    private fun promptManualMemory() {
        val input = AppCompatEditText(this)
        input.hint = "ecommerce|Vând accesorii de fitness pe Shopify"
        AlertDialog.Builder(this)
            .setTitle("Adaugă fapt manual (categorie|fapt)")
            .setView(input)
            .setPositiveButton("Salvează") { _, _ ->
                val parts = input.text.toString().split("|")
                if (parts.size == 2 && parts[1].isNotBlank()) {
                    memoryStore.addMemory(parts[0], parts[1])
                    toast("Salvat.")
                } else {
                    toast("Format: categorie|fapt")
                }
            }
            .show()
    }

    // ---------- NOTIȚE / DOCUMENTE DE RESEARCH ----------

    private fun showNotesDialog() {
        val notes = memoryStore.getAllNotes()
        if (notes.isEmpty()) {
            toast("Nu ai încă notițe. Scrie ceva de tipul \"research dropshipping\" sau \"research codul X\".")
            return
        }
        val titles = notes.map { it.title }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Notițele tale")
            .setItems(titles) { _, which -> showNoteDetail(notes[which]) }
            .setNegativeButton("Închide", null)
            .show()
    }

    private fun showNoteDetail(note: MemoryStore.Note) {
        AlertDialog.Builder(this)
            .setTitle(note.title)
            .setMessage(note.content)
            .setPositiveButton("OK", null)
            .setNeutralButton("Copiază") { _, _ ->
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Notă System", note.content))
                toast("Copiat.")
            }
            .setNegativeButton("Șterge") { _, _ ->
                memoryStore.deleteNote(note.id)
                toast("Notă ștearsă.")
            }
            .show()
    }

    // ---------- VOCE ----------

    private fun checkMicPermissionAndListen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startVoiceInput()
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ro-RO")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Spune comanda...")
        }
        try {
            speechLauncher.launch(intent)
        } catch (e: Exception) {
            toast("Recunoașterea vocală nu e disponibilă pe acest telefon.")
        }
    }

    // ---------- CAMERĂ / DIAGNOZĂ FOTO ----------

    private fun checkCameraPermissionAndLaunch() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchCamera() {
        val imagesDir = File(cacheDir, "images").apply { mkdirs() }
        val file = File(imagesDir, "diagnostic_${System.currentTimeMillis()}.jpg")
        photoUri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        takePictureLauncher.launch(photoUri)
    }

    private fun sendImageMessage(uri: Uri) {
        val client = apiClient
        if (client == null) {
            toast("Adaugă mai întâi cheia API din Setări.")
            return
        }
        val base64 = uriToBase64(uri)
        if (base64 == null) {
            toast("Nu am putut citi imaginea.")
            return
        }
        val displayText = "[poză trimisă pentru diagnoză auto]"
        appendToLog("Tu: $displayText")

        client.sendImageMessage(
            base64Image = base64,
            prompt = "Explică-mi ce vezi în imagine, ca unui începător complet la mecanică/electrică auto. " +
                "Dacă e un cod de eroare sau o schemă, explică pas cu pas ce înseamnă și ce ar trebui să verific.",
            history = history,
            memoryContext = memoryStore.buildMemoryContext(),
            onResult = { reply -> handleReply(displayText, reply) },
            onError = { error -> runOnUiThread { appendToLog("Eroare: $error"); toast(error) } }
        )
    }

    private fun uriToBase64(uri: Uri): String? {
        return try {
            contentResolver.openInputStream(uri)?.use { input ->
                Base64.encodeToString(input.readBytes(), Base64.NO_WRAP)
            }
        } catch (e: Exception) {
            null
        }
    }

    // ---------- REMINDER ZILNIC ----------

    private fun toggleReminder() {
        val prefs = getSharedPreferences("jarvis_prefs", MODE_PRIVATE)
        val enabled = prefs.getBoolean("reminder_enabled", false)
        if (enabled) {
            disableReminder()
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                enableReminder()
            }
        }
    }

    private fun reminderPendingIntent(): PendingIntent {
        val intent = Intent(this, DailyReminderReceiver::class.java)
        return PendingIntent.getBroadcast(
            this, 2001, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun enableReminder() {
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        val triggerTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP, triggerTime, AlarmManager.INTERVAL_DAY, reminderPendingIntent()
        )
        getSharedPreferences("jarvis_prefs", MODE_PRIVATE).edit().putBoolean("reminder_enabled", true).apply()
        updateReminderButtonLabel()
        toast("Reminder zilnic activat, la 9:00.")
    }

    private fun disableReminder() {
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(reminderPendingIntent())
        getSharedPreferences("jarvis_prefs", MODE_PRIVATE).edit().putBoolean("reminder_enabled", false).apply()
        updateReminderButtonLabel()
        toast("Reminder dezactivat.")
    }

    private fun updateReminderButtonLabel() {
        val enabled = getSharedPreferences("jarvis_prefs", MODE_PRIVATE).getBoolean("reminder_enabled", false)
        binding.reminderButton.text = if (enabled) "Reminder ✓" else "Reminder"
    }

    // ---------- CHAT TEXT ----------

    private fun sendMessage(text: String) {
        val client = apiClient
        if (client == null) {
            toast("Adaugă mai întâi cheia API din Setări.")
            return
        }
        appendToLog("Tu: $text")
        binding.inputText.setText("")

        client.sendMessage(
            userMessage = text,
            history = history,
            memoryContext = memoryStore.buildMemoryContext(),
            onResult = { reply -> handleReply(text, reply) },
            onError = { error -> runOnUiThread { appendToLog("Eroare: $error"); toast(error) } }
        )
    }

    /** Comun pentru răspunsuri text și imagine: parsează tag-uri, salvează memorie, vorbește, arată vizualul. */
    private fun handleReply(userText: String, reply: String) {
        runOnUiThread {
            val parsed = processReply(reply)
            appendToLog("System: ${parsed.text}")
            history.add("user" to userText)
            history.add("assistant" to reply)
            tts.speak(parsed.text, TextToSpeech.QUEUE_FLUSH, null, null)
            parsed.action?.let { JarvisActions.serviceInstance?.executeAction(it) }
            parsed.remembers.forEach { (cat, fact) -> memoryStore.addMemory(cat, fact) }
            parsed.note?.let { (title, content) -> memoryStore.addNote(title, content) }
            if (parsed.visual != null) {
                showVisual(parsed.visual.first, parsed.visual.second)
            } else {
                showIdleSphere()
            }
        }
    }

    private data class ParsedReply(
        val text: String,
        val action: JSONObject?,
        val remembers: List<Pair<String, String>>,
        val visual: Pair<String, String>?,
        val note: Pair<String, String>?
    )

    private fun processReply(reply: String): ParsedReply {
        var cleaned = reply
        var action: JSONObject? = null
        var visual: Pair<String, String>? = null
        var note: Pair<String, String>? = null
        val remembers = mutableListOf<Pair<String, String>>()

        actionRegex.find(reply)?.let { match ->
            action = try { JSONObject(match.groupValues[1]) } catch (e: Exception) { null }
            cleaned = cleaned.replace(match.value, "")
        }

        noteRegex.find(reply)?.let { match ->
            val title = match.groupValues[1].trim()
            val content = match.groupValues[2].trim()
            if (content.isNotEmpty()) note = title to content
            cleaned = cleaned.replace(match.value, "")
        }

        visualRegex.find(reply)?.let { match ->
            val type = match.groupValues[1].trim().lowercase()
            val payload = match.groupValues[2].trim()
            if (payload.isNotEmpty()) visual = type to payload
            cleaned = cleaned.replace(match.value, "")
        }

        rememberRegex.findAll(reply).forEach { match ->
            val category = match.groupValues[1].trim()
            val fact = match.groupValues[2].trim()
            if (fact.isNotEmpty()) remembers.add(category to fact)
            cleaned = cleaned.replace(match.value, "")
        }

        return ParsedReply(cleaned.trim(), action, remembers, visual, note)
    }

    // ---------- STAGE: orb + vizual contextual ----------

    private fun dpToPx(dp: Float): Float = dp * resources.displayMetrics.density

    private fun shrinkSphereToCorner() {
        binding.sphereView.animate()
            .scaleX(0.32f).scaleY(0.32f)
            .translationX(dpToPx(78f))
            .translationY(dpToPx(-70f))
            .setDuration(350)
            .start()
    }

    private fun expandSphereToCenter() {
        binding.sphereView.animate()
            .scaleX(1f).scaleY(1f)
            .translationX(0f).translationY(0f)
            .setDuration(350)
            .start()
    }

    private fun showVisual(type: String, payload: String) {
        binding.mapWebView.visibility = android.view.View.GONE
        binding.chartView.visibility = android.view.View.GONE
        binding.diagramPanel.visibility = android.view.View.GONE

        val recognized = when (type) {
            "map" -> {
                binding.mapWebView.settings.javaScriptEnabled = true
                binding.mapWebView.loadUrl("https://www.openstreetmap.org/search?query=" + Uri.encode(payload))
                binding.mapWebView.visibility = android.view.View.VISIBLE
                true
            }
            "chart" -> {
                val parsed = parsePairs(payload)
                if (parsed.isEmpty()) return showIdleSphere()
                binding.chartView.setData(parsed)
                binding.chartView.visibility = android.view.View.VISIBLE
                true
            }
            "diagram" -> {
                binding.diagramPanel.text = payload
                binding.diagramPanel.visibility = android.view.View.VISIBLE
                true
            }
            else -> false
        }

        if (!recognized) {
            showIdleSphere()
            return
        }

        shrinkSphereToCorner()
        binding.visualContainer.animate().alpha(1f).setDuration(300).start()
    }

    private fun showIdleSphere() {
        binding.visualContainer.animate().alpha(0f).setDuration(250).start()
        expandSphereToCenter()
    }

    private fun parsePairs(payload: String): List<Pair<String, Float>> {
        return payload.split(",").mapNotNull { part ->
            val kv = part.split(":")
            if (kv.size == 2) {
                kv[1].trim().toFloatOrNull()?.let { value -> kv[0].trim() to value }
            } else null
        }
    }

    private fun appendToLog(line: String) {
        conversationLog.append(line).append("\n\n")
        binding.conversationView.text = conversationLog.toString()
        binding.scrollView.post { binding.scrollView.fullScroll(android.view.View.FOCUS_DOWN) }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        tts.shutdown()
        super.onDestroy()
    }
}
