package me.moontimer.smpcore.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DurationParserTest {

    @Test
    void parsesCompoundDuration() {
        assertEquals(93600, DurationParser.parseSeconds("1d2h"));
        assertEquals(93600 + 1800, DurationParser.parseSeconds("1d2h30m"));
    }

    @Test
    void parsesSingleUnit() {
        assertEquals(60, DurationParser.parseSeconds("1m"));
        assertEquals(3600, DurationParser.parseSeconds("1h"));
    }

    @Test
    void rejectsInvalid() {
        assertEquals(-1, DurationParser.parseSeconds(""));
        assertEquals(-1, DurationParser.parseSeconds("abc"));
        assertEquals(-1, DurationParser.parseSeconds("1x"));
        assertEquals(-1, DurationParser.parseSeconds("10"));
    }
}

