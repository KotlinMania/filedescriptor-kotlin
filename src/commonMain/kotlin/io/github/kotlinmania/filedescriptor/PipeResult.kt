package io.github.kotlinmania.filedescriptor

/** The value or error returned by this operation. */
class PipeResult private constructor(
    val value: Pipe?,
    val error: FileDescriptorError?,
) {
    init {
        require((value == null) != (error == null))
    }

    val isSuccess: Boolean get() = value != null

    val isFailure: Boolean get() = error != null

    fun getOrNull(): Pipe? = value

    fun getOrThrow(): Pipe =
        when {
            value != null -> value
            error != null -> throw FileDescriptorException(error)
            else -> error("PipeResult has neither a value nor an error")
        }

    companion object {
        internal fun from(result: Result<Pipe>): PipeResult {
            val failure = result.exceptionOrNull()
            return if (failure != null) {
                PipeResult(null, FileDescriptorError.fromFailure(failure))
            } else {
                PipeResult(result.getOrThrow(), null)
            }
        }
    }
}
