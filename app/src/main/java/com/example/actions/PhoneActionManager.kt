package com.example.actions

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class ActionResult(
    val success: Boolean,
    val badgeTitle: String,
    val messageEn: String,
    val messageSi: String
)

enum class VolumeAction {
    UP, DOWN, MUTE, UNMUTE
}

enum class MediaControlAction {
    PLAY_PAUSE, NEXT, PREVIOUS
}

data class DeviceTelemetry(
    val batteryPercent: Int,
    val isCharging: Boolean,
    val isOnline: Boolean,
    val networkType: String,
    val hasFlashlight: Boolean,
    val isFlashlightOn: Boolean
)

class PhoneActionManager(private val context: Context) {

    companion object {
        private const val TAG = "PhoneActionManager"

        private val KNOWN_APP_PACKAGES = mapOf(
            "youtube" to listOf("com.google.android.youtube", "app.revanced.android.youtube"),
            "whatsapp" to listOf("com.whatsapp", "com.whatsapp.w4b"),
            "facebook" to listOf("com.facebook.katana", "com.facebook.lite"),
            "chrome" to listOf("com.android.chrome", "com.chrome.beta"),
            "instagram" to listOf("com.instagram.android"),
            "maps" to listOf("com.google.android.apps.maps"),
            "spotify" to listOf("com.spotify.music"),
            "tiktok" to listOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill"),
            "gmail" to listOf("com.google.android.gm"),
            "play store" to listOf("com.android.vending"),
            "calculator" to listOf("com.google.android.calculator", "com.sec.android.app.popupcalculator", "com.miui.calculator"),
            "clock" to listOf("com.google.android.deskclock", "com.sec.android.app.clockpackage", "com.android.deskclock"),
            "gallery" to listOf("com.google.android.apps.photos", "com.sec.android.gallery3d", "com.miui.gallery")
        )

        private val SINHALA_APP_ALIASES = mapOf(
            "යූටියුබ්" to "youtube",
            "වට්ස්ඇප්" to "whatsapp",
            "ෆේස්බුක්" to "facebook",
            "ක්‍රෝම්" to "chrome",
            "කැමරාව" to "camera",
            "කැමරා" to "camera",
            "සෙටින්ග්ස්" to "settings",
            "මැප්" to "maps",
            "ගැලරිය" to "gallery"
        )
    }

    private val cameraManager: CameraManager? =
        context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager

    private val audioManager: AudioManager? =
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private val connectivityManager: ConnectivityManager? =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val _isFlashlightOn = MutableStateFlow(false)
    val isFlashlightOn: StateFlow<Boolean> = _isFlashlightOn.asStateFlow()

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            _isFlashlightOn.value = enabled
        }

        override fun onTorchModeUnavailable(cameraId: String) {
            _isFlashlightOn.value = false
        }
    }

    init {
        try {
            cameraManager?.registerTorchCallback(torchCallback, null)
        } catch (e: Exception) {
            Log.w(TAG, "Unable to register TorchCallback", e)
        }
    }

    fun cleanup() {
        try {
            cameraManager?.unregisterTorchCallback(torchCallback)
        } catch (e: Exception) {
            Log.w(TAG, "Unable to unregister TorchCallback", e)
        }
    }

    fun hasFlashlightHardware(): Boolean {
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            return false
        }
        return findFlashCameraId() != null
    }

    private fun findFlashCameraId(): String? {
        val manager = cameraManager ?: return null
        return try {
            val ids = manager.cameraIdList
            // Prefer back-facing camera with flash
            ids.firstOrNull { id ->
                val chars = manager.getCameraCharacteristics(id)
                val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                hasFlash && facing == CameraCharacteristics.LENS_FACING_BACK
            } ?: ids.firstOrNull { id ->
                val chars = manager.getCameraCharacteristics(id)
                chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error inspecting camera flash characteristics", e)
            null
        }
    }

    fun setFlashlight(enabled: Boolean): ActionResult {
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            return ActionResult(
                success = false,
                badgeTitle = "Flashlight Unavailable",
                messageEn = "This device does not have flashlight hardware.",
                messageSi = "මෙම දුරකථනයේ ෆ්ලෑෂ්ලයිට් පහසුකම නොමැත."
            )
        }

        val manager = cameraManager
        val cameraId = findFlashCameraId()
        if (manager == null || cameraId == null) {
            return ActionResult(
                success = false,
                badgeTitle = "Flashlight Unavailable",
                messageEn = "Could not access a camera flash unit on this device.",
                messageSi = "දුරකථනයේ ෆ්ලෑෂ්ලයිට් ඒකකයට ප්‍රවේශ විය නොහැක."
            )
        }

        return try {
            manager.setTorchMode(cameraId, enabled)
            _isFlashlightOn.value = enabled
            if (enabled) {
                ActionResult(
                    success = true,
                    badgeTitle = "Flashlight ON",
                    messageEn = "Flashlight has been turned on.",
                    messageSi = "ෆ්ලෑෂ්ලයිට් එක ක්‍රියාත්මක කළා."
                )
            } else {
                ActionResult(
                    success = true,
                    badgeTitle = "Flashlight OFF",
                    messageEn = "Flashlight has been turned off.",
                    messageSi = "ෆ්ලෑෂ්ලයිට් එක නිවා දැමුවා."
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to toggle flashlight", e)
            ActionResult(
                success = false,
                badgeTitle = "Flashlight Error",
                messageEn = "Could not change flashlight state: ${e.localizedMessage ?: "Camera in use"}",
                messageSi = "ෆ්ලෑෂ්ලයිට් එක පාලනය කිරීමට නොහැකි විය."
            )
        }
    }

    fun toggleFlashlight(): ActionResult = setFlashlight(!_isFlashlightOn.value)

    fun openApp(rawAppName: String): ActionResult {
        val cleaned = rawAppName.trim().lowercase(Locale.ROOT)
        if (cleaned.isBlank()) {
            return ActionResult(
                success = false,
                badgeTitle = "Missing App Name",
                messageEn = "Please specify which app you want to open.",
                messageSi = "කරුණාකර විවෘත කළ යුතු ඇප් එක සඳහන් කරන්න."
            )
        }

        val normalized = SINHALA_APP_ALIASES[cleaned] ?: cleaned

        if (normalized == "settings") return openSettings()
        if (normalized == "camera") return openCamera()

        val pm = context.packageManager

        // 1. Check known package names first
        val knownPackages = KNOWN_APP_PACKAGES.entries
            .firstOrNull { normalized.contains(it.key) }
            ?.value
            .orEmpty()

        for (pkg in knownPackages) {
            val launchIntent = pm.getLaunchIntentForPackage(pkg)
            if (launchIntent != null && safeStartActivity(launchIntent)) {
                val displayName = normalized.replaceFirstChar { it.uppercase() }
                return ActionResult(
                    success = true,
                    badgeTitle = "Opened $displayName",
                    messageEn = "Opening $displayName.",
                    messageSi = "$displayName විවෘත කරමින් පවතී."
                )
            }
        }

        // 2. Query installed launcher activities across all OEMs (Samsung, Xiaomi, Oppo, Vivo, etc.)
        val mainIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val installedApps = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.queryIntentActivities(mainIntent, PackageManager.ResolveInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(mainIntent, 0)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed querying installed launcher apps", e)
            emptyList()
        }

        val matchedResolveInfo = installedApps.firstOrNull { info ->
            val label = info.loadLabel(pm).toString().lowercase(Locale.ROOT)
            val pkg = info.activityInfo.packageName.lowercase(Locale.ROOT)
            label == normalized || label.contains(normalized) || pkg.contains(normalized)
        }

        if (matchedResolveInfo != null) {
            val pkg = matchedResolveInfo.activityInfo.packageName
            val appLabel = matchedResolveInfo.loadLabel(pm).toString()
            val launchIntent = pm.getLaunchIntentForPackage(pkg) ?: Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                setClassName(pkg, matchedResolveInfo.activityInfo.name)
            }
            if (safeStartActivity(launchIntent)) {
                return ActionResult(
                    success = true,
                    badgeTitle = "Opened $appLabel",
                    messageEn = "Opening $appLabel.",
                    messageSi = "$appLabel විවෘත කරමින් පවතී."
                )
            }
        }

        return ActionResult(
            success = false,
            badgeTitle = "App Not Installed",
            messageEn = "$rawAppName does not appear to be installed on this device.",
            messageSi = "$rawAppName ඇප් එක මෙම දුරකථනයේ ස්ථාපනය කර නොමැත."
        )
    }

    fun openSettings(section: String? = null): ActionResult {
        val action = when (section?.lowercase(Locale.ROOT)) {
            "wifi", "wi-fi" -> Settings.ACTION_WIFI_SETTINGS
            "bluetooth" -> Settings.ACTION_BLUETOOTH_SETTINGS
            "sound", "volume" -> Settings.ACTION_SOUND_SETTINGS
            "display", "brightness" -> Settings.ACTION_DISPLAY_SETTINGS
            "battery" -> Intent.ACTION_POWER_USAGE_SUMMARY
            else -> Settings.ACTION_SETTINGS
        }
        val intent = Intent(action)
        val launched = safeStartActivity(intent) || safeStartActivity(Intent(Settings.ACTION_SETTINGS))
        return if (launched) {
            ActionResult(
                success = true,
                badgeTitle = "Opened Settings",
                messageEn = "Opening device Settings.",
                messageSi = "දුරකථනයේ Settings විවෘත කරමින් පවතී."
            )
        } else {
            ActionResult(
                success = false,
                badgeTitle = "Settings Unavailable",
                messageEn = "Unable to open Settings on this device.",
                messageSi = "Settings විවෘත කිරීමට නොහැකි විය."
            )
        }
    }

    fun openCamera(): ActionResult {
        val cameraIntent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
        if (safeStartActivity(cameraIntent)) {
            return ActionResult(
                success = true,
                badgeTitle = "Opened Camera",
                messageEn = "Opening Camera.",
                messageSi = "කැමරාව විවෘත කරමින් පවතී."
            )
        }

        // Fallback across OEM camera packages
        val oemCameras = listOf(
            "com.sec.android.app.camera",
            "com.android.camera",
            "com.android.camera2",
            "com.google.android.GoogleCamera",
            "com.oplus.camera",
            "com.oppo.camera",
            "com.vivo.camera"
        )
        for (pkg in oemCameras) {
            val intent = context.packageManager.getLaunchIntentForPackage(pkg)
            if (intent != null && safeStartActivity(intent)) {
                return ActionResult(
                    success = true,
                    badgeTitle = "Opened Camera",
                    messageEn = "Opening Camera.",
                    messageSi = "කැමරාව විවෘත කරමින් පවතී."
                )
            }
        }

        return ActionResult(
            success = false,
            badgeTitle = "Camera Unavailable",
            messageEn = "No camera app was found on this device.",
            messageSi = "මෙම දුරකථනයේ කැමරා ඇප් එකක් සොයාගත නොහැකි විය."
        )
    }

    fun getBatteryStatus(): ActionResult {
        val batteryStatusIntent = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val propLevel = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1

        val level = batteryStatusIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatusIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val computedPercent = if (level >= 0 && scale > 0) (level * 100) / scale else propLevel

        if (computedPercent < 0) {
            return ActionResult(
                success = false,
                badgeTitle = "Battery Unavailable",
                messageEn = "Unable to read battery level from this device.",
                messageSi = "බැටරි ප්‍රතිශතය කියවීමට නොහැකි විය."
            )
        }

        val status = batteryStatusIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

        val chargingEn = if (isCharging) " and currently charging" else ""
        val chargingSi = if (isCharging) " (චාර්ජ් වෙමින් පවතී)" else ""

        return ActionResult(
            success = true,
            badgeTitle = "Battery $computedPercent%",
            messageEn = "Your battery is at $computedPercent%$chargingEn.",
            messageSi = "ඔබගේ දුරකථනයේ බැටරිය $computedPercent% ක් ඇත$chargingSi."
        )
    }

    fun getCurrentTime(): ActionResult {
        val now = LocalDateTime.now()
        val timeEn = now.format(DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH))
        val hour24 = now.hour
        val periodSi = when (hour24) {
            in 0..4 -> "රාත්‍රී"
            in 5..11 -> "උදෑසන"
            in 12..15 -> "දහවල්"
            in 16..18 -> "සවස"
            else -> "රාත්‍රී"
        }
        val hour12 = if (now.hour % 12 == 0) 12 else now.hour % 12
        val minuteFormatted = now.minute.toString().padStart(2, '0')
        return ActionResult(
            success = true,
            badgeTitle = timeEn,
            messageEn = "The current time is $timeEn.",
            messageSi = "දැන් වෙලාව $periodSi $hour12.$minuteFormatted යි."
        )
    }

    fun getCurrentDate(): ActionResult {
        val now = LocalDateTime.now()
        val dateEn = now.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.ENGLISH))
        val monthSi = when (now.monthValue) {
            1 -> "ජනවාරි"
            2 -> "පෙබරවාරි"
            3 -> "මාර්තු"
            4 -> "අප්‍රේල්"
            5 -> "මැයි"
            6 -> "ජූනි"
            7 -> "ජූලි"
            8 -> "අගෝස්තු"
            9 -> "සැප්තැම්බර්"
            10 -> "ඔක්තෝබර්"
            11 -> "නොවැම්බර්"
            else -> "දෙසැම්බර්"
        }
        val daySi = when (now.dayOfWeek.value) {
            1 -> "සඳුදා"
            2 -> "අඟහරුවාදා"
            3 -> "බදාදා"
            4 -> "බ්‍රහස්පතින්දා"
            5 -> "සිකුරාදා"
            6 -> "සෙනසුරාදා"
            else -> "ඉරිදා"
        }
        return ActionResult(
            success = true,
            badgeTitle = now.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH)),
            messageEn = "Today is $dateEn.",
            messageSi = "අද ${now.year} $monthSi ${now.dayOfMonth} වෙනි $daySi යි."
        )
    }

    fun adjustMediaVolume(action: VolumeAction): ActionResult {
        val am = audioManager ?: return ActionResult(
            success = false,
            badgeTitle = "Audio Unavailable",
            messageEn = "Could not access audio volume controls.",
            messageSi = "ශබ්ද පාලනයට ප්‍රවේශ විය නොහැක."
        )

        if (am.isVolumeFixed) {
            return ActionResult(
                success = false,
                badgeTitle = "Volume Fixed",
                messageEn = "This device uses fixed audio volume that cannot be changed by apps.",
                messageSi = "මෙම උපාංගයේ ශබ්දය වෙනස් කළ නොහැක."
            )
        }

        return try {
            val direction = when (action) {
                VolumeAction.UP -> AudioManager.ADJUST_RAISE
                VolumeAction.DOWN -> AudioManager.ADJUST_LOWER
                VolumeAction.MUTE -> AudioManager.ADJUST_MUTE
                VolumeAction.UNMUTE -> AudioManager.ADJUST_UNMUTE
            }
            am.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
            val current = am.getStreamVolume(AudioManager.STREAM_MUSIC)
            val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
            val percent = (current * 100) / max

            ActionResult(
                success = true,
                badgeTitle = "Volume $percent%",
                messageEn = "Media volume is now at $percent%.",
                messageSi = "මාධ්‍ය ශබ්දය දැන් $percent% කි."
            )
        } catch (e: SecurityException) {
            Log.w(TAG, "Volume adjustment blocked by policy", e)
            ActionResult(
                success = false,
                badgeTitle = "Permission Restricted",
                messageEn = "Do Not Disturb or system policy prevented changing volume.",
                messageSi = "පද්ධති සීමාවක් නිසා ශබ්දය වෙනස් කිරීමට නොහැකි විය."
            )
        }
    }

    fun openBrowserSearch(query: String): ActionResult {
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty()) {
            val homeIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))
            return if (safeStartActivity(homeIntent)) {
                ActionResult(
                    success = true,
                    badgeTitle = "Opened Google",
                    messageEn = "Opening Google Search in your browser.",
                    messageSi = "Google වෙබ් බ්‍රව්සරය විවෘත කරමින් පවතී."
                )
            } else {
                ActionResult(
                    success = false,
                    badgeTitle = "Browser Unavailable",
                    messageEn = "No web browser app was found on this device.",
                    messageSi = "වෙබ් බ්‍රව්සරයක් සොයාගත නොහැකි විය."
                )
            }
        }

        val webSearchIntent = Intent(Intent.ACTION_WEB_SEARCH).apply {
            putExtra(SearchManager.QUERY, cleanQuery)
        }
        val browserUrlIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://www.google.com/search?q=${Uri.encode(cleanQuery)}")
        )

        return if (safeStartActivity(browserUrlIntent) || safeStartActivity(webSearchIntent)) {
            ActionResult(
                success = true,
                badgeTitle = "Google Search",
                messageEn = "Searching Google for \"$cleanQuery\".",
                messageSi = "\"$cleanQuery\" සඳහා Google හි සොයමින් පවතී."
            )
        } else {
            ActionResult(
                success = false,
                badgeTitle = "Browser Unavailable",
                messageEn = "Could not open a browser to search for \"$cleanQuery\".",
                messageSi = "සෙවුම විවෘත කිරීමට බ්‍රව්සරයක් හමු නොවීය."
            )
        }
    }

    fun openDialer(phoneNumber: String?): ActionResult {
        val cleanNumber = phoneNumber?.filter { it.isDigit() || it == '+' || it == '*' || it == '#' }.orEmpty()
        val uri = if (cleanNumber.isNotEmpty()) Uri.parse("tel:$cleanNumber") else Uri.parse("tel:")
        val intent = Intent(Intent.ACTION_DIAL, uri)
        return if (safeStartActivity(intent)) {
            val label = if (cleanNumber.isNotEmpty()) cleanNumber else "Phone Dialer"
            ActionResult(
                success = true,
                badgeTitle = "Opened Dialer",
                messageEn = if (cleanNumber.isNotEmpty()) "Opening dialer for $cleanNumber." else "Opening phone dialer.",
                messageSi = if (cleanNumber.isNotEmpty()) "$cleanNumber සඳහා ඇමතුම් පුවරුව විවෘත කළා." else "ඇමතුම් පුවරුව විවෘත කළා."
            )
        } else {
            ActionResult(
                success = false,
                badgeTitle = "Dialer Unavailable",
                messageEn = "No phone dialer app is available on this device.",
                messageSi = "මෙම උපාංගයේ දුරකථන ඇමතුම් ඇප් එකක් නොමැත."
            )
        }
    }

    fun prepareSms(recipient: String?, messageBody: String?): ActionResult {
        val cleanRecipient = recipient?.filter { it.isDigit() || it == '+' }.orEmpty()
        val body = messageBody.orEmpty().trim()
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$cleanRecipient")).apply {
            if (body.isNotEmpty()) {
                putExtra("sms_body", body)
            }
        }
        return if (safeStartActivity(intent)) {
            ActionResult(
                success = true,
                badgeTitle = "Prepared SMS",
                messageEn = "Opening Messages with your prepared text.",
                messageSi = "ඔබගේ පණිවිඩය සමඟ SMS ඇප් එක විවෘත කළා."
            )
        } else {
            ActionResult(
                success = false,
                badgeTitle = "SMS Unavailable",
                messageEn = "No SMS messaging app was found on this device.",
                messageSi = "SMS පණිවිඩ ඇප් එකක් සොයාගත නොහැකි විය."
            )
        }
    }

    fun goHome(): ActionResult {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        return if (safeStartActivity(homeIntent)) {
            ActionResult(
                success = true,
                badgeTitle = "Home Screen",
                messageEn = "Returning to your home screen.",
                messageSi = "මුල් තිරයට (Home) යමින් පවතී."
            )
        } else {
            ActionResult(
                success = false,
                badgeTitle = "Home Unavailable",
                messageEn = "Could not navigate to the home screen.",
                messageSi = "මුල් තිරයට යාමට නොහැකි විය."
            )
        }
    }

    fun controlMedia(action: MediaControlAction): ActionResult {
        val am = audioManager ?: return ActionResult(
            success = false,
            badgeTitle = "Media Unavailable",
            messageEn = "Unable to access system media playback controls.",
            messageSi = "මාධ්‍ය පාලනයට ප්‍රවේශ විය නොහැක."
        )
        val keyCode = when (action) {
            MediaControlAction.PLAY_PAUSE -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
            MediaControlAction.NEXT -> KeyEvent.KEYCODE_MEDIA_NEXT
            MediaControlAction.PREVIOUS -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
        }
        return try {
            am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
            am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
            val labelEn = when (action) {
                MediaControlAction.PLAY_PAUSE -> "Toggled media play/pause."
                MediaControlAction.NEXT -> "Skipped to next track."
                MediaControlAction.PREVIOUS -> "Returned to previous track."
            }
            val labelSi = when (action) {
                MediaControlAction.PLAY_PAUSE -> "මාධ්‍ය වාදනය Play/Pause කරන ලදී."
                MediaControlAction.NEXT -> "ඊළඟ ගීතයට මාරු කරන ලදී."
                MediaControlAction.PREVIOUS -> "පෙර ගීතයට මාරු කරන ලදී."
            }
            ActionResult(
                success = true,
                badgeTitle = "Media Control",
                messageEn = labelEn,
                messageSi = labelSi
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed dispatching media key", e)
            ActionResult(
                success = false,
                badgeTitle = "Media Error",
                messageEn = "Could not send media playback command.",
                messageSi = "මාධ්‍ය පාලන විධානය ක්‍රියාත්මක කිරීමට නොහැකි විය."
            )
        }
    }

    fun setTimer(rawDuration: String): ActionResult {
        val digits = rawDuration.filter { it.isDigit() }.toIntOrNull() ?: 60
        val lower = rawDuration.lowercase(Locale.ROOT)
        val seconds = when {
            lower.contains("min") || lower.contains("මිනිත්තු") || lower.contains("විනාඩි") -> digits * 60
            lower.contains("hour") || lower.contains("පැය") -> digits * 3600
            else -> digits
        }.coerceIn(1, 86_400)

        val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, seconds)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
        }
        return if (safeStartActivity(intent)) {
            ActionResult(
                success = true,
                badgeTitle = "Timer ${seconds}s",
                messageEn = "Opening timer for $seconds seconds.",
                messageSi = "තත්පර $seconds ක Timer එකක් සකසමින් පවතී."
            )
        } else {
            ActionResult(
                success = false,
                badgeTitle = "Timer Unavailable",
                messageEn = "No clock/timer app was found to set a timer.",
                messageSi = "Timer සැකසීමට Clock ඇප් එකක් හමු නොවීය."
            )
        }
    }

    fun getNetworkStatus(): ActionResult {
        val telemetry = readTelemetry()
        return if (telemetry.isOnline) {
            ActionResult(
                success = true,
                badgeTitle = "${telemetry.networkType} Connected",
                messageEn = "You are connected to the internet via ${telemetry.networkType}.",
                messageSi = "ඔබ ${telemetry.networkType} හරහා අන්තර්ජාලයට සම්බන්ධ වී ඇත."
            )
        } else {
            ActionResult(
                success = false,
                badgeTitle = "Offline",
                messageEn = "No active internet connection was detected.",
                messageSi = "සක්‍රිය අන්තර්ජාල සම්බන්ධතාවයක් නොමැත."
            )
        }
    }

    fun readTelemetry(): DeviceTelemetry {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val percent = if (level >= 0 && scale > 0) (level * 100) / scale else -1
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

        val cm = connectivityManager
        val activeNetwork = cm?.activeNetwork
        val caps = activeNetwork?.let { cm.getNetworkCapabilities(it) }
        val isOnline = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        val networkType = when {
            caps == null -> "Offline"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile Data"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
            else -> if (isOnline) "Connected" else "Offline"
        }

        return DeviceTelemetry(
            batteryPercent = percent,
            isCharging = isCharging,
            isOnline = isOnline,
            networkType = networkType,
            hasFlashlight = hasFlashlightHardware(),
            isFlashlightOn = _isFlashlightOn.value
        )
    }

    private fun safeStartActivity(intent: Intent): Boolean {
        return try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed launching intent ${intent.action}", e)
            false
        }
    }
}
