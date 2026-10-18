package io.github.kotlinmania.filedescriptor

/** The value or error returned by this operation. */
class WriteResult private constructor(
    val value: Int?,
    val error: FileDescriptorError?,
) {
    init {
        require((value == null) != (error == null))
    }

    val isSuccess: Boolean get() = value != null

    val isFailure: Boolean get() = error != null

    fun getOrNull(): Int? = value

    fun getOrThrow(): Int =
        when {
            value != null -> value
            error != null -> throw FileDescriptorException(error)
            else -> error("WriteResult has neither a value nor an error")
        }

    companion object {
        internal fun from(result: Result<Int>): WriteResult {
            val failure = result.exceptionOrNull()
            return if (failure != null) {
                WriteResult(null, FileDescriptorError.fromFailure(failure))
            } else {
                WriteResult(result.getOrThrow(), null)
            }
        }
    }
}
