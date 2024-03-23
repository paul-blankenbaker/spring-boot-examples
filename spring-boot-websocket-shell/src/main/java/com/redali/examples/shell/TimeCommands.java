package com.redali.examples.shell;


import org.springframework.shell.command.annotation.Command;

import java.time.Instant;

@Command
public class TimeCommands {
    private Instant lastTimeStamp = Instant.now();

    @Command(command = "timestamp", description = "Shows the current time")
    public String currentTime() {
        lastTimeStamp = Instant.now();
        return lastTimeStamp.toString();
    }

    @Command(command = "elapsed", description = "Shows duration in seconds since last invoked")
    public String elapsedTime() {
        var now = Instant.now();
        double seconds = (now.toEpochMilli() - lastTimeStamp.toEpochMilli()) / 1000.0;
        var output = String.format("%.3f seconds from %s to %s".formatted(seconds, lastTimeStamp, now));
        lastTimeStamp = now;
        return output;
    }
}
