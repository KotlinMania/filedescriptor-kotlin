package io.github.kotlinmania.filedescriptor

/** An owned standard-I/O source for process redirection. */
class DescriptorStdio internal constructor(
    private val descriptor: FileDescriptor,
) : AsRawFileDescriptor,
    IntoRawFileDescriptor,
    AutoCloseable {
    override fun asRawFileDescriptor(): Long = descriptor.asRawFileDescriptor()

    override fun intoRawFileDescriptor(): Long = descriptor.intoRawFileDescriptor()

    override fun close() {
        descriptor.close()
    }
}
