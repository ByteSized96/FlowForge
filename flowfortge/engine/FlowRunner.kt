package com.flowforge.engine

import com.flowforge.engine.actions.CreateFileAction
import com.flowforge.engine.actions.FileExistsCondition
import com.flowforge.engine.actions.NotificationAction
import com.flowforge.engine.actions.OpenAppAction
import com.flowforge.engine.actions.OpenWebsiteAction
import com.flowforge.engine.actions.ScreenshotAction
import com.flowforge.model.FlowBlock
import com.flowforge.model.FlowLog
import com.flowforge.model.FlowLogStatus

class FlowRunner {

    fun run(
        blocks: List<FlowBlock>,
        onStepStarted: (FlowBlock) -> Unit = {},
        delayMs: Long = 600
    ): List<FlowLog> {
        val logs = mutableListOf<FlowLog>()

        logs += FlowLog("Flow started", FlowLogStatus.INFO)

        if (blocks.isEmpty()) {
            logs += FlowLog("No blocks found. Add at least one block.", FlowLogStatus.WARNING)
            logs += FlowLog("Flow stopped", FlowLogStatus.ERROR)
            return logs
        }

        blocks.forEachIndexed { index, block ->
            onStepStarted(block)

            if (delayMs > 0) {
                Thread.sleep(delayMs)
            }

            logs += FlowLog("Step ${index + 1}: ${block.title}", FlowLogStatus.INFO)
            logs += executeBlock(block)
        }

        logs += FlowLog("Flow completed successfully", FlowLogStatus.SUCCESS)

        return logs
    }

    private fun executeBlock(block: FlowBlock): FlowLog {
        return when (block.title) {
            "Manual Trigger" -> {
                FlowLog("Manual trigger accepted", FlowLogStatus.SUCCESS)
            }

            "Open Website" -> {
                try {
                    val url = block.config["url"].orEmpty()

                    if (url.isBlank()) {
                        FlowLog("Open Website skipped: no URL set", FlowLogStatus.WARNING)
                    } else {
                        OpenWebsiteAction().open(url)
                        FlowLog("Opened website: $url", FlowLogStatus.SUCCESS)
                    }
                } catch (e: Exception) {
                    FlowLog("Open Website failed: ${e.message}", FlowLogStatus.ERROR)
                }
            }

            "Take Screenshot" -> {
                try {
                    val file = ScreenshotAction().capture()
                    FlowLog("Screenshot saved: ${file.absolutePath}", FlowLogStatus.SUCCESS)
                } catch (e: Exception) {
                    FlowLog("Screenshot failed: ${e.message}", FlowLogStatus.ERROR)
                }
            }

            "Create File" -> {
                try {
                    val filename = block.config["filename"].orEmpty()
                    val content = block.config["content"].orEmpty()

                    val file = CreateFileAction().create(filename, content)
                    FlowLog("Created file: ${file.absolutePath}", FlowLogStatus.SUCCESS)
                } catch (e: Exception) {
                    FlowLog("Create File failed: ${e.message}", FlowLogStatus.ERROR)
                }
            }

            "Show Notification" -> {
                try {
                    val title = block.config["title"].orEmpty()
                    val message = block.config["message"].orEmpty()

                    NotificationAction().show(title, message)
                    FlowLog("Notification shown", FlowLogStatus.SUCCESS)
                } catch (e: Exception) {
                    FlowLog("Notification failed: ${e.message}", FlowLogStatus.ERROR)
                }
            }

            "If File Exists" -> {
                val path = block.config["path"].orEmpty()

                if (path.isBlank()) {
                    FlowLog("File exists check skipped: no path set", FlowLogStatus.WARNING)
                } else {
                    val exists = FileExistsCondition().check(path)

                    if (exists) {
                        FlowLog("Condition passed: file exists", FlowLogStatus.SUCCESS)
                    } else {
                        FlowLog("Condition failed: file does not exist", FlowLogStatus.WARNING)
                    }
                }
            }
            "Open App" -> {
                try {
                    val path = block.config["path"].orEmpty()
                    OpenAppAction().open(path)
                    FlowLog("Opened app/file: $path", FlowLogStatus.SUCCESS)
                } catch (e: Exception) {
                    FlowLog("Open App failed: ${e.message}", FlowLogStatus.ERROR)
                }
            }

            else -> {
                FlowLog("Unknown block skipped", FlowLogStatus.WARNING)
            }
        }
    }
}