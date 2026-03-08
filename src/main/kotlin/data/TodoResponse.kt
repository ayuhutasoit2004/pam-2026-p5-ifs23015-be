package org.delcom.data

import kotlinx.serialization.Serializable
import org.delcom.entities.Todo

@Serializable
data class TodoResponse(
    val todo: Todo
)

@Serializable
data class TodoIdResponse(
    val todoId: String
)