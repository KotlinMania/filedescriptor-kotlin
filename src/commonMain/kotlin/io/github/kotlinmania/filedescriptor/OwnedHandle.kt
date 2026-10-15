@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package io.github.kotlinmania.filedescriptor

/**
 * [OwnedHandle] manages the lifecycle of the platform [RawFileDescriptor] type.
 */
class OwnedHandle(
    var handle: Long,
    var handleType: HandleType = HandleType.Unknown,
) : AsRawFileDescriptor,
    IntoRawFileDescriptor,
    AutoCloseable {
    private var isClosed = false

    override fun asRawFileDescriptor(): Long = handle

    override fun intoRawFileDescriptor(): Long {
        isClosed = true
        return handle
    }

    /**
     * Attempt to duplicate the underlying handle and return an [OwnedHandle]
     * wrapped around the duplicate.
     */
    fun tryClone(): Result<OwnedHandle> = dupImpl(this, handleType)

    override fun close() {
        if (!isClosed) {
            isClosed = true
            closeHandlePlatform(handle, handleType)
        }
    }

    companion object {
        /**
         * Create a new handle from an object convertible to [RawFileDescriptor].
         */
        fun <F : IntoRawFileDescriptor> new(f: F): OwnedHandle {
            val handle = f.intoRawFileDescriptor()
            return OwnedHandle(
                handle = handle,
                handleType = probeHandleType(handle),
            )
        }

        fun fromRaw(handle: Long, handleType: HandleType = HandleType.Unknown): OwnedHandle =
            OwnedHandle(
                handle = handle,
                handleType = if (handleType == HandleType.Unknown) probeHandleType(handle) else handleType,
            )

        /**
         * Attempt to duplicate the underlying handle from an object representable
         * as [RawFileDescriptor].
         */
        fun <F : AsRawFileDescriptor> dup(f: F): Result<OwnedHandle> = dupImpl(f, HandleType.Unknown)
    }
}
