package com.redali.example.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Example REST controller and startup command to testing the /actuator/logging controls.
 *
 * <p>This controller only provides test methods related to dynamic logging controls, you DO
 * not need it in a normal Spring Boot application. Everything required to enable dynamic logging controls is set in the
 * application.properties configuration file.</p>
 *
 * <p>This controller and command line runner demonstrate the following:</p>
 *
 * <ul>
 *     <li>Using the @Slf4j annotation from lombok so that you can then use log.info(), log.debug(), ... in your class.
 *     If you are avoiding lombok, you will need to declare your log instance by hand via:
 *     <pre><code>private static Logger log = LoggerFactory.getLogger(LoggingExampleController.class)</code></pre></li>
 *     <li>A single REST endpoint /log/test that uses an optional msg=TEXT parameter for logging messages. Use like:
 *     <pre>curl <a href="http://localhost:8080/log/test">http://localhost:8080/log/test</a>
 *     curl <a href="http://localhost:8080/log/test?msg=Hello+World">http://localhost:8080/log/test?msg=Hello+World</a></pre>
 *     The output returned provides information about and adjusting log levels using the Spring Boot /actuator/loggers endpoint.</li>
 *     <li>A CommandLineRunner implementation (which isn't typical on a Controller) that logs a message to the server
 *     console showing the initial curl request to use to get the example output.</li>
 * </ul>
 */
@Slf4j // From lombok - provides logger "log" in class (several logging options available
@RestController // Let Spring Boot know that this class provides REST handlers
@RequestMapping("${api.url}/log/") // Root path of REST handlers provided by this class
// This is optional, it allows the swagger-ui (or any ui) to actually make use of the API from a different localhost port.
@CrossOrigin(origins = "http://localhost:${management.server.port:8080}")
@Tag(name = "Logging Test", description = """
        This REST controller provides a service to test the effects of dynamically adjusting
        log levels using the /actuator/loggers endpoint built into Spring Boot.
        """)
public class ExampleController implements CommandLineRunner {
    // Example of creating the log instance by hand (if not using @Slf4j annotation on class)
    // private static Logger log = LoggerFactory.getLogger(LoggingExampleController.class)

    // This is just for the initial startup log message (see run() method below)
    @Value("http://${server.address:localhost}:${server.port:8080}${api.url}/")
    private String baseUrl;

    @Value("http://${management.server.address:localhost}:${management.server.port:8080}${management.endpoints.web.base-path:/actuator}/")
    private String managementUrl;

    /**
     * Test end point (/log/test[?msg=LOG_TEXT]) that logs a message at all log levels.
     *
     * @param msg Message to log at all levels (defaults to "Test log message" if parameter not set in URL).
     * @return Text output describing log state and showing some example curl commands to adjust.
     */
    @GetMapping(value = "test", produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseStatus(code = HttpStatus.OK)
    @Operation(summary = "Log message at all levels",
            description = "Logs a message at all levels and returns an informational text message describing how to adjust the log levels")
    public String test(
            @RequestParam(name = "msg", defaultValue = "Test log message from /log/test endpoint")
            @Parameter(description = "Optional text message that you would like to appear in the log file")
            String msg) {
        int cnt = 0;
        // For performance (and possibly a bit of security), prefer field substitution over string concatenation
        String messageFormat = "{}: {}";
        log.trace(messageFormat, cnt++, msg);
        log.debug(messageFormat, cnt++, msg);
        log.info(messageFormat, cnt++, msg);
        log.warn(messageFormat, cnt++, msg);
        log.error(messageFormat, cnt, msg);

        String actuatorBase = managementUrl + "loggers";
        String actuatorUrl = actuatorBase + "/" + log.getName();
        String newLevel = log.isDebugEnabled() ? "INFO" : "DEBUG";

        // Return some text showing current level of this specific logger and how to fetch/adjust
        final String newCurlLine = "\n  curl ";
        return "Logger name: " + log.getName()
                + "  error:" + log.isErrorEnabled()
                + "  warn:" + log.isWarnEnabled()
                + "  info:" + log.isInfoEnabled()
                + "  debug:" + log.isDebugEnabled()
                + "  trace:" + log.isTraceEnabled()
                + "\nGet and set levels using curl, some examples:"
                + newCurlLine + actuatorBase + " | jq # all logging levels"
                + newCurlLine + actuatorBase + "/ROOT; echo # root logging level"
                + newCurlLine + actuatorBase + "/com.redali; echo # package level"
                + newCurlLine + actuatorUrl + "; echo # class level"
                + newCurlLine + " -i -X POST -H 'Content-Type: application/json' -d '{\"configuredLevel\":\""
                + newLevel + "\"}' " + actuatorUrl
                + "\n";
    }

    /**
     * Example of doing something once the Spring Boot application is ready to rock and roll.
     *
     * <p>In this example implementation, we will just log a message to the console showing the user the curl
     * command to run to hit our test end point.</p>
     *
     * @param args - ignored.
     */
    @Override
    public void run(String... args) {
        log.info("For testing info, try: curl " + baseUrl + "log/test");
        log.info("For swagger-ui,  try: curl " + managementUrl + "swagger-ui");
        log.info("For openapi JSON try: curl " + managementUrl + "openapi/springdocDefault | jq");
    }
}