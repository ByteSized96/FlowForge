package com.flowforge.model

import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class FlowLog(
    val message: String,
    val status: FlowLogStatus = FlowLogStatus.INFO,
    val time: String = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
)

enum class FlowLogStatus {
    INFO,
    SUCCESS,
    WARNING,
    ERROR
}