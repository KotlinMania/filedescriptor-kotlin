package io.github.kotlinmania.filedescriptor

/** Two connected file descriptors, in creation order. */
class DescriptorPair(
    val first: FileDescriptor,
    val second: FileDescriptor,
) : AutoCloseable {
    override fun close() {
        try {
            first.close()
        } finally {
            second.close()
        }
    }
}
