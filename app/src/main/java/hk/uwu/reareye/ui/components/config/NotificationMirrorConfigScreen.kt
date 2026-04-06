package hk.uwu.reareye.ui.components.config

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hk.uwu.reareye.ui.components.card.SuperCard
import hk.uwu.reareye.ui.config.ConfigKeys
import hk.uwu.reareye.ui.config.PrefsManager
import hk.uwu.reareye.ui.config.PrefsManager.Companion.getPrefsManager
import hk.uwu.reareye.ui.theme.rearAcrylicEffect
import hk.uwu.reareye.ui.theme.rearAcrylicSource
import hk.uwu.reareye.ui.theme.rememberAcrylicHazeState
import hk.uwu.reareye.ui.theme.rememberAcrylicHazeStyle
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@SuppressLint("LocalContextGetResourceValueCall")
@Composable
fun NotificationMirrorConfigScreen(
    prefsManager: PrefsManager,
    onBack: () -> Unit,
    onOpenAppList: () -> Unit,
) {
    val context = LocalContext.current
    val layoutDirection = LocalLayoutDirection.current
    val scrollBehavior = MiuixScrollBehavior()
    val hazeState = rememberAcrylicHazeState()
    val hazeStyle = rememberAcrylicHazeStyle()

    var mirrorEnabled by remember {
        mutableStateOf(
            prefsManager.getBoolean(ConfigKeys.HOOK_NOTIFICATION_MIRROR_ENABLED, false)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.rearAcrylicEffect(hazeState, hazeStyle),
                color = Color.Transparent,
                title = "Notification Mirror",
                navigationIconPadding = 12.dp,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            modifier = Modifier.graphicsLayer {
                                if (layoutDirection == LayoutDirection.Rtl) scaleX = -1f
                            },
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .scrollEndHaptic()
                .overScrollVertical()
                .rearAcrylicSource(hazeState)
                .padding(horizontal = 12.dp),
            contentPadding = PaddingValues(
                top = paddingValues.calculateTopPadding() + 12.dp,
                bottom = paddingValues.calculateBottomPadding() + 12.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            overscrollEffect = null,
        ) {
            item {
                Card(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth(),
                    insideMargin = PaddingValues(16.dp),
                ) {
                    Text(
                        text = "Mirror notifications from your main screen to the rear screen in real-time.",
                        fontSize = 14.sp,
                        color = Color(0xFF888888),
                    )
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    SwitchPreference(
                        title = "Enable Notification Mirror",
                        summary = "Forward qualifying notifications to the rear screen",
                        checked = mirrorEnabled,
                        onCheckedChange = { checked ->
                            mirrorEnabled = checked
                            prefsManager.putBoolean(ConfigKeys.HOOK_NOTIFICATION_MIRROR_ENABLED, checked)
                        },
                    )
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    SwitchPreference(
                        title = "Show notification text",
                        summary = "Display the full notification body text on rear screen",
                        checked = prefsManager.getBoolean(ConfigKeys.NOTIFICATION_MIRROR_SHOW_TEXT, true),
                        onCheckedChange = { checked ->
                            prefsManager.putBoolean(ConfigKeys.NOTIFICATION_MIRROR_SHOW_TEXT, checked)
                        },
                    )
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    SwitchPreference(
                        title = "Show timestamp",
                        summary = "Display when the notification was received",
                        checked = prefsManager.getBoolean(ConfigKeys.NOTIFICATION_MIRROR_SHOW_TIME, true),
                        onCheckedChange = { checked ->
                            prefsManager.putBoolean(ConfigKeys.NOTIFICATION_MIRROR_SHOW_TIME, checked)
                        },
                    )
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    SwitchPreference(
                        title = "Filter: Allowlist mode",
                        summary = "Only mirror notifications from selected apps. If off, mirrors all except blocked.",
                        checked = prefsManager.getBoolean(ConfigKeys.NOTIFICATION_MIRROR_ALLOWLIST_MODE, false),
                        onCheckedChange = { checked ->
                            prefsManager.putBoolean(ConfigKeys.NOTIFICATION_MIRROR_ALLOWLIST_MODE, checked)
                        },
                    )
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    insideMargin = PaddingValues(16.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "App Filters",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Select which apps to mirror or block",
                            fontSize = 13.sp,
                            color = Color(0xFF888888),
                        )
                        SuperCard(
                            title = "Allowed Apps",
                            summary = "Apps whose notifications will be mirrored",
                            onClick = { onOpenAppList() },
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    insideMargin = PaddingValues(16.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "⚠️ Notes",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "• Ongoing/foreground service notifications are automatically excluded\n" +
                                    "• Group summary notifications are skipped\n" +
                                    "• Rate limited: same notification won't mirror twice in 2 seconds\n" +
                                    "• Max 5 simultaneous mirrored notifications",
                            fontSize = 12.sp,
                            color = Color(0xFF888888),
                            lineHeight = 18.sp,
                        )
                    }
                }
            }
        }
    }
}
