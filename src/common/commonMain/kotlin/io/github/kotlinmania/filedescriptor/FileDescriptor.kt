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
    fun tryClone(): FileDescriptorResult = FileDescriptorResult.from(dupImpl(handle, handle.handleType).map { FileDescriptor(it) })

    /**
     * Create a standard I/O representation for process redirection.
     */
    fun asStdio(): DescriptorStdioResult = DescriptorStdioResult.from(asStdioPlatform(this))

    /**
     * Create a file representation using a duplicated handle.
     */
    fun asFile(): DescriptorFileResult = DescriptorFileResult.from(asFilePlatform(this))

    /**
     * Attempt to change the non-blocking I/O mode.
     */
    fun setNonBlocking(nonBlocking: Boolean): DescriptorStatus = DescriptorStatus.from(setNonBlockingPlatform(this, nonBlocking))

    /**
     * Read bytes from the descriptor into [buf].
     */
    fun read(buf: ByteArray, offset: Int = 0, length: Int = buf.size - offset): ReadResult = ReadResult.from(readPlatform(handle.asRawFileDescriptor(), handle.handleType, buf, offset, length))

    /**
     * Write bytes from [buf] to the descriptor.
     */
    fun write(buf: ByteArray, offset: Int = 0, length: Int = buf.size - offset): WriteResult = WriteResult.from(writePlatform(handle.asRawFileDescriptor(), handle.handleType, buf, offset, length))

    /**
     * Flush buffered data.
     */
    fun flush(): DescriptorStatus = DescriptorStatus.from(flushPlatform(handle.asRawFileDescriptor()))

    /**
     * Read all remaining bytes into a UTF-8 string.
     */
    fun readToString(): TextReadResult = TextReadResult.from(readToStringPlatform(this))

    override fun close() {
        handle.close()
    }

    companion object {
        /**
         * Create a new descriptor from an object convertible to a raw file descriptor.
         */
        fun <F : IntoRawFileDescriptor> new(f: F): FileDescriptor {
            val handle = OwnedHandle.new(f)
            return FileDescriptor(handle)
        }

        fun fromRaw(rawFd: Long): FileDescriptor = FileDescriptor(OwnedHandle.fromRaw(rawFd))

        /**
         * Attempt to duplicate the underlying handle from an object representable
         * as a raw file descriptor.
         */
        fun <F : AsRawFileDescriptor> dup(f: F): FileDescriptorResult = FileDescriptorResult.from(dupImpl(f, HandleType.Unknown).map { FileDescriptor(it) })

        /**
         * Duplicate descriptor to a specific target descriptor number.
         */
        fun <F : AsRawFileDescriptor> dup2(f: F, destFd: Long): FileDescriptorResult = FileDescriptorResult.from(dup2Platform(f, destFd).map { FileDescriptor(it) })

        /**
         * Redirect standard I/O to the specified descriptor.
         */
        fun <F : AsRawFileDescriptor> redirectStdio(f: F, stdio: StdioDescriptor): FileDescriptorResult = FileDescriptorResult.from(redirectStdioPlatform(f, stdio))
    }
}
