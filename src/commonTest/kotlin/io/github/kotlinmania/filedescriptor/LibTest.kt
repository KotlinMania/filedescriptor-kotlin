// port-lint: tests windows.rs
package io.github.kotlinmania.filedescriptor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration

class LibTest {

    @Test
    fun testSocketpair() {
        val pair = socketpair().getOrThrow()
        val a = pair.first
        val b = pair.second
        val data = "hello".encodeToByteArray()
        val written = a.write(data).getOrThrow()
        assertEquals(5, written)
        val buf = ByteArray(5)
        val read = b.read(buf).getOrThrow()
        assertEquals(5, read)
        assertEquals("hello", buf.decodeToString())
    }

    @Test
    fun pipeReadWrite() {
        val pipe = Pipe.new().getOrThrow()
        pipe.write.write("hello".encodeToByteArray()).getOrThrow()
        pipe.write.close()

        val s = pipe.read.readToString().getOrThrow()
        assertEquals("hello", s)
    }

    @Test
    fun pollSocketPair() {
        val pair = socketpair().getOrThrow()
        val a = pair.first
        val b = pair.second
        val pollArray = mutableListOf(
            Pollfd(
                fd = a.asSocketDescriptor(),
                events = POLLIN,
                revents = 0,
            ),
        )
        val readyBefore = poll(pollArray, Duration.parse("20ms")).getOrThrow()
        assertEquals(0, readyBefore)

        b.write("hello".encodeToByteArray()).getOrThrow()
        val readyAfter = poll(pollArray, Duration.parse("20ms")).getOrThrow()
        assertEquals(1, readyAfter)
        assertTrue((pollArray[0].revents.toInt() and POLLIN.toInt()) != 0)
    }

    @Test
    fun testFdSet() {
        val fdSet = FdSet()
        assertTrue(fdSet.checkFd(5L).isSuccess)
        fdSet.add(5L)
        assertTrue(fdSet.contains(5L))
        assertTrue(fdSet.isSet(5L))
        assertTrue(!fdSet.contains(6L))
    }

    @Test
    fun testDup() {
        val pipe = Pipe.new().getOrThrow()
        val dupedRead = pipe.read.tryClone().getOrThrow()
        pipe.write.write("world".encodeToByteArray()).getOrThrow()
        val buf = ByteArray(5)
        val read = dupedRead.read(buf).getOrThrow()
        assertEquals(5, read)
        assertEquals("world", buf.decodeToString())
    }
}
