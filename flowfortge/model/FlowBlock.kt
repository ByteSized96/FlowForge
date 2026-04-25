
package com.flowforge.model

data class FlowBlock(
    val id: Long,
    val title: String,
    val description: String,
    val type: FlowBlockType,
    val config: MutableMap<String, String> = mutableMapOf()
)

enum class FlowBlockType {
    TRIGGER,
    ACTION,
    CONDITION
}
