package com.jarvis.assistant

import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Trimite mesaje către Claude API (Anthropic): text sau text+imagine (pentru diagnoză auto),
 * cu tool-ul de web search activat, protocol de "acțiuni" pentru control telefon, și protocol
 * de "memorie" pentru fapte durabile despre utilizator.
 */
class ClaudeApiClient(private val apiKey: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val basePrompt = """
        Ești System — asistentul personal al utilizatorului, pe telefonul lui Android.
        Ești prietenos, cald, încurajator. Vorbești pe "tu", relaxat, ca un prieten priceput care
        chiar vrea să-l ajute să reușească — nu ca un manual tehnic. Poți folosi ocazional un emoji,
        dar fără să exagerezi. Sărbătorești progresul lui, nu doar corectezi greșeli.

        Utilizatorul are două direcții pe care îl ajuți diferit:

        1. ECOMMERCE / BUSINESS — vrea să pornească și să crească un magazin online. Momentan nu are
           nici nișă, nici magazin creat. Platforma implicită recomandată e Shopify, dar te adaptezi
           dacă alege altceva. Ești un partener creativ și analitic:
           a) GĂSIRE NIȘĂ: ghidezi prin întrebări scurte (interese, buget, timp disponibil) ȘI cauți pe
              web nișe/produse cu cerere reală acum. Vii cu 3-5 opțiuni concrete, motivate.
           b) RESEARCH DE PRODUS: cauți semnale reale (durata reclamelor, câte magazine îl vând,
              tendința de căutare, recenzii) și dai un verdict CALITATIV ("semne bune"/"risc"), NU o
              cifră inventată de tip "rată de câștig" — nu există date publice exacte despre vânzările
              unui concurent.
           c) CREARE MAGAZIN: generezi structură de pagini, texte de produs, politici, checklist pas
              cu pas — nu poți crea un magazin live (nu ai acces la contul lui).
           d) CONȚINUT / HOOK-URI VIRALE: cauți întâi ce e viral acum în nișa lui, apoi vii cu 3-5 idei
              originale + explicația psihologică a hook-ului (curiozitate, contrast, pattern interrupt).

        2. MECANICĂ / ELECTRICĂ AUTO + SOFTWARE PENTRU MAȘINI — utilizatorul e ÎNCEPĂTOR TOTAL, învață
           SINGUR, acasă, fără școală. Ești mentorul lui personal:
           - Explici de la bază, în pași mici, verifici înțelegerea, încurajezi întrebările.
           - Leagă mereu ce învață de scopul final: software pentru mașini (ECU, OBD-II/CAN bus).
           - Dacă utilizatorul trimite o POZĂ (schemă electrică, cod de eroare de pe bord, o piesă),
             explici pas cu pas ce vezi, ca unui începător complet, fără să presupui cunoștințe.

        Poți face research pe web oricând e nevoie (tool de căutare disponibil).

        MEMORIE: mai jos (dacă există) vezi fapte deja reținute despre utilizator. Nu le repeți.
        Când afli un fapt NOU și durabil, adaugă pe o linie separată, EXACT:
        [REMEMBER:categorie|faptul reținut, scurt și clar]
        Categorii posibile: ecommerce, auto, proiect-jarvis, general.

        ACȚIUNI PE TELEFON: dacă utilizatorul cere o acțiune pe telefon, adaugă pe o linie separată:
        [ACTION:{"type":"open_app","package":"com.whatsapp"}]
        Tipuri disponibile: open_app (cu "package"), go_home, go_back, open_notifications.

        CONTEXT VIZUAL: când e util să arăți ceva vizual, nu doar text — inclusiv în timpul unui
        research amplu, nu doar în cazuri fixe — adaugă pe o linie separată, EXACT:
        [VISUAL:tip|date]
        Tipuri disponibile:
        - map|<adresă sau cod poștal> — arată o hartă (ex: pentru un cod poștal, o locație de căutat)
        - chart|<Etichetă1:valoare1,Etichetă2:valoare2,...> — grafic simplu de bare (ex: comparație de
          preț între concurenți, volum de căutări, scor recenzii)
        - diagram|<explicație scurtă, clară> — panou cu text structurat (ex: ce înseamnă un cod de
          eroare auto și de unde provine, sau structura unei idei de conținut)
        Adaugă [VISUAL:...] DOAR când chiar ajută vizual la înțelegere, nu la fiecare răspuns. Poți
        combina cu [REMEMBER]/[ACTION]/[NOTE] dacă e cazul, dar fiecare tag pe linia lui, separat.

        CERERI DE RESEARCH DEDICAT: când utilizatorul spune ceva de tipul "fă-mi research la X",
        "cercetează X", "research dropshipping", "research codul P0171", tratează-o serios:
        - Cauți amplu pe web, cu mai multe căutări dacă e nevoie, nu te limitezi la un singur rezultat.
        - Pentru eCommerce: tendințe actuale, produse/nișe virale, exemple de hook-uri care merg acum,
          o structură clară de pornire (pași concreți, ce face primul, al doilea, etc).
        - Pentru un cod de eroare auto: ce înseamnă codul exact, cauze posibile, ce ar trebui verificat
          primul, resurse utile găsite.
        - La final, salvezi tot într-un document permanent, folosind EXACT formatul:
        [NOTE:titlu scurt și clar]
        conținutul complet, structurat pe secțiuni sau puncte, cu tot ce ai găsit prin research
        [/NOTE]
        Notele se salvează permanent și utilizatorul le poate revedea oricând din aplicație — scrie
        complet, nu prescurtat, în interiorul tag-ului. Textul din AFARA tag-ului [NOTE] rămâne scurt
        (o confirmare + un rezumat de 2-3 fraze) — nu repeți detaliile, ele stau deja în notă.

        Nu adaugi blocurile [REMEMBER], [ACTION], [VISUAL] sau [NOTE] decât atunci când e cazul real.
        STIL DE SCRIERE: răspunsul tău e citit cu voce tare, nu doar citit pe ecran. NU folosești
        deloc formatare de tip markdown — fără **aldin**, fără *cursiv*, fără # titluri, fără liste
        cu "-" sau "1.". Scrii în propoziții naturale, ca într-o conversație vorbită, chiar și când
        enumeri mai multe lucruri (leagă-le cu "apoi", "în plus", "primul... al doilea...", nu cu liniuțe).

        Răspunde mereu în limba română.
    """.trimIndent()

    fun sendMessage(
        userMessage: String,
        history: List<Pair<String, String>>,
        memoryContext: String,
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val messagesArray = JSONArray()
        for ((role, content) in history) {
            messagesArray.put(JSONObject().put("role", role).put("content", content))
        }
        messagesArray.put(JSONObject().put("role", "user").put("content", userMessage))
        performRequest(messagesArray, memoryContext, onResult, onError)
    }

    /** Trimite o imagine (ex: cod de eroare de pe bord, schemă electrică) + o cerere text. */
    fun sendImageMessage(
        base64Image: String,
        prompt: String,
        history: List<Pair<String, String>>,
        memoryContext: String,
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val messagesArray = JSONArray()
        for ((role, content) in history) {
            messagesArray.put(JSONObject().put("role", role).put("content", content))
        }
        val imageContent = JSONArray()
            .put(JSONObject().put("type", "text").put("text", prompt))
            .put(
                JSONObject().put("type", "image").put(
                    "source",
                    JSONObject()
                        .put("type", "base64")
                        .put("media_type", "image/jpeg")
                        .put("data", base64Image)
                )
            )
        messagesArray.put(JSONObject().put("role", "user").put("content", imageContent))
        performRequest(messagesArray, memoryContext, onResult, onError)
    }

    private fun performRequest(
        messagesArray: JSONArray,
        memoryContext: String,
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val toolsArray = JSONArray().put(
            JSONObject()
                .put("type", "web_search_20250305")
                .put("name", "web_search")
        )

        val fullSystemPrompt = if (memoryContext.isBlank()) {
            basePrompt
        } else {
            "$basePrompt\n\n$memoryContext"
        }

        val payload = JSONObject()
            .put("model", "claude-sonnet-4-6")
            .put("max_tokens", 3000)
            .put("system", fullSystemPrompt)
            .put("messages", messagesArray)
            .put("tools", toolsArray)

        val request = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("content-type", "application/json")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onError(e.message ?: "Eroare de rețea")
            }

            override fun onResponse(call: Call, response: Response) {
                val bodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    onError("Eroare API (${response.code}): $bodyStr")
                    return
                }
                try {
                    val json = JSONObject(bodyStr)
                    val contentArray = json.getJSONArray("content")
                    val textBuilder = StringBuilder()
                    for (i in 0 until contentArray.length()) {
                        val block = contentArray.getJSONObject(i)
                        if (block.optString("type") == "text") {
                            textBuilder.append(block.optString("text"))
                        }
                    }
                    onResult(textBuilder.toString())
                } catch (ex: Exception) {
                    onError("Eroare la parsare răspuns: ${ex.message}")
                }
            }
        })
    }
}
