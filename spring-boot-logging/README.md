# General

This example focuses on common developer tasks related to
logging in a spring-boot application. My intention is to use it as a personal
reference for the following:

* Dependencies required in the pom.xml
* Configuration of log levels at run time
* Ability to adjust log levels on the fly via /actuator/loggers endpoints
* How to trap and verify log messages in unit tests

In addition, a helper script [spring-boot-log-level.sh](src/scripts/spring-boot-log-level.sh)
has been provided that can be used to get and set the log levels (it was a
pain for me to remember the curl commands). After building, you should be able
to copy the script to /usr/local/bin (or somewhere in your PATH) on a Linux
system and use it via:

```shell
spring-boot-log-level.sh --get --port ${PORT} -c com.redali.example.controllers.ExampleController 
```

The script is capable of setting up kubectl port forwarding if your service is
running inside of Kubernetes and can be used like:

```shell
spring-boot-log-level.sh --namespace demo --deployment spring-boot-logging-example --port 8888 --get -c com.redali.example
```

And you can then create aliases or other short scripts to leverage it. For
example:

```shell
alias demo-log-level='spring-boot-log-level.sh --namespace demo --port ${PORT} --deployment'
```

Which could then be used to manage any service running in the demo namespace
that uses port ${PORT} for actuator access with commands like:

```shell
demo-log-level spring-boot-logging-example --set -c com.redali.example.controllers.ExampleController -l debug
demo-log-level spring-boot-logging-example --get -c com.redali.example.controllers.ExampleController
```

Well, maybe that isn't all that much easier.

# Bonus

As a side bonus (and in order to actually perform some testing):

* How to set up a minimal REST endpoint
* How to hit the REST endpoint in a minimal unit test
* How to document REST end points using the OpenAPI annotations
* How to enable/disable the swagger-ui in the project

There are many comments in this small code base. And this might be my default
"starter" goto as it has a lot of the base configuration and setup that's a
pain to track down.

To use as a starting point:

* Delete or refactor [SwaggerConfig.java](src/main/java/com/redali/example/SwaggerConfig.java)
* Delete or refactor [ExampleController.java](src/main/java/com/redali/example/controllers/ExampleController.java)
and [ExampleTests.java](src/test/java/com/redali/example/ExampleTests.java).
* Make sure application still builds (mvn install).
* Make sure application still runs (java -jar target/*.jar)
* Make sure you can still access the actuator endpoints (curl http://localhost:8888/actuator/loggers).

# Build/Run

## As Normal Java Application

You need Maven and a Java 17 environment in order to run this.

Package into jar:

```shell
mvn package
```

Run service:

```shell
java -jar target/*.jar
```

## Exercising

Once you have the application running, you can hit the test endpoint via:

```shell
curl http://localhost:8080/logging-example/api/v1/log/test; echo
```

It will try to log a message 5 times at 5 different log levels. You will see
3 of them come out in the console until you change the log level settings.
It will echo back some example curl commands for showing and tweaking the
logging levels.

You should be able to adjust the log levels by POSTING a new level to
the /actuator/loggers endpoint for the package or class to apply the
adjustment to.

```shell
curl  -i -X POST -H 'Content-Type: application/json' -d '{"configuredLevel":"DEBUG"}' \
  http://localhost:8888/actuator/loggers/com.redali.example.controllers.ExampleController
```

The swagger-ui should be available unless you've changed the default settings.
This UI should let you view and try the various endpoints available in the
service.

http://localhost:8888/actuator/swagger-ui

You can verify service is up and healthy via:

```shell
curl http://localhost:8888/actuator/health; echo
```

You can get ALL the logging levels via:

```shell
curl http://localhost:8888/actuator/loggers | jq
```

You can get the root level logging level:

```shell
curl http://localhost:8888/actuator/loggers/ROOT; echo
```

You can get the logging level of the "com.redali" (or any other class/package)
via:

```shell
curl http://localhost:8888/actuator/loggers/com.redali; echo
```

## Docker

One of the nice Spring Boot bonuses is that you have the ability to build
docker containers of your Java application without doing much. You don't
even need to create a Docker file.

### Build docker container image

```shell
mvn clean
mvn spring-boot:build-image
```

To check that this initial image will run the service:

```shell
docker run --name logging-example --rm -p 8080:8080 -p 8888:8888 spring-boot-logging-example:0.0.1-SNAPSHOT
```

The endpoints described in the previous sections should still work when
running as a docker container. Try the [Swagger UI](http://localhost:8888/actuator/swagger-ui)
and some curl commands.

```shell
curl http://localhost:8080/logging-example/api/v1/log/test; echo
curl http://localhost:8888/actuator/health; echo
```

# Kubernetes (microk8s)

This section describes how to quickly check that your container will run
inside Kurbenetes. The commands shown assume that you have microk8s installed
as your Kubernetes implementation. That being said, other than the importing
of the container image, the commands shown below use kubectl and should be
compatible with other Kubernetes implementations.

If you haven't done so yet, you will need to build the container image:

```shell
mvn clean
mvn spring-boot:build-image
```

The container image built will likely be available only locally to your docker
service. There are several mechanisms to get it into Kubernetes and these can
be a bit of a challenge to set up. For development and testing purposes, you
can export the container image from docker and import it into microk8s using
the following command:

```shell
docker image save spring-boot-logging-example:0.0.1-SNAPSHOT | microk8s ctr image import -
```

You can then generate a template deployment and service yaml file for kubernetes
using the following commands (skip the namespace if you already have an
"demo" namespace):

```shell
kubectl create namespace demo -o=yaml --dry-run=client >| target/k8s.yaml
echo ---  >> target/k8s.yaml
kubectl create deployment spring-boot-logging --namespace demo --image docker.io/library/spring-boot-logging-example:0.0.1-SNAPSHOT -o=yaml --dry-run=client >> target/k8s.yaml
echo --- >> target/k8s.yaml
kubectl create service clusterip spring-boot-logging-example --namespace demo --tcp 8080:8080 --tcp 8888:8888 -o=yaml --dry-run=client >> target/k8s.yaml
```

To apply these yaml files to your Kubernetes cluster:

```shell
kubectl apply -f target/k8s.yaml
```

At this point, you should be able to verify that the bash script can be used
to query and adjust log levels on the fly. An alias has been used reduce some
typing and the output lines are prefixed with "# Output:" to simplify
copy/pasting into a terminal window.

```shell
alias demo-logging-level='./src/scripts/spring-boot-log-level.sh --namespace demo --deployment spring-boot-logging --port 8888'
demo-logging-level --pkg com.redali.example.controllers --get

# Ouptut: {"configuredLevel":"INFO","effectiveLevel":"INFO"}

demo-logging-level --pkg com.redali.example.controllers --set --level TRACE

# Ouptut: HTTP/1.1 204
# Ouptut: Date: Mon, 20 Feb 2023 12:49:05 GMT

demo-logging-level --pkg com.redali.example.controllers --get

# Ouptut: {"configuredLevel":"TRACE","effectiveLevel":"TRACE"}
```

Instead of using the helper script, you can use the following command to
gain access to the REST endpoints and management port directly:

```shell
kubectl port-forward --namespace demo deployment/spring-boot-logging 8080:8080 8888:8888
```

You should then be able to hit the REST endpoint:

```shell
curl http://localhost:8080/logging-example/api/v1/log/test; echo
```

You should also have access to the actuator and [Swagger UI](http://localhost:8888/actuator/swagger-ui) endpoints under port
8888:

```shell
curl http://localhost:8888/actuator/health; echo

# Output: {"status":"UP"}
```

To remove from the deployment from your Kubernetes cluster:

```shell
kubectl delete -f target/k8s.yaml
```
