package com.redali.examples.shell;

import org.jline.terminal.Terminal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.shell.command.CommandRegistration;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

// Adding command annotation to class allows us to create sub-commands
@Command(command = "system", alias = {"sys"}, description = "Functions from java.util.System")
public class SystemCommands {
    private static final Logger log = LoggerFactory.getLogger(SystemCommands.class);
    private static final String NL = System.lineSeparator();
    private static final String KEY_NAMES = "KEY_NAMES";
    private final Terminal terminal;

    /**
     * Escapes a Linux shell argument to prevent users from running arbitrary commands.
     *
     * @param argument Argument to escape.
     * @return Escaped argument by doubling every single quote character in original value (if any) and then
     * enclosing the entire argument in single quotes. For example, "date" becomes "'date'" and "/home';date'"
     * becomes "'/home'';date'''".
     */
    private static String escapeSpecialCharacters(String argument) {
        // Wrap argument in single quotes after protecting/escaping any single quotes in argument
        // NOT INTENDED FOR PRODUCTION: Minimal testing done, not certain if this is a secure solution
        return "'" + argument.replace("'", "'\"'\"'") + "'";
    }

    // Spring will pass us the necessary terminal instance when constructing
    public SystemCommands(Terminal terminal) {
        this.terminal = terminal;
    }

    @Command(command = "property", alias = {"prop", "properties"}, description = "Shows system property values")
    public void property(@Option(arity = CommandRegistration.OptionArity.ZERO_OR_MORE, label = KEY_NAMES,
            description = "Shows system property value(s) for each key specified or all keys if none specified")
                             List<String> keys) {
        // Example of using the Writer from the injected Terminal object to display output to the
        // terminal. This allows us to flush output as the command runs, which really isn't needed in this
        // example, but can be useful if you have a command that takes a long time to finish, and
        // you want the user to see something as progress is being made
        var out = terminal.writer();
        if (keys == null || keys.isEmpty()) {
            keys = new ArrayList<>();
            for (var key : System.getProperties().keySet()) {
                keys.add(key.toString());
            }
            keys = keys.stream().sorted().collect(Collectors.toList());
        }
        for (var key : keys) {
            out.print(key);
            out.print('=');
            out.println(System.getProperty(key));
            out.flush();
        }
    }

    @Command(command = "environment", alias = {"env"}, description = "Shows environment variable values")
    public String env(@Option(arity = CommandRegistration.OptionArity.ZERO_OR_MORE, label = KEY_NAMES,
            description = "List environment variable names (like PATH) to show value of, omit to list keys")
                      List<String> keys) {
        // Example of using StringBuilder to build up output and then return a ginormous string for output
        // at end
        var out = new StringBuilder(1024);
        if (keys == null || keys.isEmpty()) {
            keys = System.getenv().keySet().stream().sorted().toList();
        }
        for (var key : keys) {
            out.append(key);
            out.append('=');
            out.append(System.getenv(key));
            out.append(NL);
        }
        return out.toString();
    }

    @Command(command = "ls", alias = {"dir", "ls"}, description = "List files in directories")
    public int ls(@Option(longNames = {"dirs"}, shortNames = { 'd' },
            arity = CommandRegistration.OptionArity.ZERO_OR_MORE, // "--dirs" without specifying directories is OK
            description = "The directories to show the contents (omit for current directory)") String[] dirs,
                  @Option(longNames = {"long"}, shortNames = { 'l' }, arity = CommandRegistration.OptionArity.ZERO,
                          description = "Show long listing format") boolean verbose)
            throws IOException, InterruptedException {

        var args = new ArrayList<String>();
        args.add("ls");
        if (verbose) {
            args.add("-l");
        }
        // null check required, as we get null instead of an empty list if "--dirs" is omitted
        if (dirs != null) {
            Collections.addAll(args, dirs);
        }
        // Returning int here will cause the spring shell interpreter to display the exit code from the output
        return execShellCmd(args);
    }

    @Command(command = "pinfo", alias = { "pinfo" }, description = "Prints process information on current process")
    public void pinfo() throws IOException, InterruptedException {
        var phandle = ProcessHandle.current();
        var pinfo = phandle.info();
        terminal.writer().println("""
                PID:     %d
                User:    %s
                Started: %s
                CPU:     %s
                Command: %s
                """.formatted(phandle.pid(),
                pinfo.user().orElse(null),
                pinfo.totalCpuDuration().orElse(null),
                pinfo.commandLine().orElse(null),
                pinfo.startInstant().orElse(null)));

        // Let's see how the Linux ps -f PID output compares
        var cmd = new ArrayList<String>();
        cmd.add("ps");
        cmd.add("-f");
        cmd.add(String.valueOf(phandle.pid()));
        exec(cmd);
    }

    /**
     * Helper to run an arbitrary Linux "sh" command.
     *
     * @param shellArgs Args to run, we will attempt to escape to prevent people from getting away with something
     *                  like: { "ls", "/home", ";", "ps" }.
     * @return Exit code from running Linux command
     */
    private int execShellCmd(List<String> shellArgs) throws IOException, InterruptedException {
        var execArgs = new ArrayList<String>(2 + shellArgs.size());
        var osName = System.getProperty("os.name");
        if (!"Linux".equals(osName)) {
            terminal.writer().println("Shell commands only sanitized for Linux, not supported in " + osName);
            return 1;
        }
        execArgs.add("sh");
        execArgs.add("-c");
        execArgs.add(escapeShellCommand(shellArgs));
        return exec(execArgs);
    }

    /**
     * Runs a Linux command and write stderr and stdout to terminal output.
     *
     * @param execCmd Command to run on system like: { "/usr/bin/ps", "-f" } - it is up to you to
     *                make sure the arguments are "safe and secure".
     * @return Exit code from running Linux command
     */
    private int exec(List<String> execCmd) throws IOException, InterruptedException {
        var builder = new ProcessBuilder(execCmd);
        builder.redirectErrorStream(true);
        var process = builder.start();
        var out = terminal.writer();
        log.debug("Invoking: {}", execCmd);

        try (InputStream in = process.getInputStream()) {
            var reader = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = reader.readLine()) != null) {
                out.println(line);
                out.flush();
            }
        }

        int exitCode = process.waitFor();
        log.debug("Exit code of {} from running: {}", exitCode, execCmd);
        return exitCode;
    }

    /**
     * Primitive Linux shell argument escaping.
     *
     * @param shellCmd List of zero or more shell arguments that you want escaped.
     * @return A string where escaped arguments are joined together with a single separating space.
     */
    private String escapeShellCommand(List<String> shellCmd) {
        var cmd = new StringBuilder();
        shellCmd.forEach(arg -> {
            if (!cmd.isEmpty()) {
                cmd.append(' ');
            }
            var escaped = escapeSpecialCharacters(arg);
            log.trace("Escaped: \"{}\" into \"{}\"", arg, escaped);
            cmd.append(escaped);
        });
        return cmd.toString();
    }
}
