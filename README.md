# ASSISTANT VOICE

**Bilingual Sinhala, Singlish & English Native Android AI Voice Assistant**  
Developed by **MADUSANKA** | Package: `com.madusanka.assistantvoice` | Min SDK: Android 8.0 (API 26)

---

## Features

- **Gemini AI Brain (Firebase AI Logic + App Check):**
  - Understands natural **Sinhala (සිංහල)**, **Singlish** (e.g. `"YouTube eka open karanna"`), and **English**.
  - Supports multi-turn conversation context, translation, general knowledge Q&A, and structured command routing.
  - Zero hardcoded API keys in source code or APK — uses **Firebase AI Logic (`Firebase.ai`)** protected by **Firebase App Check** (`DebugAppCheckProviderFactory` in debug builds, `RecaptchaAppCheckProviderFactory` in release builds).
- **Native Voice Recognition & Text-to-Speech:**
  - Real-time voice capture via Android `SpeechRecognizer` with live RMS audio waveform animation.
  - One-tap quick toggle between Sinhala (`si-LK`) and English (`en-US`) speech input.
  - Spoken replies via Android `TextToSpeech` with voice selection, speed, pitch, and auto-speak controls.
  - Graceful fallback and clear messaging if Google Speech Services or Sinhala TTS voice packs are missing.
- **Jarvis System-Wide Mode (Control Outside the App):**
  - **Floating Jarvis Overlay (`JarvisOverlayActivity`):** Translucent dark-glass AI orb sheet that pops up directly over any open app and listens immediately.
  - **System Default Digital Assistant (`ACTION_ASSIST`):** Set Assistant Voice as your phone's default assistant to summon Jarvis via Power-hold or bottom-corner swipe from anywhere.
  - **Quick Settings Tile (`JarvisTileService`):** Swipe down Quick Settings on any screen and tap **Jarvis Voice**.
  - **Home Screen Jarvis Widget (`JarvisWidgetProvider`):** Pin the neon Jarvis Mic & Flashlight bar to your home screen.
  - **Jarvis Standby Notification (`JarvisStandbyService`):** Optional persistent notification shade & lock-screen bar with instant **Speak**, **Flashlight**, and **Stop** actions.
- **Native Android Phone Controls:**
  - Open installed apps across Samsung, Xiaomi/Redmi, Vivo, Oppo, Realme, Huawei, Motorola, and Pixel devices (YouTube, WhatsApp, Facebook, Chrome, and any launcher app).
  - Open Camera and System Settings.
  - Toggle Flashlight ON/OFF with hardware detection (`CameraManager.setTorchMode`).
  - Read Battery percentage & charging status, current Time & Date, and Internet connection status.
  - Adjust media volume (`AudioManager`), launch Google Search, open Phone Dialer, and prepare SMS messages.
- **Futuristic Neon Material 3 UI & DataStore Settings:**
  - Black & dark purple theme with neon cyan and electric purple accents (plus Light theme toggle in Settings).
  - Animated Canvas AI Orb, Voice Waveform strip, rounded chat bubbles with verified action badges, and full Settings persisted via Jetpack DataStore.

---

## Project Architecture

```text
app/src/main/java/com/example/
├── MainActivity.kt                    # App Check init, edge-to-edge, Compose Navigation
├── AppCheckHelper.kt                  # Firebase App Check debug & release configuration
├── ai/GeminiRepository.kt             # Firebase AI Logic (Gemini 3.8 Flash / 3.5 Lite / 4 Argon)
├── actions/PhoneActionManager.kt      # Native Android intents, CameraManager, AudioManager, Battery, Network
├── voice/VoiceRecognitionManager.kt   # SpeechRecognizer with real-time RMS & bilingual error handling
├── voice/TextToSpeechManager.kt       # Android TextToSpeech with Sinhala/English routing & voice picker
├── data/AppSettings.kt                # Jetpack DataStore Preferences repository
├── viewmodel/AssistantViewModel.kt    # MVVM state orchestration & verified action execution
└── ui/
    ├── components/AiOrb.kt            # Animated futuristic orbital AI sphere
    ├── components/VoiceWaveform.kt    # Reactive audio waveform & status indicator
    ├── components/ChatBubble.kt       # Chat bubbles with verified action status badges
    ├── screens/AssistantScreen.kt     # Main voice & chat interface
    ├── screens/SettingsScreen.kt      # Gemini, voice, language, theme, privacy & diagnostics
    └── theme/                         # Color.kt, Theme.kt, Type.kt (Space Grotesk, Plus Jakarta Sans, JetBrains Mono)
```

---

## Security & Gemini AI Configuration

1. **Production & Personal Testing (Recommended — Zero API Keys in APK):**
   - This app uses **Firebase AI Logic (`com.google.firebase:firebase-ai`)** and **Firebase App Check**.
   - Never hardcode a private Gemini API key in Kotlin files, `strings.xml`, `build.gradle.kts`, or commit `.env` / `local.properties` to GitHub.
   - `app/google-services.json` only contains public Firebase client identifiers. Access to Gemini is guarded by **Firebase App Check** (Debug token for local testing, reCAPTCHA Enterprise for release builds).
   - For personal testing on a physical device with a debug APK:
     1. Run the app once and check Android Studio Logcat for `DebugAppCheckProviderFactory` (or pass `FIREBASE_APPCHECK_DEBUG_TOKEN` via launch intent).
     2. Register that debug token in **Firebase Console → App Check → Apps → Manage debug tokens**.
     3. Open **Settings → Gemini AI Configuration → Test Gemini Connection** inside the app to verify connectivity.

---

## Step-by-Step Build & GitHub Actions Guide

No `npm install` or Node.js commands are required. Everything builds natively with Gradle and JDK 17.

### 1. Create a GitHub Repository
1. Sign in to [GitHub](https://github.com) and click **New repository** (`+` → **New repository**).
2. Name it `assistant-voice` (Public or Private).
3. Do not add an extra README or `.gitignore` if uploading this complete project directly.

### 2. Upload the Project to GitHub
Using Git from your computer inside the extracted project folder:
```bash
git init
git add .
git commit -m "Initial commit: Assistant Voice Android app"
git branch -M main
git remote add origin https://github.com/<YOUR_USERNAME>/assistant-voice.git
git push -u origin main
```
*(Ensure `.env` and `debug.keystore` remain excluded by `.gitignore`.)*

### 3. Run GitHub Actions
- The workflow at `.github/workflows/android-apk.yml` triggers **automatically** whenever you push to `main` or `master`.
- To trigger it manually:
  1. Open your GitHub repository → **Actions** tab.
  2. Select **Build Assistant Voice Debug APK**.
  3. Click **Run workflow** → **Run workflow**.

### 4. Download the Built APK Artifact
1. Wait for the **Build Debug APK** job to finish with a green checkmark.
2. Click on the completed workflow run.
3. Scroll down to **Artifacts** and download **`Assistant-Voice-APK`**.
4. Extract the downloaded ZIP to get `app-debug.apk`.

### 5. Install the APK on Your Android Phone
1. Transfer `app-debug.apk` to your Android phone (Android 8.0 or newer).
2. Tap `app-debug.apk` in your phone's **Files** app.
3. If prompted, enable **Install unknown apps** for your Files/browser app.
4. Tap **Install**, then open **Assistant Voice** and grant Microphone permission when asked.
