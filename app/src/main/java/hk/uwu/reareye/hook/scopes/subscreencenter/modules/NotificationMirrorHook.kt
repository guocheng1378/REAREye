package hk.uwu.reareye.hook.scopes.subscreencenter.modules

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.service.notification.StatusBarNotification
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.YLog
import hk.uwu.reareye.ui.config.ConfigKeys
import hk.uwu.reareye.widgetapi.IRearWidgetApiService
import hk.uwu.reareye.widgetapi.RearWidgetApiClient
import hk.uwu.reareye.widgetapi.RearWidgetNoticeOptions
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Notification Mirror Hook
 *
 * Intercepts system notifications and mirrors qualifying ones to the rear screen
 * via the existing RearWidgetApi postNotice mechanism.
 *
 * Configuration (via prefs):
 * - [ConfigKeys.HOOK_NOTIFICATION_MIRROR_ENABLED]: Master toggle
 * - [ConfigKeys.NOTIFICATION_MIRROR_APPS]: Allowlist of package names (empty = all)
 * - [ConfigKeys.NOTIFICATION_MIRROR_BLOCK_APPS]: Blocklist of package names
 */
class NotificationMirrorHook : YukiBaseHooker() {

    companion object {
        private const val TAG = "REAREye-NotifMirror"
        private const val BUSINESS_NOTIFICATION_MIRROR = "notification_mirror"
        private const val MIRROR_INDEX_BASE = 100 // High index to avoid conflict with regular cards
        private const val MAX_NOTIFICATIONS = 5
    }

    private val hookInitialized = AtomicBoolean(false)
    private val mirroredKeys = ConcurrentHashMap<String, Long>() // notificationKey -> postTime
    private var remoteClient: RearWidgetApiClient? = null
    private var hostContext: Context? = null
    private var mainHandler: Handler? = null

    override fun onHook() {
        loadApp("com.android.systemui") {
            val notifManagerRef = "com.android.systemui.statusbar.notification.collection.NotifPipeline".toClass().resolve()

            notifManagerRef.firstMethod {
                name = "addCollectionListener"
                parameterCount = 1
            }.hook().before {
                if (!hookInitialized.compareAndSet(false, true)) return@before
                debugLog("NotifPipeline.addCollectionListener intercepted")
            }

            // Hook NotificationListenerService to capture posted notifications
            val listenerClz =
                "com.android.systemui.statusbar.notification.NotificationEntryManager".toClass().resolve()

            listenerClz.firstMethod {
                name = "addNotification"
                parameterCount = 2
            }.hook().after {
                if (!isEnabled()) return@after
                val sbn = args.getOrNull(1) as? StatusBarNotification ?: return@after
                handleNotificationPosted(sbn)
            }

            listenerClz.firstMethod {
                name = "removeNotification"
                parameterCount = 2
            }.hook().after {
                if (!isEnabled()) return@after
                val key = args.getOrNull(1) as? String ?: return@after
                handleNotificationRemoved(key)
            }
        }

        loadApp("com.xiaomi.subscreencenter") {
            onAppLifecycle {
                onCreate {
                    hostContext = appContext
                    mainHandler = Handler(appContext!!.mainLooper)
                }
            }
        }
    }

    private fun isEnabled(): Boolean {
        return prefs.getBoolean(ConfigKeys.HOOK_NOTIFICATION_MIRROR_ENABLED, false)
    }

    private fun isAppAllowed(packageName: String): Boolean {
        val allowlist = prefs.getStringSet(ConfigKeys.NOTIFICATION_MIRROR_APPS, emptySet()) ?: emptySet()
        val blocklist = prefs.getStringSet(ConfigKeys.NOTIFICATION_MIRROR_BLOCK_APPS, emptySet()) ?: emptySet()

        // Blocklist takes priority
        if (packageName in blocklist) return false

        // Empty allowlist = allow all (except blocked)
        if (allowlist.isEmpty()) return true

        return packageName in allowlist
    }

    private fun handleNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        val key = sbn.key

        if (!isAppAllowed(packageName)) return

        // Skip ongoing/foreground service notifications
        val notification = sbn.notification ?: return
        val flags = notification.flags
        if (flags and Notification.FLAG_ONGOING_EVENT != 0) return
        if (flags and Notification.FLAG_FOREGROUND_SERVICE != 0) return

        // Skip group summary notifications
        if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

        // Rate limit: don't mirror the same key within 2 seconds
        val now = System.currentTimeMillis()
        val lastPost = mirroredKeys[key] ?: 0L
        if (now - lastPost < 2000) return

        // Enforce max notifications
        if (mirroredKeys.size >= MAX_NOTIFICATIONS) {
            val oldest = mirroredKeys.entries.minByOrNull { it.value }?.key
            oldest?.let { removeMirroredNotification(it) }
        }

        val title = notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = notification.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val bigText = notification.extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()
        val subText = notification.extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString().orEmpty()

        val displayText = bigText.ifBlank { text }
        if (title.isBlank() && displayText.isBlank()) return

        mirroredKeys[key] = now

        postMirrorNotification(
            packageName = packageName,
            key = key,
            title = title,
            text = displayText,
            subText = subText,
            index = MIRROR_INDEX_BASE + (mirroredKeys.size % MAX_NOTIFICATIONS),
        )

        debugLog("mirrored pkg=$packageName title=$title key=$key")
    }

    private fun handleNotificationRemoved(key: String) {
        if (!mirroredKeys.containsKey(key)) return
        removeMirroredNotification(key)
        debugLog("removed mirror for key=$key")
    }

    private fun postMirrorNotification(
        packageName: String,
        key: String,
        title: String,
        text: String,
        subText: String,
        index: Int,
    ) {
        val ctx = hostContext ?: return
        val handler = mainHandler ?: return

        handler.post {
            runCatching {
                val client = remoteClient ?: RearWidgetApiClient().also { remoteClient = it }
                if (!client.isConnected() && !client.bind(ctx)) {
                    debugLog("client bind failed for mirror")
                    return@post
                }

                val payload = Bundle().apply {
                    putString("title", title)
                    putString("business", BUSINESS_NOTIFICATION_MIRROR)
                    putString("__mirror_notif_key__", key)
                    putString("__mirror_pkg__", packageName)
                    putString("__mirror_text__", text)
                    if (subText.isNotBlank()) putString("__mirror_sub__", subText)
                }

                val options = RearWidgetNoticeOptions(
                    sticky = false,
                    disablePopup = true,
                    forcePopup = false,
                    enableFloat = false,
                    showTimeTip = true,
                    index = index,
                    priority = 800, // Lower priority than user-configured cards
                )

                client.postNotice(
                    targetPackage = "com.xiaomi.subscreencenter",
                    business = BUSINESS_NOTIFICATION_MIRROR,
                    payload = payload,
                    options = options,
                )

                debugLog("posted mirror notice title=$title pkg=$packageName")
            }.onFailure {
                debugLog("post mirror failed err=${it.message}")
                YLog.error("[$TAG] post mirror failed", it)
            }
        }
    }

    private fun removeMirroredNotification(key: String) {
        val ctx = hostContext ?: return
        val handler = mainHandler ?: return
        mirroredKeys.remove(key)

        handler.post {
            runCatching {
                val client = remoteClient ?: return@post
                if (!client.isConnected()) return@post

                // Build a ticket-like composite key for removal
                val compositeKey = "${ctx.packageName}:${BUSINESS_NOTIFICATION_MIRROR}:$key"
                // Note: We'd need the original ticket to remove properly.
                // For now, the notification will naturally expire when updated/removed.
                debugLog("mirror cleanup for key=$key")
            }
        }
    }

    private fun debugLog(message: String) {
        if (prefs.getBoolean(ConfigKeys.MORE_DEBUG, false)) {
            YLog.debug("[$TAG] $message")
        }
    }
}
