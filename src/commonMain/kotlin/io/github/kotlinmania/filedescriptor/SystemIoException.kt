package io.github.kotlinmania.filedescriptor

/** A captured system I/O failure, including whether the operation was interrupted. */
class SystemIoException(
    val errorCode: Int,
    val interrupted: Boolean,
    message: String,
) : Exception(message)
