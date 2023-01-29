# General

This example focuses on common developer tasks related to
logging in a spring-boot application. My intention is to use it as a personal
reference for the following:

* Dependencies required in the pom.xml
* Configuration of log levels at run time
* Ability to adjust log levels on the fly via /actuator/loggers endpoints
* How to trap and verify log messages in unit tests

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

* Delete or refactor [SwaggerConfig.java](src/main/java/com/redali/example/SwaggerConfig.java))
* Delete or refactor [ExampleController.java](src/main/java/com/redali/example/controllers/ExampleController.java))
and [ExampleTests.java](src/test/java/com/redali/example/ExampleTests.java)).
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
the /actuator/loggers endpoint. The following shows how to do this when

```shell
curl  -i -X POST -H 'Content-Type: application/json' -d '{"configuredLevel":"DEBUG"}' \
  http://localhost:8888/actuator/loggers/com.redali.example.controllers.ExampleController
```

The swagger-ui should be available unless you've changed the default settings.
This UI should let you view and try the various endpoints available in the
service. It should be limited to localhost and available at:

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
docker containers of your Java application without doing much.

### Build docker container image

```shell
mvn clean
mvn spring-boot:build-image
```

To check that this initial image will run the service:

```shell
docker run --name logging-example --rm -p 8080:8080 -p 8888:8888 spring-boot-logging-example:0.0.1-SNAPSHOT
```

```shell
[pabla@tamale spring-boot-logging]$ docker image ls | grep spring-boot-logging-example
spring-boot-logging-example                          0.0.1-SNAPSHOT                                 46334fcdb1cf   43 years ago    286MB
[pabla@tamale spring-boot-logging]$ 
```

# Kubernetes

## Build docker container image

```shell
mvn spring-boot:build-image
```

To check that this initial image will run the service:

```shell
docker run --rm --name logging-example -p 8080:8080 -p 8888:8888 spring-boot-logging-example:0.0.1-SNAPSHOT
```

# Kubernetes (microk8s)

If you don't have a registry set up, you can save the docker container and then
import it into microk8s via:

```shell
docker image save spring-boot-logging-example:0.0.1-SNAPSHOT | microk8s ctr image import -
```

You can then generate a template deployment and service yaml file for kubernetes using
the following commands (skip the namespace if you already have an "examples" namespace):

```shell
kubectl create namespace examples -o=yaml --dry-run=client >| target/k8s.yaml
echo ---  >> target/k8s.yaml
kubectl create deployment spring-boot-logging-example --namespace examples --image docker.io/library/spring-boot-logging-example:0.0.1-SNAPSHOT -o=yaml --dry-run=client >> target/k8s.yaml
echo --- >> target/k8s.yaml
kubectl create service clusterip spring-boot-logging-example --namespace examples --tcp 8080:8080 --tcp 8888:8888 -o=yaml --dry-run=client >> target/k8s.yaml
```

To apply these yaml files to your Kubernetes cluster:

```shell
kubectl apply -f target/k8s.yaml
```

To expose the service ports from Kubernetes:

```shell
kubectl port-forward --namespace examples deployment/spring-boot-logging-example 8080:8080 8888:8888
```

You should then be able to curl the health of the service on the exposed port:

```shell
[pabla@tamale ~]$ curl http://localhost:8080/actuator/health; echo
{"status":"UP"}
[pabla@tamale ~]$
```

To remove from your cluster:

```shell
kubectl delete -f target/k8s.yaml
```
