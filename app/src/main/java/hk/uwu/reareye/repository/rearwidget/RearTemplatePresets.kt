package hk.uwu.reareye.repository.rearwidget

/**
 * Built-in preset templates that ship with REAREye.
 * Each template is a JSON/MAML file content that can be deployed to the rear screen widget system.
 */
object RearTemplatePresets {

    data class PresetTemplate(
        val id: String,
        val name: String,
        val description: String,
        val business: String,
        val category: String,
        val iconEmoji: String,
        val templateContent: String,
    )

    val all: List<PresetTemplate> = listOf(
        clockDigital(),
        clockAnalog(),
        batteryRing(),
        customText(),
        stepCounter(),
        weatherBrief(),
        musicNowPlaying(),
        systemInfo(),
    )

    fun findById(id: String): PresetTemplate? = all.find { it.id == id }

    fun categories(): List<String> = all.map { it.category }.distinct().sorted()

    fun byCategory(category: String): List<PresetTemplate> = all.filter { it.category == category }

    private fun clockDigital() = PresetTemplate(
        id = "builtin_clock_digital",
        name = "Digital Clock",
        description = "Large digital clock display with date",
        business = "clock_digital",
        category = "Clock",
        iconEmoji = "🕐",
        templateContent = """<?xml version="1.0" encoding="utf-8"?>
<RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#000000">

    <TextClock
        android:id="@+id/time"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_centerInParent="true"
        android:format12Hour="hh:mm:ss"
        android:format24Hour="HH:mm:ss"
        android:textSize="72sp"
        android:textColor="#FFFFFF"
        android:fontFamily="sans-serif-thin" />

    <TextClock
        android:id="@+id/date"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_below="@id/time"
        android:layout_centerHorizontal="true"
        android:format12Hour="EEEE, MMM d"
        android:format24Hour="EEEE, MMM d"
        android:textSize="18sp"
        android:textColor="#88FFFFFF"
        android:layout_marginTop="8dp" />
</RelativeLayout>"""
    )

    private fun clockAnalog() = PresetTemplate(
        id = "builtin_clock_analog",
        name = "Analog Clock",
        description = "Classic analog clock face",
        business = "clock_analog",
        category = "Clock",
        iconEmoji = "⏰",
        templateContent = """<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#000000">

    <AnalogClock
        android:layout_width="240dp"
        android:layout_height="240dp"
        android:layout_gravity="center"
        android:dial="@drawable/clock_dial"
        android:hand_hour="@drawable/clock_hour_hand"
        android:hand_minute="@drawable/clock_minute_hand" />
</FrameLayout>"""
    )

    private fun batteryRing() = PresetTemplate(
        id = "builtin_battery_ring",
        name = "Battery Ring",
        description = "Circular battery level indicator with percentage",
        business = "battery_ring",
        category = "Status",
        iconEmoji = "🔋",
        templateContent = """<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#000000">

    <ProgressBar
        android:id="@+id/battery_ring"
        style="@android:style/Widget.ProgressBar.Horizontal"
        android:layout_width="200dp"
        android:layout_height="200dp"
        android:layout_gravity="center"
        android:max="100"
        android:progress="85"
        android:progressDrawable="@drawable/battery_ring_drawable" />

    <TextView
        android:id="@+id/battery_pct"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="center"
        android:text="85%"
        android:textSize="48sp"
        android:textColor="#FFFFFF"
        android:fontFamily="sans-serif-light" />

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="center_horizontal"
        android:layout_marginTop="140dp"
        android:text="Battery"
        android:textSize="14sp"
        android:textColor="#66FFFFFF" />
</FrameLayout>"""
    )

    private fun customText() = PresetTemplate(
        id = "builtin_custom_text",
        name = "Custom Text",
        description = "Editable scrolling text display",
        business = "custom_text",
        category = "Utility",
        iconEmoji = "✏️",
        templateContent = """<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#000000"
    android:padding="32dp">

    <TextView
        android:id="@+id/custom_text"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_gravity="center"
        android:text="Hello from REAREye!"
        android:textSize="36sp"
        android:textColor="#FFFFFF"
        android:fontFamily="sans-serif-medium"
        android:gravity="center"
        android:lineSpacingMultiplier="1.3" />
</FrameLayout>"""
    )

    private fun stepCounter() = PresetTemplate(
        id = "builtin_step_counter",
        name = "Step Counter",
        description = "Daily step count with goal progress",
        business = "step_counter",
        category = "Health",
        iconEmoji = "👟",
        templateContent = """<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#000000"
    android:gravity="center"
    android:orientation="vertical">

    <TextView
        android:id="@+id/step_count"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="0"
        android:textSize="64sp"
        android:textColor="#FFFFFF"
        android:fontFamily="sans-serif-thin" />

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="steps today"
        android:textSize="16sp"
        android:textColor="#66FFFFFF"
        android:layout_marginTop="4dp" />

    <ProgressBar
        style="@android:style/Widget.ProgressBar.Horizontal"
        android:layout_width="200dp"
        android:layout_height="6dp"
        android:layout_marginTop="24dp"
        android:max="10000"
        android:progress="0"
        android:progressTint="#4CAF50"
        android:progressBackgroundTint="#33FFFFFF" />

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Goal: 10,000"
        android:textSize="12sp"
        android:textColor="#44FFFFFF"
        android:layout_marginTop="8dp" />
</LinearLayout>"""
    )

    private fun weatherBrief() = PresetTemplate(
        id = "builtin_weather_brief",
        name = "Weather Brief",
        description = "Temperature and weather condition",
        business = "weather_brief",
        category = "Weather",
        iconEmoji = "🌤️",
        templateContent = """<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#000000"
    android:gravity="center"
    android:orientation="vertical">

    <TextView
        android:id="@+id/weather_icon"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="☀️"
        android:textSize="64sp" />

    <TextView
        android:id="@+id/temperature"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="22°C"
        android:textSize="48sp"
        android:textColor="#FFFFFF"
        android:fontFamily="sans-serif-light"
        android:layout_marginTop="8dp" />

    <TextView
        android:id="@+id/condition"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Sunny"
        android:textSize="16sp"
        android:textColor="#88FFFFFF"
        android:layout_marginTop="4dp" />

    <TextView
        android:id="@+id/location"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Beijing"
        android:textSize="12sp"
        android:textColor="#44FFFFFF"
        android:layout_marginTop="12dp" />
</LinearLayout>"""
    )

    private fun musicNowPlaying() = PresetTemplate(
        id = "builtin_music_now_playing",
        name = "Now Playing",
        description = "Current track info with album art placeholder",
        business = "music_now_playing",
        category = "Music",
        iconEmoji = "🎵",
        templateContent = """<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#000000"
    android:gravity="center"
    android:orientation="vertical"
    android:padding="24dp">

    <ImageView
        android:id="@+id/album_art"
        android:layout_width="160dp"
        android:layout_height="160dp"
        android:scaleType="centerCrop"
        android:background="#1AFFFFFF" />

    <TextView
        android:id="@+id/track_title"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Not Playing"
        android:textSize="22sp"
        android:textColor="#FFFFFF"
        android:fontFamily="sans-serif-medium"
        android:gravity="center"
        android:layout_marginTop="20dp"
        android:singleLine="true"
        android:ellipsize="marquee" />

    <TextView
        android:id="@+id/artist_name"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text=""
        android:textSize="14sp"
        android:textColor="#88FFFFFF"
        android:gravity="center"
        android:layout_marginTop="4dp"
        android:singleLine="true" />
</LinearLayout>"""
    )

    private fun systemInfo() = PresetTemplate(
        id = "builtin_system_info",
        name = "System Info",
        description = "RAM, CPU, and storage overview",
        business = "system_info",
        category = "Status",
        iconEmoji = "📊",
        templateContent = """<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#000000"
    android:gravity="center"
    android:orientation="vertical"
    android:padding="24dp">

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="System Status"
        android:textSize="18sp"
        android:textColor="#88FFFFFF"
        android:layout_marginBottom="24dp" />

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:gravity="center_vertical"
        android:layout_marginBottom="16dp">

        <TextView
            android:layout_width="80dp"
            android:layout_height="wrap_content"
            android:text="RAM"
            android:textSize="14sp"
            android:textColor="#66FFFFFF" />

        <ProgressBar
            style="@android:style/Widget.ProgressBar.Horizontal"
            android:layout_width="0dp"
            android:layout_height="6dp"
            android:layout_weight="1"
            android:max="100"
            android:progress="60"
            android:progressTint="#2196F3"
            android:progressBackgroundTint="#33FFFFFF" />

        <TextView
            android:id="@+id/ram_pct"
            android:layout_width="48dp"
            android:layout_height="wrap_content"
            android:text="60%"
            android:textSize="12sp"
            android:textColor="#FFFFFF"
            android:gravity="end"
            android:layout_marginStart="8dp" />
    </LinearLayout>

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:gravity="center_vertical"
        android:layout_marginBottom="16dp">

        <TextView
            android:layout_width="80dp"
            android:layout_height="wrap_content"
            android:text="Storage"
            android:textSize="14sp"
            android:textColor="#66FFFFFF" />

        <ProgressBar
            style="@android:style/Widget.ProgressBar.Horizontal"
            android:layout_width="0dp"
            android:layout_height="6dp"
            android:layout_weight="1"
            android:max="100"
            android:progress="45"
            android:progressTint="#FF9800"
            android:progressBackgroundTint="#33FFFFFF" />

        <TextView
            android:id="@+id/storage_pct"
            android:layout_width="48dp"
            android:layout_height="wrap_content"
            android:text="45%"
            android:textSize="12sp"
            android:textColor="#FFFFFF"
            android:gravity="end"
            android:layout_marginStart="8dp" />
    </LinearLayout>

    <TextView
        android:id="@+id/uptime"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Uptime: 12h 34m"
        android:textSize="12sp"
        android:textColor="#44FFFFFF"
        android:layout_marginTop="8dp" />
</LinearLayout>"""
    )
}
