package com.katoaapps.openminilaunch.features.mindyspace.todo

data class TodoItem(
    val id: String,
    val text: String,
    val completed: Boolean = false,
)
