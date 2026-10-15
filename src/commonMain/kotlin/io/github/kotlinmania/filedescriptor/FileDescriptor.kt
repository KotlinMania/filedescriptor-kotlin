// port-lint: source lib.rs
@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package io.github.kotlinmania.filedescriptor

/**
 * [FileDescriptor] is a wrapper on top of [OwnedHandle] exposing read and write operations.
 */
class FileDescriptor(
    val handle: OwnedHandle,
) : AsRawFileDescriptor,
    IntoRawFileDescriptor,
    AsRawSocketDescriptor,
    IntoRawSocketDescriptor,
    AutoCloseable {
    override fun asRawFileDescriptor(): Long = handle.asRawFileDescriptor()

    override fun intoRawFileDescriptor(): Long = handle.intoRawFileDescriptor()

    override fun asSocketDescriptor(): Long = handle.asRawFileDescriptor()

    override fun intoSocketDescriptor(): Long = handle.intoRawFileDescriptor()

    /**
     * Attempt to duplicate the underlying handle and return a [FileDescriptor]
     * wrapped around the duplicate.
     */
    fun tryClone(): Result<FileDescriptor> = handle.tryClone().map { FileDescriptor(it) }

    /**
     * Create a standard I/O representation for process redirection.
     */
    fun asStdio(): Result<Any?> = asStdioPlatform(this)

    /**
     * Create a file representation using a duplicated handle.
     */
    fun asFile(): Result<Any?> = asFilePlatform(this)

    /**
     * Attempt to change the non-blocking I/O mode.
     */
    fun setNonBlocking(nonBlocking: Boolean): Result<Unit> = setNonBlockingPlatform(this, nonBlocking)

    /**
     * Read bytes from the descriptor into [buf].
     */
    fun read(buf: ByteArray, offset: Int = 0, length: Int = buf.size - offset): Result<Int> = readPlatform(handle.asRawFileDescriptor(), handle.handleType, buf, offset, length)

    /**
     * Write bytes from [buf] to the descriptor.
     */
    fun write(buf: ByteArray, offset: Int = 0, length: Int = buf.size - offset): Result<Int> = writePlatform(handle.asRawFileDescriptor(), handle.handleType, buf, offset, length)

    /**
     * Flush buffered data.
     */
    fun flush(): Result<Unit> = flushPlatform(handle.asRawFileDescriptor())

    /**
     * Read all remaining bytes into a UTF-8 string.
     */
    fun readToString(): Result<String> = readToStringPlatform(this)

    override fun close() {
        handle.close()
    }

    companion object {
        /**
         * Create a new descriptor from an object convertible to [RawFileDescriptor].
         */
        fun <F : IntoRawFileDescriptor> new(f: F): FileDescriptor {
            val handle = OwnedHandle.new(f)
            return FileDescriptor(handle)
        }

        fun fromRaw(rawFd: Long): FileDescriptor = FileDescriptor(OwnedHandle.fromRaw(rawFd))

        /**
         * Attempt to duplicate the underlying handle from an object representable
         * as [RawFileDescriptor].
         */
        fun <F : AsRawFileDescriptor> dup(f: F): Result<FileDescriptor> = OwnedHandle.dup(f).map { FileDescriptor(it) }

        /**
         * Duplicate descriptor to a specific target descriptor number.
         */
        fun <F : AsRawFileDescriptor> dup2(f: F, destFd: Long): Result<FileDescriptor> = dup2Platform(f, destFd).map { FileDescriptor(it) }

        /**
         * Redirect standard I/O to the specified descriptor.
         */
        fun <F : AsRawFileDescriptor> redirectStdio(f: F, stdio: StdioDescriptor): Result<FileDescriptor> = redirectStdioPlatform(f, stdio)
    }
}
