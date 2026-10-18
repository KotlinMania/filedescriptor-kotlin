package io.github.kotlinmania.filedescriptor

internal class FileDescriptorException(
    val error: FileDescriptorError,
) : Exception(error.message)
