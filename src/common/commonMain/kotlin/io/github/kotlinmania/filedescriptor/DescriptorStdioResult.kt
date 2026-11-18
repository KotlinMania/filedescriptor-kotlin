package io.github.kotlinmania.filedescriptor

/** The value or error returned by this operation. */
class DescriptorStdioResult private constructor(
    val value: DescriptorStdio?,
    val error: FileDescriptorError?,
) {
    init {
        require((value == null) != (error == null))
    }

    val isSuccess: Boolean get() = value != null

    val isFailure: Boolean get() = error != null

    fun getOrNull(): DescriptorStdio? = value

    fun getOrThrow(): DescriptorStdio =
        when {
            value != null -> value
            error != null -> throw FileDescriptorException(error)
            else -> error("DescriptorStdioResult has neither a value nor an error")
        }

    companion object {
        internal fun from(result: Result<DescriptorStdio>): DescriptorStdioResult {
            val failure = result.exceptionOrNull()
            return if (failure != null) {
                DescriptorStdioResult(null, FileDescriptorError.fromFailure(failure))
            } else {
                DescriptorStdioResult(result.getOrThrow(), null)
            }
        }
    }
}
