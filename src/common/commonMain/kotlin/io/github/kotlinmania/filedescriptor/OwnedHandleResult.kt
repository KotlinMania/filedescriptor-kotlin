package io.github.kotlinmania.filedescriptor

/** The value or error returned by this operation. */
class OwnedHandleResult private constructor(
    val value: OwnedHandle?,
    val error: FileDescriptorError?,
) {
    init {
        require((value == null) != (error == null))
    }

    val isSuccess: Boolean get() = value != null

    val isFailure: Boolean get() = error != null

    fun getOrNull(): OwnedHandle? = value

    fun getOrThrow(): OwnedHandle =
        when {
            value != null -> value
            error != null -> throw FileDescriptorException(error)
            else -> error("OwnedHandleResult has neither a value nor an error")
        }

    companion object {
        internal fun from(result: Result<OwnedHandle>): OwnedHandleResult {
            val failure = result.exceptionOrNull()
            return if (failure != null) {
                OwnedHandleResult(null, FileDescriptorError.fromFailure(failure))
            } else {
                OwnedHandleResult(result.getOrThrow(), null)
            }
        }
    }
}
