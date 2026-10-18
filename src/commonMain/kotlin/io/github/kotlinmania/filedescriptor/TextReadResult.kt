package io.github.kotlinmania.filedescriptor

/** The value or error returned by this operation. */
class TextReadResult private constructor(
    val value: String?,
    val error: FileDescriptorError?,
) {
    init {
        require((value == null) != (error == null))
    }

    val isSuccess: Boolean get() = value != null

    val isFailure: Boolean get() = error != null

    fun getOrNull(): String? = value

    fun getOrThrow(): String =
        when {
            value != null -> value
            error != null -> throw FileDescriptorException(error)
            else -> error("TextReadResult has neither a value nor an error")
        }

    companion object {
        internal fun from(result: Result<String>): TextReadResult {
            val failure = result.exceptionOrNull()
            return if (failure != null) {
                TextReadResult(null, FileDescriptorError.fromFailure(failure))
            } else {
                TextReadResult(result.getOrThrow(), null)
            }
        }
    }
}
