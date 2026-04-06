package hk.uwu.reareye.ui.components.config

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hk.uwu.reareye.repository.rearwidget.RearBusinessConfig
import hk.uwu.reareye.repository.rearwidget.RearCardConfig
import hk.uwu.reareye.repository.rearwidget.RearTemplatePresets
import hk.uwu.reareye.repository.rearwidget.RearWidgetConfigCodec
import hk.uwu.reareye.repository.rearwidget.RearWidgetManagerRepository
import hk.uwu.reareye.ui.components.motion.ArtRevealItem
import hk.uwu.reareye.ui.components.motion.ArtStaggeredReveal
import hk.uwu.reareye.ui.config.PrefsManager
import hk.uwu.reareye.ui.theme.rearAcrylicEffect
import hk.uwu.reareye.ui.theme.rearAcrylicSource
import hk.uwu.reareye.ui.theme.rememberAcrylicHazeState
import hk.uwu.reareye.ui.theme.rememberAcrylicHazeStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.BasicComponentDefaults
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import java.io.File

@SuppressLint("LocalContextGetResourceValueCall")
@Composable
fun TemplateMarketScreen(
    prefsManager: PrefsManager,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val layoutDirection = LocalLayoutDirection.current
    val scope = rememberCoroutineScope()
    val scrollBehavior = MiuixScrollBehavior()
    val hazeState = rememberAcrylicHazeState()
    val hazeStyle = rememberAcrylicHazeStyle()

    val templates = remember { RearTemplatePresets.all }
    val categories = remember { listOf("All") + RearTemplatePresets.categories() }
    var selectedCategory by remember { mutableStateOf("All") }
    var importedIds by remember { mutableStateOf(setOf<String>()) }
    var importInProgress by remember { mutableStateOf<String?>(null) }
    var screenVisible by remember { mutableStateOf(false) }

    val filteredTemplates = remember(selectedCategory, templates) {
        if (selectedCategory == "All") templates
        else RearTemplatePresets.byCategory(selectedCategory)
    }

    // Check which templates are already imported
    LaunchedEffect(Unit) {
        screenVisible = false
        delay(100)
        val businesses = withContext(Dispatchers.IO) {
            RearWidgetManagerRepository.loadBusinesses(prefsManager)
        }
        importedIds = templates
            .filter { t -> businesses.any { it.business == t.business } }
            .map { it.id }
            .toSet()
        delay(120)
        screenVisible = true
    }

    fun importTemplate(template: RearTemplatePresets.PresetTemplate) {
        scope.launch {
            importInProgress = template.id
            withContext(Dispatchers.IO) {
                // Write template content to managed directory
                val root = File(context.filesDir, "rear_widget_business")
                if (!root.exists()) root.mkdirs()

                val safeName = template.business.replace(Regex("[^a-zA-Z0-9._-]"), "_")
                val target = File(root, "${safeName}_${System.currentTimeMillis()}.xml")
                target.writeText(template.templateContent)

                // Load existing businesses and add the new one
                val existing = RearWidgetManagerRepository.loadBusinesses(prefsManager)
                if (existing.none { it.business == template.business }) {
                    val newBusiness = RearBusinessConfig(
                        id = RearWidgetConfigCodec.newBusinessId(
                            "com.xiaomi.subscreencenter",
                            template.business
                        ),
                        packageName = "com.xiaomi.subscreencenter",
                        business = template.business,
                        filePath = target.absolutePath,
                    )

                    val newCard = RearCardConfig(
                        id = RearWidgetConfigCodec.newCardId(),
                        title = template.name,
                        packageName = "com.xiaomi.subscreencenter",
                        business = template.business,
                        enabled = true,
                        sticky = true,
                        priority = 500,
                    )

                    // Save businesses
                    val updatedBusinesses = existing + newBusiness
                    RearWidgetManagerRepository.saveBusinesses(
                        context, prefsManager, updatedBusinesses
                    )

                    // Save cards
                    val existingCards = RearWidgetManagerRepository.loadCards(prefsManager)
                    val updatedCards = existingCards + newCard
                    RearWidgetManagerRepository.saveCards(
                        context, prefsManager, updatedCards
                    )
                }
            }

            importedIds = importedIds + template.id
            importInProgress = null
            Toast.makeText(
                context,
                "Imported: ${template.name}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.rearAcrylicEffect(hazeState, hazeStyle),
                color = Color.Transparent,
                title = "Template Market",
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
            // Header card
            item {
                ArtRevealItem(visible = screenVisible, delayMillis = 0) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        insideMargin = PaddingValues(16.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(28.dp),
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Template Market",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = "One-tap import built-in templates to your rear screen",
                                    fontSize = 13.sp,
                                    color = BasicComponentDefaults.summaryColor().color,
                                )
                            }
                        }
                    }
                }
            }

            // Category filter chips
            item {
                ArtRevealItem(visible = screenVisible, delayMillis = 60) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        categories.forEach { category ->
                            val selected = category == selectedCategory
                            val bgColor = if (selected) Color(0xFF5B8DEF) else Color(0xFF2A2A2A)
                            val textColor = if (selected) Color.White else Color(0xFFAAAAAA)
                            Text(
                                text = category,
                                fontSize = 13.sp,
                                color = textColor,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(bgColor)
                                    .clickable { selectedCategory = category }
                                    .padding(horizontal = 14.dp, vertical = 7.dp),
                            )
                        }
                    }
                }
            }

            // Template cards
            itemsIndexed(filteredTemplates, key = { _, t -> t.id }) { index, template ->
                ArtStaggeredReveal(
                    visible = screenVisible,
                    revealKey = template.id,
                    delayMillis = (80 + index * 25).coerceAtMost(200),
                ) {
                    val isImported = template.id in importedIds
                    val isImporting = importInProgress == template.id

                    TemplateCard(
                        template = template,
                        isImported = isImported,
                        isImporting = isImporting,
                        onImport = { importTemplate(template) },
                    )
                }
            }

            // Empty state
            if (filteredTemplates.isEmpty()) {
                item {
                    ArtRevealItem(visible = screenVisible, delayMillis = 40) {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "No templates in this category",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                textAlign = TextAlign.Center,
                                color = BasicComponentDefaults.summaryColor().color,
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TemplateCard(
    template: RearTemplatePresets.PresetTemplate,
    isImported: Boolean,
    isImporting: Boolean,
    onImport: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(dampingRatio = 0.9f, stiffness = 620f)
            ),
        insideMargin = PaddingValues(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Emoji icon
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = template.iconEmoji,
                    fontSize = 28.sp,
                )
            }

            // Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = template.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = template.description,
                    fontSize = 13.sp,
                    color = BasicComponentDefaults.summaryColor().color,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = template.category,
                        fontSize = 11.sp,
                        color = Color(0xFF5B8DEF),
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                    Text(
                        text = template.business,
                        fontSize = 11.sp,
                        color = BasicComponentDefaults.summaryColor().color.copy(alpha = 0.7f),
                    )
                }
            }

            // Action button
            when {
                isImported -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = "Added",
                            fontSize = 12.sp,
                            color = Color(0xFF4CAF50),
                        )
                    }
                }

                isImporting -> {
                    Text(
                        text = "Importing...",
                        fontSize = 12.sp,
                        color = Color(0xFF5B8DEF),
                    )
                }

                else -> {
                    Button(
                        onClick = onImport,
                        colors = ButtonDefaults.buttonColorsPrimary(),
                        modifier = Modifier.padding(horizontal = 2.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Download,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Import", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
