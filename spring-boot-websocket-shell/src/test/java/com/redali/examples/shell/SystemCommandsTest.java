package com.redali.examples.shell;

import org.jline.terminal.impl.DumbTerminal;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

//
// Example of writing a unit test for shell commands that write to the output
// terminal by creating a "mock" Terminal that allows us to inspect the output
// of each command invoked.
//
class SystemCommandsTest {
    @Test
    void propertyNull() throws IOException {
        TerminalStringBuffer terminal = TerminalStringBuffer.create();
        var instance = new SystemCommands(terminal);

        instance.property(null);
        var got = terminal.clear();
        assertTrue(got.contains("os.name=" + System.getProperty("os.name")));
        assertTrue(got.contains("user.name=" + System.getProperty("user.name")));
        got = terminal.clear();
        assertEquals(0, got.length());
    }

    @Test
    void propertyEmpty() throws IOException {
        TerminalStringBuffer terminal = TerminalStringBuffer.create();
        var instance = new SystemCommands(terminal);

        instance.property(Collections.emptyList());
        var got = terminal.clear();
        assertTrue(got.contains("os.name=" + System.getProperty("os.name")));
        assertTrue(got.contains("user.name=" + System.getProperty("user.name")));
    }

    @Test
    void env() throws IOException {
        TerminalStringBuffer terminal = TerminalStringBuffer.create();
        var instance = new SystemCommands(terminal);

        // The env command doesn't use the terminal and returns output as a String instead
        var got = instance.env(null);
        assertFalse(got.isEmpty());
    }

    //
    // Rest of tests are not fully implemented - just checking that we can invoke them
    //

    @Test
    void ls() throws IOException, InterruptedException {
        TerminalStringBuffer terminal = TerminalStringBuffer.create();
        var instance = new SystemCommands(terminal);

        int exitCode = instance.ls(null, true);
        var got = terminal.clear();
        assertFalse(got.isEmpty());
        assertTrue(exitCode >= 0);
    }

    @Test
    void pinfo() throws IOException, InterruptedException {
        TerminalStringBuffer terminal = TerminalStringBuffer.create();
        var instance = new SystemCommands(terminal);

        instance.pinfo();
        var got = terminal.clear();
        assertFalse(got.isEmpty());
    }

    static class TerminalStringBuffer extends DumbTerminal {
        private final ByteArrayOutputStream out;

        private TerminalStringBuffer(InputStream in, ByteArrayOutputStream out) throws IOException {
            super(in, out);
            this.out = out;
        }

        public static TerminalStringBuffer create() throws IOException {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ByteArrayInputStream in = new ByteArrayInputStream(new byte[0]);
            return new TerminalStringBuffer(in, out);
        }

        public String clear() {
            var contents = out.toString(StandardCharsets.UTF_8);
            out.reset();
            return contents;
        }
    }

}