package com.redali.example;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Swagger and example of doing something once the application is ready.
 *
 * <p>This demonstrates the following:</p>
 *
 * <ul>
 *  <li>How to do something conditionally. In this case we only load this if the management port has been moved
 *  to a different value than the main server port.</li>
 *  <li>How to tweak a few of the OpenAPI settings to make the swagger-ui look a bit nicer.</li>
 *  <li>How to implement a CommandLineRunner so you can do something once Spring Boot has loaded (in this case we
 *  just log a messages).</li>
 * </ul>
 *
 * <p>NOTE: This entire file can be removed, it doesn't provide anything required for the application to run.</p>
 */
@Slf4j
@Configuration
@ConditionalOnExpression("'${springdoc.swagger-ui.enabled:false}'.equals('true')")
public class SwaggerConfig implements CommandLineRunner {

    @Value("http://${management.server.address:localhost}:${management.server.port:8080}${management.endpoints.web.base-path:/actuator}/")
    private String actuatorUrl;

    /**
     * Example of customizing the meta-data used in the Swagger OpenAPI output.
     *
     * <p>This is an optional ben that allows us to customize some of the static meta data
     * used in the swagger/OpenAPI output.</p>
     *
     * <p>NOTE: This bean doesn't really belong in a controller class.</p>
     *
     * @param appName        - Name from pom.xml.
     * @param appDescription - Description from pom.xml.
     * @param appVersion     - Version from pom.xml.
     * @param licenseName    - License name from pom.xml.
     * @param licenseUrl     - License URL from pom.xml.
     * @return Customized OpenAPI meta data.
     */
    @Bean
    public OpenAPI customOpenAPI(
            @Value("${app.name}") String appName,
            @Value("${app.description}") String appDescription, @Value("${app.version}") String appVersion,
            @Value("${app.license.name}") String licenseName, @Value("${app.license.url}") String licenseUrl) {

        return new OpenAPI().info(new Info().title(appName).version(appVersion).description(appDescription)
                .license(new License().name(licenseName).url(licenseUrl)));
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
        log.info("For swagger-ui,  try: curl " + actuatorUrl + "swagger-ui");
        log.info("For openapi JSON try: curl " + actuatorUrl + "openapi/springdocDefault | jq");
    }
}
