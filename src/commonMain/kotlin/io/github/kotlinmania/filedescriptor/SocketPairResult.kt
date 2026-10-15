package io.github.kotlinmania.filedescriptor

/** The value or error returned by this operation. */
class SocketPairResult private constructor(
    val value: DescriptorPair?,
    val error: FileDescriptorError?,
) {
    init {
        require((value == null) != (error == null))
    }

    val isSuccess: Boolean get() = value != null

    val isFailure: Boolean get() = error != null

    fun getOrNull(): DescriptorPair? = value

    fun getOrThrow(): DescriptorPair =
        when {
            value != null -> value
            error != null -> throw FileDescriptorException(error)
            else -> error("SocketPairResult has neither a value nor an error")
        }

    companion object {
        internal fun from(result: Result<DescriptorPair>): SocketPairResult {
            val failure = result.exceptionOrNull()
            return if (failure != null) {
                SocketPairResult(null, FileDescriptorError.fromFailure(failure))
            } else {
                SocketPairResult(result.getOrThrow(), null)
            }
        }
    }
}
