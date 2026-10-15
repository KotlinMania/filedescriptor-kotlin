package io.github.kotlinmania.filedescriptor

/** A captured operating-system error and its interruption classification. */
class SystemIoError(
    val errorCode: Int,
    val interrupted: Boolean,
    val message: String,
)
