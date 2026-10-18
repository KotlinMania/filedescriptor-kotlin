package io.github.kotlinmania.filedescriptor

/** The value or error returned by this operation. */
class DescriptorFileResult private constructor(
    val value: DescriptorFile?,
    val error: FileDescriptorError?,
) {
    init {
        require((value == null) != (error == null))
    }

    val isSuccess: Boolean get() = value != null

    val isFailure: Boolean get() = error != null

    fun getOrNull(): DescriptorFile? = value

    fun getOrThrow(): DescriptorFile =
        when {
            value != null -> value
            error != null -> throw FileDescriptorException(error)
            else -> error("DescriptorFileResult has neither a value nor an error")
        }

    companion object {
        internal fun from(result: Result<DescriptorFile>): DescriptorFileResult {
            val failure = result.exceptionOrNull()
            return if (failure != null) {
                DescriptorFileResult(null, FileDescriptorError.fromFailure(failure))
            } else {
                DescriptorFileResult(result.getOrThrow(), null)
            }
        }
    }
}
