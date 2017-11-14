package tech.macil.util;

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
        assertEquals(r.readLine(), "foo");
        assertEquals(r.readLine(), "");
        assertEquals(r.readLine(), "bar x x");
        assertEquals(r.readLine(), null);
    }

    @Test
    public void lines() throws Exception {
        NLRequiringBufferedReader r = new NLRequiringBufferedReader(new StringReader(
                "foo\n\nbar x x\nbaz"));
        assertEquals(
                r.lines().collect(Collectors.toList()),
                Lists.newArrayList("foo", "", "bar x x"));
    }

}