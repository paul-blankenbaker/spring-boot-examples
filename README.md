# spring-boot-examples

Collection of minimal spring-boot applications to use as notes on setting
up and getting started with using spring-boot as a starting point for
applications and services.

The top level mvnw and mvnw.cmd Maven stubs should be usable to build all
subprojects from the top level, or individual projects. For example, to
build just spring-boot-logging:

```sh
cd spring-boot-logging
../mvnw install
```

## [spring-boot-logging](spring-boot-logging/README.md)

* Configuring actuators so logging levels can be dynamically adjusted at
runtime.
* Examples of unit testing for logging statements.
* Simple REST controller.
* Annotations to REST controller and configuring for OpenAPI and Swagger UI.

## [spring-boot-native](spring-boot-native/README.md)

* Creating a spring-boot project that is compiled into a native executable
that does not require a JVM to execute.
* How convert a spring-boot JAR file or native executable into a container
image.
* How to run the container image in docker.
* Minimal steps to deploy container image into Kubernetes.

## [spring-boot-websocket-raw](spring-boot-websocket-raw/README.md)

* Creating a spring-boot project that accepts raw (text message) based
clients instead of STOMP clients.
* Includes example [Python WebSocket Client](spring-boot-web-socket-raw/src/examples/python/websocket-client.py) 
* Includes example [JavaScript WebSocket Client](spring-boot-web-socket-raw/src/examples/javascript/websocket-client.js) 
