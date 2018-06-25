package tech.macil.minecraft.rcon.util

import com.google.common.collect.Lists
import org.junit.Test

import java.io.StringReader
import java.util.stream.Collectors

import org.junit.Assert.*

class NLRequiringBufferedReaderTest {
    @Test
    fun readLine() {
        val r = NLRequiringBufferedReader(StringReader(
                "foo\n\nbar x x\nbaz"))
        assertEquals("foo", r.readLine())
        assertEquals("", r.readLine())
        assertEquals("bar x x", r.readLine())
        assertNull(r.readLine())
    }

    @Test
    fun lines() {
        val r = NLRequiringBufferedReader(StringReader(
                "foo\n\nbar x x\nbaz"))
        assertEquals(
                Lists.newArrayList("foo", "", "bar x x"),
                r.lines().collect(Collectors.toList())
        )
    }
}