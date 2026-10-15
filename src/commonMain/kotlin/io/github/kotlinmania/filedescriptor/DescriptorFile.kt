package io.github.kotlinmania.filedescriptor

/** An owned file created by duplicating a descriptor. */
class DescriptorFile internal constructor(
    private val descriptor: FileDescriptor,
) : AsRawFileDescriptor,
    IntoRawFileDescriptor,
    AutoCloseable {
    override fun asRawFileDescriptor(): Long = descriptor.asRawFileDescriptor()

    override fun intoRawFileDescriptor(): Long = descriptor.intoRawFileDescriptor()

    fun read(buf: ByteArray, offset: Int = 0, length: Int = buf.size - offset): ReadResult =
        descriptor.read(buf, offset, length)

    fun write(buf: ByteArray, offset: Int = 0, length: Int = buf.size - offset): WriteResult =
        descriptor.write(buf, offset, length)

    fun flush(): DescriptorStatus = descriptor.flush()

    fun readToString(): TextReadResult = descriptor.readToString()

    override fun close() {
        descriptor.close()
    }
}
