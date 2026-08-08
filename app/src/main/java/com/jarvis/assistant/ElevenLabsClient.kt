package com.jarvis.assistant

import android.content.Context
import android.media.MediaPlayer
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

class ElevenLabsClient(private val apiKey: String, private val voiceId: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun synthesizeAndPlay(
        context: Context,
        text: String,
        onDone: () -> Unit,
        onError: (String) -> Unit
    ) {
        val body = JSONObject()
            .put("text", text)
            .put("model_id", "eleven_multilingual_v2")
            .put(
                "voice_settings",
                JSONObject().put("stability", 0.5).put("similarity_boost", 0.8)
            )

        val request = Request.Builder()
            .url("https://api.elevenlabs.io/v1/text-to-speech/$voiceId")
            .addHeader("xi-api-key", apiKey)
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "audio/mpeg")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onError(e.message ?: "Eroare rețea ElevenLabs")
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    onError("Eroare ElevenLabs (${response.code})")
                    return
                }
                try {
                    val bytes = response.body?.bytes()
                    if (bytes == null) {
                        onError("Răspuns gol de la ElevenLabs")
                        return
                    }
                    val file = File(context.cacheDir, "tts_${System.currentTimeMillis()}.mp3")
                    FileOutputStream(file).use { it.write(bytes) }

                    val player = MediaPlayer()
                    player.setDataSource(file.absolutePath)
                    player.setOnCompletionListener {
                        it.release()
                        file.delete()
                        onDone()
                    }
                    player.setOnErrorListener { mp, _, _ ->
                        mp.release()
                        file.delete()
                        onError("Eroare la redarea audio")
                        true
                    }
                    player.prepare()
                    player.start()
                } catch (e: Exception) {
                    onError("Eroare la procesarea audio: ${e.message}")
                }
            }
        })
    }
}
