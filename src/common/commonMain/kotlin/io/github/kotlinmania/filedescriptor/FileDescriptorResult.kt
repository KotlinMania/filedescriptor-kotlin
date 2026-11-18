package io.github.kotlinmania.filedescriptor

/** The value or error returned by this operation. */
class FileDescriptorResult private constructor(
    val value: FileDescriptor?,
    val error: FileDescriptorError?,
) {
    init {
        require((value == null) != (error == null))
    }

    val isSuccess: Boolean get() = value != null

    val isFailure: Boolean get() = error != null

    fun getOrNull(): FileDescriptor? = value

    fun getOrThrow(): FileDescriptor =
        when {
            value != null -> value
            error != null -> throw FileDescriptorException(error)
            else -> error("FileDescriptorResult has neither a value nor an error")
        }

    companion object {
        internal fun from(result: Result<FileDescriptor>): FileDescriptorResult {
            val failure = result.exceptionOrNull()
            return if (failure != null) {
                FileDescriptorResult(null, FileDescriptorError.fromFailure(failure))
            } else {
                FileDescriptorResult(result.getOrThrow(), null)
            }
        }
    }
}
