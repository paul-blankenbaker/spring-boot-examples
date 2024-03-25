# Main Objectives

* Demonstrate the basics of "framing" a Spring Shell project that can
be use interactively or as a command line interpreter.
* Demonstrate how to create a WebSocket client than can interact with the
[spring-boot-websocket-raw](../spring-boot-websocket-raw/README.md) server.

## Spring Shell Examples

* How to set the Spring Shell version to match the pom.xml version using
resource filtering on the standard
[template/version-default.st](src/main/resources/template/version-default.st) file.
* Settings in the [application.properties](src/main/resources/application.properties)
that control the behavior of things like banner output, log messages, interactive, ...
* How to run a Spring Shell test without hanging the build.
* An example of running a Linux shell command from Spring Shell (not necessarily a good practice).
* An example of writing a unit test that captures command output to the terminal for inspection.

### Reference Documentation
For further reference, please consider the following sections:

* [Official Apache Maven documentation](https://maven.apache.org/guides/index.html)
* [Spring Boot Maven Plugin Reference Guide](https://docs.spring.io/spring-boot/docs/3.1.5/maven-plugin/reference/html/)
* [Create an OCI image](https://docs.spring.io/spring-boot/docs/3.1.5/maven-plugin/reference/html/#build-image)
* [Spring Shell](https://spring.io/projects/spring-shell)
