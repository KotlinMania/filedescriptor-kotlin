package io.github.kotlinmania.filedescriptor

/** Success or failure of an operation that does not return a value. */
class DescriptorStatus private constructor(
    val error: FileDescriptorError?,
) {
    val isSuccess: Boolean get() = error == null

    val isFailure: Boolean get() = error != null

    fun getOrThrow() {
        if (error != null) throw FileDescriptorException(error)
    }

    companion object {
        internal fun from(result: Result<Unit>): DescriptorStatus =
            DescriptorStatus(result.exceptionOrNull()?.let { FileDescriptorError.fromFailure(it) })
    }
}
