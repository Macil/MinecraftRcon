package tech.macil.minecraft.rcon.util;

import com.google.common.collect.Lists;
import org.junit.Test;

import java.io.StringReader;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class NLRequiringBufferedReaderTest {
    @Test
    public void readLine() throws Exception {
        NLRequiringBufferedReader r = new NLRequiringBufferedReader(new StringReader(
                "foo\n\nbar x x\nbaz"));
        assertEquals("foo", r.readLine());
        assertEquals("", r.readLine());
        assertEquals("bar x x", r.readLine());
        assertEquals(null, r.readLine());
    }

    @Test
    public void lines() throws Exception {
        NLRequiringBufferedReader r = new NLRequiringBufferedReader(new StringReader(
                "foo\n\nbar x x\nbaz"));
        assertEquals(
                Lists.newArrayList("foo", "", "bar x x"),
                r.lines().collect(Collectors.toList())
        );
    }
}