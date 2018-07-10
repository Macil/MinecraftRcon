package tech.macil.minecraft.rcon.util

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ByteArrayQueueInputStreamTest {
    private val executor = Executors.newScheduledThreadPool(2)

    @AfterAll
    fun tearDown() {
        executor.shutdownNow()
    }

    @Test
    fun readByteBlocks() {
        val afterRead = AtomicBoolean(false)
        val (input, output) = ByteArrayQueue.makePair()

        val setterFuture = executor.schedule({
            assertFalse(afterRead.get())
            output.write(ByteArray(10) { it.toByte() })
            output.close()
        }, 100, TimeUnit.MILLISECONDS)

        assertEquals(0, input.read())
        afterRead.set(true)
        setterFuture.get() // check it didn't throw

        for (i in 1..9) {
            assertEquals(i, input.read())
        }
        assertEquals(-1, input.read())
    }

    @Test
    fun readByteArrayBlocks() {
        val afterRead = AtomicBoolean(false)
        val (input, output) = ByteArrayQueue.makePair()
        val target = ByteArray(20)

        val setterFuture = executor.schedule({
            assertFalse(afterRead.get())
            output.write(ByteArray(10) { it.toByte() })
            output.close()
        }, 100, TimeUnit.MILLISECONDS)

        assertEquals(10, input.read(target, 0, 20))
        afterRead.set(true)
        setterFuture.get() // check it didn't throw

        assert(target.contentEquals(ByteArray(20) { if (it < 10) it.toByte() else 0 }))
        assertEquals(-1, input.read())
    }

    @Test
    fun readByteMultiple() {
        val (input, output) = ByteArrayQueue.makePair()
        assertEquals(0, input.available())
        output.write(ByteArray(2) { ((it + 1) * 10).toByte() })
        assertEquals(2, input.available())
        output.write(ByteArray(2) { ((it + 1) * 40).toByte() })
        assertEquals(4, input.available())
        output.close()
        assertEquals(4, input.available())
        assertEquals(10, input.read())
        assertEquals(3, input.available())
        assertEquals(20, input.read())
        assertEquals(2, input.available())
        assertEquals(40, input.read())
        assertEquals(1, input.available())
        assertEquals(80, input.read())
        assertEquals(0, input.available())
        assertEquals(-1, input.read())
        assertEquals(0, input.available())
        assertEquals(-1, input.read())
        assertEquals(0, input.available())
    }

    @Test
    fun readByteArrayMultiple() {
        val (input, output) = ByteArrayQueue.makePair()
        val target = ByteArray(20)

        assertEquals(0, input.available())
        output.write(ByteArray(2) { ((it + 1) * 10).toByte() })
        assertEquals(2, input.available())
        output.write(ByteArray(2) { ((it + 1) * 40).toByte() })
        assertEquals(4, input.available())
        output.close()
        assertEquals(4, input.available())
        assertEquals(2, input.read(target, 0, 20))
        assertEquals(2, input.available())
        assertEquals(2, input.read(target, 2, 18))
        assertEquals(0, input.available())
        assertEquals(-1, input.read(target, 4, 18))
        assertEquals(0, input.available())

        val expected = ByteArray(20)
        expected[0] = 10
        expected[1] = 20
        expected[2] = 40
        expected[3] = 80

        assert(target.contentEquals(expected))
    }
}
