package io.github.kotlinmania.filedescriptor

import io.github.kotlinmania.libc.time.StdDuration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConcreteApiTest {
    @Test
    fun duplicatedFileRemainsUsableAfterOriginalCloses() {
        val created: PipeResult = Pipe.new()
        assertNull(created.error)
        val pipe = assertNotNull(created.value)
        pipe.write.use { writer ->
            val duplicate: DescriptorFileResult = pipe.read.asFile()
            val file = assertNotNull(duplicate.value)
            assertNotEquals(pipe.read.asRawFileDescriptor(), file.asRawFileDescriptor())
            pipe.read.close()
            file.use {
                val written: WriteResult = writer.write(byteArrayOf(42))
                assertEquals(1, written.value)
                val buffer = ByteArray(1)
                val read: ReadResult = file.read(buffer)
                assertEquals(1, read.value)
                assertEquals(42, buffer[0].toInt())
                assertNull(read.error)
            }
        }
    }

    @Test
    fun stdioDuplicateOwnsAnIndependentHandle() {
        val pipe = Pipe.new().getOrThrow()
        pipe.write.use {
            val stdio = pipe.read.asStdio().getOrThrow()
            assertNotEquals(pipe.read.asRawFileDescriptor(), stdio.asRawFileDescriptor())
            pipe.read.close()
            stdio.use {
                pipe.write.write(byteArrayOf(1)).getOrThrow()
                val result: PollResult = poll(listOf(Pollfd(stdio.asRawFileDescriptor(), POLLIN)), StdDuration.ZERO)
                assertEquals(1, result.value)
                assertNull(result.error)
            }
        }
    }

    @Test
    fun failuresRetainConcreteKindAndDescriptor() {
        val result: PollResult = poll(listOf(Pollfd(Long.MAX_VALUE, POLLIN)), StdDuration.ZERO)
        assertNull(result.value)
        val error: FileDescriptorError = assertNotNull(result.error)
        assertEquals(FileDescriptorErrorKind.IllegalFdValue, error.kind)
        assertEquals(Long.MAX_VALUE, error.sourceDescriptor)
        assertTrue(error.message.isNotEmpty())
    }
}
