package com.redali.examples.shell;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

// If your commands are simple, then simple unit tests can be used
class TimeCommandsTest {

    @Test
    void currentTime() {
        var instance = new TimeCommands();
        var before = Instant.now();
        var currentTime = instance.currentTime();
        var after = Instant.now();

        var got = Instant.parse(currentTime);
        assertTrue(before.equals(got) || before.isBefore(got));
        assertTrue(after.equals(got) || after.isAfter(got));
    }

    @Test
    void elapsedTime() {
        var instance = new TimeCommands();
        var gotStr = instance.elapsedTime();
        double got = Double.parseDouble(gotStr.substring(0, gotStr.indexOf(' ')));
        assertTrue(got >= 0.0);
        gotStr = instance.elapsedTime();
        got = Double.parseDouble(gotStr.substring(0, gotStr.indexOf(' ')));
        assertTrue(got >= 0.0);
    }
}