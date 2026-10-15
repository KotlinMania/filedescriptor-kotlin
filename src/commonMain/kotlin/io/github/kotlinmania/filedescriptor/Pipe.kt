@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package io.github.kotlinmania.filedescriptor

/**
 * Represents the readable and writable ends of a pipe.
 */
class Pipe(
    val read: FileDescriptor,
    val write: FileDescriptor,
) : AutoCloseable {
    override fun close() {
        try {
            read.close()
        } finally {
            write.close()
        }
    }

    companion object {
        /**
         * Create a new unidirectional pipe pair.
         */
        fun new(): Result<Pipe> = createPipePlatform()
    }
}
