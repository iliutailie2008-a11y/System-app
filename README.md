# System — asistent personal Android (v9, cu build automat pe GitHub Actions)

## Nou: build în cloud, direct de pe telefon (fără laptop)
Am adăugat `.github/workflows/android.yml` — la primul push pe GitHub, un server gratuit al
GitHub compilează automat proiectul și îți dă un APK descărcabil, fără să ai nevoie de laptop
sau Android Studio. Vezi secțiunea "Cum pun proiectul pe GitHub, de pe telefon" mai jos.

## Ce e nou în v8
- **Research dedicat, salvat permanent** — scrii "research dropshipping" sau "research codul P0171",
  System caută amplu pe web (mai multe căutări, nu una singură) și salvează tot într-un **document
  permanent**, nu doar o memorie scurtă. Pentru eCommerce: tendințe, nișe virale, hook-uri, structură
  de pornire pas cu pas. Pentru auto: ce înseamnă codul, cauze posibile, ce verifici primul.
- **Buton "Notițe"** — vezi toate documentele salvate, le deschizi, le copiezi, sau le ștergi
- Vizualul (hartă/grafic/schemă) poate apărea și în timpul unui research general, nu doar în cazuri fixe

## Ce e nou în v7
- **Orb-ul se micșorează într-un colț** când răspunsul are conținut vizual relevant, și revine în
  centru (idle, rotativ) când nu mai e cazul — exact ca în referințele pe care le-ai trimis
- **Hărți contextuale** — pentru un cod poștal/adresă, se deschide o hartă (OpenStreetMap) direct pe ecran
- **Grafic de bare simplu** — pentru comparații eCommerce (preț concurenți, volum de căutări, scor
  recenzii), desenat direct, fără librării externe
- **Panou cu schemă/explicație** — pentru un cod de eroare auto sau structura unei idei de conținut,
  arătat ca text structurat, nu doar vorbit
- Modelul decide singur când e util un vizual, folosind intern tag-ul `[VISUAL:tip|date]`

## Ce e nou în v6
- **Redenumit „System"** — peste tot ce vezi pe ecran (titlu, log conversație, dialoguri), nu mai zice
  „Jarvis". Numele intern al claselor de cod a rămas `Jarvis...` (nu afectează ce vezi tu, doar
  organizarea codului) — dacă vrei să redenumesc și fișierele de cod, spune și fac asta separat.
- **Temă întunecată, stil HUD** — fundal negru-albăstrui, accent cyan, gen Iron Man
- **Sferă wireframe rotativă în fundal** — desenată direct în cod (Canvas), fără imagini externe,
  deci nu are cum să lipsească vreun asset la build. Vezi fișierul `system_preview.html` din chat
  pentru un mockup identic, ca previzualizare rapidă în browser.

## Ce e nou în v5
- **📷 Diagnoză foto (auto)** — apeși butonul, faci poză la un cod de eroare de pe bord sau la o
  schemă electrică, iar Jarvis îți explică pas cu pas, ca unui începător complet
- **📋 Raport** — buton care generează un rezumat de progres pe eCommerce și auto, pe baza memoriei
- **🔔 Reminder** — o notificare zilnică simplă, la ora 9:00, care te împinge înapoi în aplicație
  (nu face research automat, e doar un impuls — conținutul inteligent apare când deschizi aplicația)

## Lăsate pentru mai târziu (au nevoie de lucruri suplimentare)
- **Wake word** ("Hey Jarvis") — necesită o librărie de detecție offline (ex: Porcupine) și o
  configurare separată, plus ascultare continuă în fundal
- **Scriere efectivă de mesaje în alte aplicații** (ex: "trimite un mesaj lui X pe WhatsApp") —
  tehnic posibil prin AccessibilityService, dar fragil: interfața WhatsApp diferă pe versiuni/limbi,
  necesită testare separată ca să nu apese greșit
- **Integrare Shopify live** (statistici reale de vânzări/trafic) — are nevoie de un magazin real
  creat mai întâi, plus credențiale API Shopify

## Important: cum rulează, de fapt
Laptopul e necesar O SINGURĂ DATĂ, pentru compilare + instalare pe telefon (sau când vrei să modifici codul).
După instalare, aplicația rulează 100% independent pe telefon — face research direct de pe internetul
telefonului, fără nicio legătură cu laptopul. Poți lăsa laptopul acasă, stins.

## Ce face acum
- Chat text + voce (STT/TTS în română)
- Research real pe web prin Claude API (tool de web search inclus)
- Poate deschide aplicații, apăsa Back/Home, prin AccessibilityService
- Cheia API se salvează local, în SharedPreferences (nu e trimisă nicăieri altundeva)
- **Memorie persistentă locală (SQLite)** — reține automat fapte durabile pe categorii
  (ecommerce, auto, proiect-jarvis, general) și le injectează în fiecare conversație nouă
- **Personalitate prietenoasă** — vorbește pe "tu", încurajator, ca un prieten priceput
- **Partener creativ eCommerce** — la cerere de idei de conținut, caută întâi tendințe virale
  reale pe web, apoi vine cu hook-uri originale adaptate nișei tale, explicând de ce funcționează
- **Mentor auto pentru începători** — presupune că nu știi termenii, explică din bază, în pași mici,
  verifică înțelegerea, te încurajează să întrebi — gândit pentru cineva care învață singur, acasă
- Buton **Memorie** în aplicație: vezi tot ce a reținut, adaugi manual, sau ștergi tot

## Cum îl pui în funcțiune

1. **Instalează Android Studio** (gratuit): https://developer.android.com/studio
2. **Open Project** → alege folderul `JarvisApp`
3. Lasă Android Studio să descarce Gradle/dependențele (prima sincronizare durează câteva minute)
4. Conectează telefonul prin USB cu **Developer Options** + **USB Debugging** activate
   (Setări → Despre telefon → apasă de 7 ori pe "Build number" → apoi Developer Options)
5. Apasă **Run ▶** — aplicația se instalează pe telefon

## Prima configurare pe telefon
1. La prima pornire, introdu **cheia API Anthropic** (o generezi gratuit pe https://console.anthropic.com → Settings → API Keys)
2. Apasă butonul **"Accesibilitate"** din aplicație → activează manual "Jarvis" din lista de servicii de accesibilitate Android
   (Android cere asta manual, din motive de siguranță — nicio aplicație nu poate activa automat controlul ecranului)
3. La prima comandă vocală, acordă permisiunea de microfon

## Cum funcționează memoria
Nu trebuie să faci nimic special — vorbești normal cu Jarvis despre afacerea ta sau despre proiectul auto,
și el reține singur ce e important (folosind intern un tag `[REMEMBER:categorie|fapt]` pe care aplicația îl
salvează în baza de date locală și îl elimină din ce citește cu voce tare). La următoarea conversație, aceste
fapte sunt incluse automat în context, așa că nu trebuie să-i reamintești.

Exemple care ar declanșa reținere automată:
- "Vând produse de skincare pe un magazin Shopify, targetez piața din România"
- "Învăț la un Golf 4, mă interesează sistemul de injecție"
- "Vreau să fac o aplicație de diagnoză OBD-II ca proiect final"

Poți vedea/gestiona manual din butonul **Memorie**.

## Cum ceri idei de conținut / hook-uri virale (eCommerce)
Scrie ceva de tipul: "dă-mi idei de conținut viral pentru [produsul tău]" sau "ce hook-uri merg acum
pentru nișa de [X]". Jarvis caută întâi ce e viral acum, apoi vine cu idei originale + explicația
psihologică din spatele fiecărui hook, ca să înveți principiul, nu doar să copiezi o idee.

## Găsire nișă (dacă nu ai încă una)
Scrie "ajută-mă să găsesc o nișă" — Jarvis te întreabă despre interese/buget/timp disponibil, apoi
cercetează pe web nișe/produse cu cerere reală acum și vine cu 3-5 opțiuni concrete, cu motivul pentru
fiecare. Platforma implicit recomandată e Shopify, dar poți alege altceva.

## Research de produs (concurență)
Scrie "cercetează produsul X" — Jarvis verifică semnale reale (durata reclamelor, câte magazine îl vând,
tendința de căutare, recenzii) și dă un verdict calitativ ("semne bune"/"risc"), NU o cifră inventată de
tip "rată de câștig". Nu există date publice exacte despre vânzările unui concurent — orice aplicație
care promite asta estimează, nu știe cu certitudine.

## Creare magazin
Jarvis nu poate crea un magazin live (nu are acces la contul tău Shopify), dar generează structura de
pagini, texte de produs, politici, și un checklist exact, pas cu pas, pentru ce trebuie să apeși tu.

## Cum ceri un research amplu, salvat permanent
Scrie "research [ceva]" — de exemplu "research dropshipping" sau "research codul P0171". System caută
pe web mai amplu decât la o întrebare normală, apoi salvează totul într-o notiță pe care o revezi
oricând din butonul **Notițe**. Răspunsul vorbit rămâne scurt — detaliile complete stau în notiță.

## Cum ceri o acțiune
Scrie sau spune, de exemplu:
- "Caută pe internet cine a câștigat ultimul Champions League"
- "Deschide-mi WhatsApp"
- "Du-mă înapoi"

Modelul decide singur când trebuie research (folosește tool-ul web search automat) și când trebuie o acțiune pe telefon
(adaugă intern un cod `[ACTION:...]` pe care aplicația îl execută și îl șterge din ce citește cu voce tare).

## Limitări reale, ca să nu ai surprize
- **Deschide aplicații după package name** — momentan nu poate "aduce niște poze din galerie" sau "apasă butonul X din Instagram";
  poate deschide aplicații, naviga back/home, și citi textul de pe ecran. Interacțiuni fine (click pe un buton anume dintr-o
  aplicație terță) se pot adăuga, dar cer cod suplimentar per-aplicație.
- **Cheia API costă bani** — folosești API-ul Anthropic cu cheia ta, plătești per cerere (foarte ieftin pentru uz personal,
  dar nu e gratuit nelimitat).
- **Accesibilitatea trebuie activată manual** — restricție de siguranță Android, nu se poate evita.
- Nu există momentan "wake word" (ex: "Hey Jarvis") — apeși butonul de microfon. Se poate adăuga cu o librărie
  de detecție offline (ex: Porcupine), dar e un pas separat.

## Ce poți adăuga în continuare
- Wake word / ascultare continuă în fundal (foreground service + Porcupine sau Vosk)
- Acțiuni mai fine: click pe elemente specifice din alte aplicații (folosind `getScreenText()` + coordonate din
  `AccessibilityNodeInfo` pentru a găsi și apăsa un buton anume)
- Widget pe ecranul principal / Quick Settings tile pentru acces rapid
- Istoric persistent al conversației (momentan se pierde la restart)
