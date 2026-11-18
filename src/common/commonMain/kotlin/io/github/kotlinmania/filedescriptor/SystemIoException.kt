package io.github.kotlinmania.filedescriptor

import io.github.kotlinmania.thiserror.StdError

/** A captured system I/O failure, including whether the operation was interrupted. */
internal class SystemIoException(
    val errorCode: Int,
    val interrupted: Boolean,
    message: String,
) : Exception(message),
    StdError
