package com.flowforge.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.flowforge.engine.FlowRunner
import com.flowforge.model.FlowBlock
import com.flowforge.model.FlowBlockType
import com.flowforge.model.FlowLog
import com.flowforge.model.FlowLogStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun FlowForgeApp() {
    val runner = remember { FlowRunner() }
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    val availableBlocks = listOf(
        FlowBlock(1, "Manual Trigger", "Start a flow manually", FlowBlockType.TRIGGER),
        FlowBlock(2, "Open Website", "Open a URL in the browser", FlowBlockType.ACTION),
        FlowBlock(3, "Take Screenshot", "Capture the current screen", FlowBlockType.ACTION),
        FlowBlock(4, "Create File", "Create a new file locally", FlowBlockType.ACTION),
        FlowBlock(5, "Show Notification", "Show a desktop alert", FlowBlockType.ACTION),
        FlowBlock(6, "If File Exists", "Check whether a file exists", FlowBlockType.CONDITION),
        FlowBlock(7, "Open App", "Launch an application or file", FlowBlockType.ACTION)
    )

    var flowBlocks by remember { mutableStateOf<List<FlowBlock>>(emptyList()) }
    var logs by remember { mutableStateOf<List<FlowLog>>(emptyList()) }
    var activeBlockId by remember { mutableStateOf<Long?>(null) }
    var isRunning by remember { mutableStateOf(false) }

    var schedulerEnabled by remember { mutableStateOf(false) }
    var scheduleValueText by remember { mutableStateOf("60") }
    var scheduleUnit by remember { mutableStateOf("Seconds") }
    var nextRunText by remember { mutableStateOf("Not scheduled") }
    var lastRunText by remember { mutableStateOf("Never") }
    var scheduledRunCount by remember { mutableStateOf(0) }
    var schedulerJob by remember { mutableStateOf<Job?>(null) }

    fun runCurrentFlow(source: String = "Manual") {
        if (flowBlocks.isEmpty() || isRunning) return

        scope.launch {
            isRunning = true
            logs = logs + FlowLog("Run triggered by: $source", FlowLogStatus.INFO)

            val runLogs = runner.run(
                blocks = flowBlocks,
                onStepStarted = { block ->
                    activeBlockId = block.id
                },
                delayMs = 650
            )

            logs = logs + runLogs
            activeBlockId = null
            isRunning = false
        }
    }


    fun scheduleDelayMillis(): Long? {
        val value = scheduleValueText.toLongOrNull() ?: return null

        return when (scheduleUnit) {
            "Seconds" -> value * 1000L
            "Minutes" -> value * 60_000L
            else -> null
        }
    }

    fun scheduleLabel(): String {
        val value = scheduleValueText.toLongOrNull() ?: return "Invalid schedule"
        return "Every $value ${scheduleUnit.lowercase()}"
    }

    fun formatTimeAfterDelay(delayMillis: Long): String {
        return LocalDateTime.now()
            .plusNanos(delayMillis * 1_000_000)
            .format(DateTimeFormatter.ofPattern("HH:mm:ss"))
    }

    fun stopScheduler() {
        schedulerJob?.cancel()
        schedulerJob = null
        schedulerEnabled = false
        nextRunText = "Not scheduled"
        logs = logs + FlowLog("Scheduler stopped", FlowLogStatus.WARNING)
    }

    fun startScheduler() {
        val delayMillis = scheduleDelayMillis()

        if (delayMillis == null || delayMillis < 5000L) {
            logs = logs + FlowLog("Scheduler needs at least 5 seconds", FlowLogStatus.WARNING)
            return
        }

        if (flowBlocks.isEmpty()) {
            logs = logs + FlowLog("Add blocks before starting scheduler", FlowLogStatus.WARNING)
            return
        }

        schedulerJob?.cancel()
        schedulerEnabled = true
        scheduledRunCount = 0
        nextRunText = formatTimeAfterDelay(delayMillis)

        logs = logs + FlowLog("Scheduler started: ${scheduleLabel()}", FlowLogStatus.SUCCESS)

        schedulerJob = scope.launch {
            while (true) {
                delay(delayMillis)

                if (!isRunning) {
                    scheduledRunCount++
                    lastRunText = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                    nextRunText = formatTimeAfterDelay(delayMillis)
                    runCurrentFlow("Scheduler")
                } else {
                    logs = logs + FlowLog("Scheduler skipped run because flow is already running", FlowLogStatus.WARNING)
                    nextRunText = formatTimeAfterDelay(delayMillis)
                }
            }
        }
    }

    fun makeBlock(
        title: String,
        description: String,
        type: FlowBlockType,
        config: Map<String, String> = emptyMap()
    ): FlowBlock {
        return FlowBlock(
            id = System.nanoTime(),
            title = title,
            description = description,
            type = type,
            config = config.toMutableMap()
        )
    }

    fun loadTemplate(name: String) {
        flowBlocks = when (name) {
            "Screenshot Saver" -> listOf(
                makeBlock("Manual Trigger", "Start a flow manually", FlowBlockType.TRIGGER),
                makeBlock("Take Screenshot", "Capture the current screen", FlowBlockType.ACTION),
                makeBlock(
                    "Show Notification",
                    "Show a desktop alert",
                    FlowBlockType.ACTION,
                    mapOf(
                        "title" to "FlowForge",
                        "message" to "Screenshot saved successfully"
                    )
                )
            )

            "Website Launcher" -> listOf(
                makeBlock("Manual Trigger", "Start a flow manually", FlowBlockType.TRIGGER),
                makeBlock(
                    "Open Website",
                    "Open a URL in the browser",
                    FlowBlockType.ACTION,
                    mapOf("url" to "https://1-byte.co.uk")
                ),
                makeBlock(
                    "Show Notification",
                    "Show a desktop alert",
                    FlowBlockType.ACTION,
                    mapOf(
                        "title" to "FlowForge",
                        "message" to "Website opened"
                    )
                )
            )

            "File Creator" -> listOf(
                makeBlock("Manual Trigger", "Start a flow manually", FlowBlockType.TRIGGER),
                makeBlock(
                    "Create File",
                    "Create a new file locally",
                    FlowBlockType.ACTION,
                    mapOf(
                        "filename" to "flowforge-note.txt",
                        "content" to "Created by FlowForge template"
                    )
                ),
                makeBlock(
                    "Show Notification",
                    "Show a desktop alert",
                    FlowBlockType.ACTION,
                    mapOf(
                        "title" to "FlowForge",
                        "message" to "File created"
                    )
                )
            )

            "Morning Startup" -> listOf(
                makeBlock("Manual Trigger", "Start a flow manually", FlowBlockType.TRIGGER),
                makeBlock(
                    "Open App",
                    "Launch an application or file",
                    FlowBlockType.ACTION,
                    mapOf("path" to "C:/Windows/System32/notepad.exe")
                ),
                makeBlock(
                    "Open Website",
                    "Open a URL in the browser",
                    FlowBlockType.ACTION,
                    mapOf("url" to "https://github.com")
                ),
                makeBlock(
                    "Show Notification",
                    "Show a desktop alert",
                    FlowBlockType.ACTION,
                    mapOf(
                        "title" to "FlowForge",
                        "message" to "Morning startup flow completed"
                    )
                )
            )

            else -> emptyList()
        }

        logs = logs + FlowLog("Loaded template: $name", FlowLogStatus.SUCCESS)
    }

    fun moveBlockUp(id: Long) {
        val index = flowBlocks.indexOfFirst { it.id == id }
        if (index > 0) {
            flowBlocks = flowBlocks.toMutableList().also {
                val item = it.removeAt(index)
                it.add(index - 1, item)
            }
        }
    }

    fun moveBlockDown(id: Long) {
        val index = flowBlocks.indexOfFirst { it.id == id }
        if (index >= 0 && index < flowBlocks.lastIndex) {
            flowBlocks = flowBlocks.toMutableList().also {
                val item = it.removeAt(index)
                it.add(index + 1, item)
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    DisposableEffect(Unit) {
        onDispose {
            schedulerJob?.cancel()
        }
    }

    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .focusable()
                .onPreviewKeyEvent { event ->
                    if (
                        event.type == KeyEventType.KeyDown &&
                        event.isCtrlPressed &&
                        event.isShiftPressed &&
                        event.key == Key.F
                    ) {
                        runCurrentFlow("Ctrl + Shift + F")
                        true
                    } else {
                        false
                    }
                }
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF0F172A),
                            Color(0xFF111827),
                            Color(0xFF020617)
                        )
                    )
                )
                .padding(18.dp)
        ) {
            AppHeader(
                blockCount = flowBlocks.size,
                isRunning = isRunning,
                schedulerEnabled = schedulerEnabled
            )

            Spacer(Modifier.height(14.dp))

            Row(modifier = Modifier.fillMaxSize()) {
                BlockSidebar(
                    blocks = availableBlocks,
                    onAddBlock = { block ->
                        flowBlocks = flowBlocks + block.copy(id = System.nanoTime())
                    },
                    onLoadTemplate = ::loadTemplate,
                    modifier = Modifier.width(280.dp)
                )

                Spacer(Modifier.width(12.dp))

                FlowCanvas(
                    blocks = flowBlocks,
                    activeBlockId = activeBlockId,
                    onBlockUpdate = { updated ->
                        flowBlocks = flowBlocks.map {
                            if (it.id == updated.id) updated else it
                        }
                    },
                    onRemoveBlock = { id ->
                        flowBlocks = flowBlocks.filterNot { it.id == id }
                    },
                    onMoveBlockUp = ::moveBlockUp,
                    onMoveBlockDown = ::moveBlockDown,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )

                Spacer(Modifier.width(12.dp))

                RunPanel(
                    logs = logs,
                    blockCount = flowBlocks.size,
                    isRunning = isRunning,
                    schedulerEnabled = schedulerEnabled,
                    scheduleValueText = scheduleValueText,
                    scheduleUnit = scheduleUnit,
                    nextRunText = nextRunText,
                    lastRunText = lastRunText,
                    scheduledRunCount = scheduledRunCount,
                    onScheduleValueChange = { scheduleValueText = it.filter { char -> char.isDigit() } },
                    onScheduleUnitChange = { scheduleUnit = it },
                    onRun = { runCurrentFlow("Run button") },
                    onClear = {
                        flowBlocks = emptyList()
                        logs = emptyList()
                        activeBlockId = null
                    },
                    onStartScheduler = { startScheduler() },
                    onStopScheduler = { stopScheduler() },
                    modifier = Modifier.width(300.dp)
                )
            }
        }
    }
}

@Composable
private fun AppHeader(
    blockCount: Int,
    isRunning: Boolean,
    schedulerEnabled: Boolean
) {
    Card(
        backgroundColor = Color(0xEE111827),
        elevation = 8.dp,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "FlowForge",
                    color = Color.White,
                    style = MaterialTheme.typography.h4,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Visual desktop automation builder • Ctrl + Shift + F to run",
                    color = Color(0xFFCBD5E1),
                    style = MaterialTheme.typography.body1
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusPill("$blockCount blocks")
                Spacer(Modifier.width(8.dp))
                StatusPill(if (isRunning) "running" else "ready")
                Spacer(Modifier.width(8.dp))
                StatusPill(if (schedulerEnabled) "scheduler on" else "scheduler off")
            }
        }
    }
}

@Composable
private fun BlockSidebar(
    blocks: List<FlowBlock>,
    onAddBlock: (FlowBlock) -> Unit,
    onLoadTemplate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        elevation = 8.dp,
        backgroundColor = Color(0xEE111827),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.fillMaxHeight()
    ) {
        Column(Modifier.padding(14.dp)) {
            Text("Block Library", color = Color.White, style = MaterialTheme.typography.h6)
            Text("Click to add blocks", color = Color(0xFF94A3B8), style = MaterialTheme.typography.body2)

            Spacer(Modifier.height(14.dp))

            LazyColumn {
                item {
                    SectionLabel("Quick Start")
                    TemplateButton("Screenshot Saver") { onLoadTemplate("Screenshot Saver") }
                    TemplateButton("Website Launcher") { onLoadTemplate("Website Launcher") }
                    TemplateButton("File Creator") { onLoadTemplate("File Creator") }
                    TemplateButton("Morning Startup") { onLoadTemplate("Morning Startup") }

                    Spacer(Modifier.height(14.dp))
                    SectionLabel("Triggers")
                }

                items(blocks.filter { it.type == FlowBlockType.TRIGGER }) { block ->
                    BlockCard(block) { onAddBlock(block) }
                }

                item {
                    Spacer(Modifier.height(8.dp))
                    SectionLabel("Actions")
                }

                items(blocks.filter { it.type == FlowBlockType.ACTION }) { block ->
                    BlockCard(block) { onAddBlock(block) }
                }

                item {
                    Spacer(Modifier.height(8.dp))
                    SectionLabel("Conditions")
                }

                items(blocks.filter { it.type == FlowBlockType.CONDITION }) { block ->
                    BlockCard(block) { onAddBlock(block) }
                }
            }
        }
    }
}

@Composable
private fun TemplateButton(text: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
    ) {
        Text(text)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        color = Color(0xFF38BDF8),
        style = MaterialTheme.typography.caption,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun BlockCard(block: FlowBlock, onClick: () -> Unit) {
    Card(
        elevation = 4.dp,
        backgroundColor = Color(0xFF1E293B),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clickable { onClick() }
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(block.title, color = Color.White, style = MaterialTheme.typography.subtitle1)
                TypeChip(block.type)
            }

            Spacer(Modifier.height(5.dp))

            Text(block.description, color = Color(0xFFCBD5E1), style = MaterialTheme.typography.body2)
        }
    }
}

@Composable
private fun FlowCanvas(
    blocks: List<FlowBlock>,
    activeBlockId: Long?,
    onBlockUpdate: (FlowBlock) -> Unit,
    onRemoveBlock: (Long) -> Unit,
    onMoveBlockUp: (Long) -> Unit,
    onMoveBlockDown: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        elevation = 8.dp,
        backgroundColor = Color(0xEE0F172A),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Flow Canvas", color = Color.White, style = MaterialTheme.typography.h6)
                    Text(
                        "Drag blocks up/down or use controls",
                        color = Color(0xFF94A3B8),
                        style = MaterialTheme.typography.body2
                    )
                }

                StatusPill("${blocks.size} steps")
            }

            Spacer(Modifier.height(14.dp))

            if (blocks.isEmpty()) {
                EmptyCanvas()
            } else {
                LazyColumn {
                    items(blocks, key = { it.id }) { block ->
                        CanvasBlock(
                            block = block,
                            isActive = block.id == activeBlockId,
                            onConfigChange = onBlockUpdate,
                            onRemove = { onRemoveBlock(block.id) },
                            onMoveUp = { onMoveBlockUp(block.id) },
                            onMoveDown = { onMoveBlockDown(block.id) },
                            onDragUp = { onMoveBlockUp(block.id) },
                            onDragDown = { onMoveBlockDown(block.id) }
                        )

                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyCanvas() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x661E293B), RoundedCornerShape(18.dp))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("No automation yet", color = Color.White, style = MaterialTheme.typography.h6)
            Spacer(Modifier.height(8.dp))
            Text(
                "Add a block or load a Quick Start template.",
                color = Color(0xFFCBD5E1)
            )
        }
    }
}

@Composable
private fun CanvasBlock(
    block: FlowBlock,
    isActive: Boolean,
    onConfigChange: (FlowBlock) -> Unit,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDragUp: () -> Unit,
    onDragDown: () -> Unit
) {
    var dragAmountY by remember { mutableStateOf(0f) }

    Card(
        elevation = if (isActive) 14.dp else 6.dp,
        backgroundColor = if (isActive) Color(0xFF14532D) else Color(0xFF1E293B),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(block.id) {
                detectDragGestures(
                    onDragEnd = { dragAmountY = 0f },
                    onDragCancel = { dragAmountY = 0f },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragAmountY += dragAmount.y

                        if (dragAmountY > 70f) {
                            onDragDown()
                            dragAmountY = 0f
                        }

                        if (dragAmountY < -70f) {
                            onDragUp()
                            dragAmountY = 0f
                        }
                    }
                )
            }
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            block.title,
                            color = Color.White,
                            style = MaterialTheme.typography.subtitle1,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.width(8.dp))

                        TypeChip(block.type)

                        if (isActive) {
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "⚡ Running",
                                color = Color(0xFF86EFAC),
                                style = MaterialTheme.typography.caption,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    Text(block.description, color = Color(0xFFCBD5E1), style = MaterialTheme.typography.body2)

                    Spacer(Modifier.height(6.dp))

                    Text(
                        "⋮⋮ drag to reorder",
                        color = Color(0xFF64748B),
                        style = MaterialTheme.typography.caption
                    )
                }

                Row {
                    SmallButton("↑", onMoveUp)
                    Spacer(Modifier.width(4.dp))
                    SmallButton("↓", onMoveDown)
                    Spacer(Modifier.width(4.dp))
                    SmallButton("Remove", onRemove)
                }
            }

            ConfigFields(block, onConfigChange)
        }
    }
}

@Composable
private fun ConfigFields(block: FlowBlock, onConfigChange: (FlowBlock) -> Unit) {
    when (block.title) {
        "Open Website" -> {
            Spacer(Modifier.height(12.dp))
            ConfigTextField(
                value = block.config["url"].orEmpty(),
                label = "Website URL",
                placeholder = "https://1-byte.co.uk"
            ) { updateConfig(block, "url", it, onConfigChange) }
        }

        "Create File" -> {
            Spacer(Modifier.height(12.dp))
            ConfigTextField(
                value = block.config["filename"].orEmpty(),
                label = "Filename",
                placeholder = "example.txt"
            ) { updateConfig(block, "filename", it, onConfigChange) }

            Spacer(Modifier.height(8.dp))

            ConfigTextField(
                value = block.config["content"].orEmpty(),
                label = "File content",
                placeholder = "Created by FlowForge"
            ) { updateConfig(block, "content", it, onConfigChange) }
        }

        "Show Notification" -> {
            Spacer(Modifier.height(12.dp))
            ConfigTextField(
                value = block.config["title"].orEmpty(),
                label = "Notification title",
                placeholder = "FlowForge"
            ) { updateConfig(block, "title", it, onConfigChange) }

            Spacer(Modifier.height(8.dp))

            ConfigTextField(
                value = block.config["message"].orEmpty(),
                label = "Notification message",
                placeholder = "Your automation has finished"
            ) { updateConfig(block, "message", it, onConfigChange) }
        }

        "If File Exists" -> {
            Spacer(Modifier.height(12.dp))
            ConfigTextField(
                value = block.config["path"].orEmpty(),
                label = "File path",
                placeholder = "C:/Users/You/Documents/example.txt"
            ) { updateConfig(block, "path", it, onConfigChange) }
        }

        "Open App" -> {
            Spacer(Modifier.height(12.dp))
            ConfigTextField(
                value = block.config["path"].orEmpty(),
                label = "App or file path",
                placeholder = "C:/Windows/System32/notepad.exe"
            ) { updateConfig(block, "path", it, onConfigChange) }
        }
    }
}

private fun updateConfig(
    block: FlowBlock,
    key: String,
    value: String,
    onConfigChange: (FlowBlock) -> Unit
) {
    val updatedConfig = block.config.toMutableMap()
    updatedConfig[key] = value
    onConfigChange(block.copy(config = updatedConfig))
}

@Composable
private fun ConfigTextField(
    value: String,
    label: String,
    placeholder: String,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, color = Color(0xFFCBD5E1)) },
        placeholder = { Text(placeholder, color = Color(0xFF64748B)) },
        modifier = Modifier.fillMaxWidth(),
        colors = TextFieldDefaults.outlinedTextFieldColors(
            textColor = Color.White,
            focusedBorderColor = Color(0xFF38BDF8),
            unfocusedBorderColor = Color(0xFF475569),
            cursorColor = Color(0xFF38BDF8)
        )
    )
}

@Composable
private fun RunPanel(
    logs: List<FlowLog>,
    blockCount: Int,
    isRunning: Boolean,
    schedulerEnabled: Boolean,
    scheduleValueText: String,
    scheduleUnit: String,
    nextRunText: String,
    lastRunText: String,
    scheduledRunCount: Int,
    onScheduleValueChange: (String) -> Unit,
    onScheduleUnitChange: (String) -> Unit,
    onRun: () -> Unit,
    onClear: () -> Unit,
    onStartScheduler: () -> Unit,
    onStopScheduler: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        elevation = 8.dp,
        backgroundColor = Color(0xEE111827),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.fillMaxHeight()
    ) {
        Column(Modifier.padding(14.dp)) {
            Text("Run Control", color = Color.White, style = MaterialTheme.typography.h6)
            Text("Execute and debug flows", color = Color(0xFF94A3B8), style = MaterialTheme.typography.body2)

            Spacer(Modifier.height(14.dp))

            Button(
                onClick = onRun,
                enabled = blockCount > 0 && !isRunning,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isRunning) "Running..." else "Run Flow")
            }

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                onClick = onClear,
                enabled = !isRunning && !schedulerEnabled,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Clear Flow")
            }

            Spacer(Modifier.height(18.dp))

            SchedulerPanel(
                schedulerEnabled = schedulerEnabled,
                scheduleValueText = scheduleValueText,
                scheduleUnit = scheduleUnit,
                nextRunText = nextRunText,
                lastRunText = lastRunText,
                scheduledRunCount = scheduledRunCount,
                onScheduleValueChange = onScheduleValueChange,
                onScheduleUnitChange = onScheduleUnitChange,
                onStartScheduler = onStartScheduler,
                onStopScheduler = onStopScheduler
            )

            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Logs", color = Color.White, style = MaterialTheme.typography.subtitle1)
                StatusPill("${logs.size}")
            }

            Spacer(Modifier.height(8.dp))

            if (logs.isEmpty()) {
                Text(
                    "Run a flow to see execution logs here.",
                    color = Color(0xFF94A3B8),
                    style = MaterialTheme.typography.body2
                )
            } else {
                LazyColumn {
                    items(logs) { log ->
                        LogRow(log)
                    }
                }
            }
        }
    }
}

@Composable
private fun SchedulerPanel(
    schedulerEnabled: Boolean,
    scheduleValueText: String,
    scheduleUnit: String,
    nextRunText: String,
    lastRunText: String,
    scheduledRunCount: Int,
    onScheduleValueChange: (String) -> Unit,
    onScheduleUnitChange: (String) -> Unit,
    onStartScheduler: () -> Unit,
    onStopScheduler: () -> Unit
) {
    Card(
        backgroundColor = Color(0xFF020617),
        shape = RoundedCornerShape(16.dp),
        elevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Scheduler",
                    color = Color.White,
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.Bold
                )

                StatusPill(if (schedulerEnabled) "ON" else "OFF")
            }

            Spacer(Modifier.height(8.dp))

            Text(
                "Run this flow repeatedly on a timer.",
                color = Color(0xFF94A3B8),
                style = MaterialTheme.typography.body2
            )

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = scheduleValueText,
                onValueChange = onScheduleValueChange,
                label = { Text("Repeat every", color = Color(0xFFCBD5E1)) },
                placeholder = { Text("60", color = Color(0xFF64748B)) },
                enabled = !schedulerEnabled,
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    textColor = Color.White,
                    focusedBorderColor = Color(0xFF38BDF8),
                    unfocusedBorderColor = Color(0xFF475569),
                    cursorColor = Color(0xFF38BDF8),
                    disabledTextColor = Color(0xFF94A3B8),
                    disabledBorderColor = Color(0xFF334155),
                    disabledLabelColor = Color(0xFF64748B)
                )
            )

            Spacer(Modifier.height(8.dp))

            Row {
                ScheduleUnitButton(
                    text = "Seconds",
                    selected = scheduleUnit == "Seconds",
                    enabled = !schedulerEnabled,
                    onClick = { onScheduleUnitChange("Seconds") },
                    modifier = Modifier.weight(1f)
                )

                Spacer(Modifier.width(8.dp))

                ScheduleUnitButton(
                    text = "Minutes",
                    selected = scheduleUnit == "Minutes",
                    enabled = !schedulerEnabled,
                    onClick = { onScheduleUnitChange("Minutes") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(10.dp))

            SchedulerInfoRow("Next run", nextRunText)
            SchedulerInfoRow("Last run", lastRunText)
            SchedulerInfoRow("Runs", scheduledRunCount.toString())

            Spacer(Modifier.height(10.dp))

            if (schedulerEnabled) {
                OutlinedButton(
                    onClick = onStopScheduler,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Stop Scheduler")
                }
            } else {
                Button(
                    onClick = onStartScheduler,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Start Scheduler")
                }
            }
        }
    }
}

@Composable
private fun ScheduleUnitButton(
    text: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val background = if (selected) Color(0xFF0C4A6E) else Color.Transparent
    val textColor = if (selected) Color(0xFFBAE6FD) else Color(0xFFCBD5E1)

    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        colors = ButtonDefaults.outlinedButtonColors(
            backgroundColor = background,
            contentColor = textColor,
            disabledContentColor = Color(0xFF64748B)
        )
    ) {
        Text(text)
    }
}

@Composable
private fun SchedulerInfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            color = Color(0xFF94A3B8),
            style = MaterialTheme.typography.caption
        )

        Text(
            value,
            color = Color(0xFFCBD5E1),
            style = MaterialTheme.typography.caption,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun LogRow(log: FlowLog) {
    val prefix = when (log.status) {
        FlowLogStatus.INFO -> "ℹ"
        FlowLogStatus.SUCCESS -> "✓"
        FlowLogStatus.WARNING -> "!"
        FlowLogStatus.ERROR -> "✕"
    }

    val color = when (log.status) {
        FlowLogStatus.INFO -> Color(0xFFCBD5E1)
        FlowLogStatus.SUCCESS -> Color(0xFF86EFAC)
        FlowLogStatus.WARNING -> Color(0xFFFDE68A)
        FlowLogStatus.ERROR -> Color(0xFFFCA5A5)
    }

    Card(
        backgroundColor = Color(0xFF020617),
        shape = RoundedCornerShape(12.dp),
        elevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 7.dp)
    ) {
        Text(
            text = "$prefix [${log.time}] ${log.message}",
            color = color,
            style = MaterialTheme.typography.body2,
            modifier = Modifier.padding(10.dp)
        )
    }
}

@Composable
private fun TypeChip(type: FlowBlockType) {
    val color = when (type) {
        FlowBlockType.TRIGGER -> Color(0xFF38BDF8)
        FlowBlockType.ACTION -> Color(0xFF22C55E)
        FlowBlockType.CONDITION -> Color(0xFFF59E0B)
    }

    Text(
        text = type.name,
        color = Color.Black,
        style = MaterialTheme.typography.caption,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(color, RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

@Composable
private fun StatusPill(text: String) {
    Text(
        text,
        color = Color(0xFFBAE6FD),
        style = MaterialTheme.typography.caption,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(Color(0xFF0C4A6E), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    )
}

@Composable
private fun SmallButton(text: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
        modifier = Modifier.height(32.dp)
    ) {
        Text(text)
    }
}
