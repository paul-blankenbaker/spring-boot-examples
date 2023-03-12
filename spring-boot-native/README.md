# General

Minimal example of a Spring Boot Java application with the steps necessary
to produce a docker image, a native image and a container image that runs
within Kubernetes.

* Uses new native support in Spring Boot 3.0.1
* Steps and boilerplate to build and run service as Java JAR, Docker container
native executable or Kubernetes deployment.
* Docker needs to be set up if you want to build docker images
* The GRAALVM_HOME environment variable needs to be set to your local GraalVM 
installation if you want to build a native executable (search for GraalVM 
installation if you don't have it installed).

The command line examples shown below were run on the following development
machine:

* AlmaLinux 8 was the base OS
* The standard OpenJDK 17 and maven installation available on AlmaLinux
* A local Java 17 compatible [GraalVM](https://www.graalvm.org/) installation
  installed at ~/.local/graalvm-ce-java17-22.3.0
* To build and run the native executable in docker containers, docker-ce was
installed using the Docker RPM repository
* To run the container images in Kubernetes, microk8s was installed using snap.

# Normal Build/Run

Package into jar:

```shell
mvn package
```

Run service:

```shell
java -jar target/spring-boot-native-0.0.2-SNAPSHOT.jar
```

You can verify service is up and healthy via:

```shell
curl http://localhost:8080/actuator/health; echo
```

## Build "fat" docker container image

```shell
mvn clean
mvn spring-boot:build-image
```

To check that this initial image will run the service:

```shell
docker run --rm -p 8080:8080 spring-boot-native:0.0.2-SNAPSHOT
```

```shell
[tamale spring-boot-native]$ docker image ls | grep spring-boot-native
spring-boot-native         0.0.2-SNAPSHOT    a3dd76fc8785   43 years ago   277MB
[tamale spring-boot-native]$ 
```

# Native Build Instructions

Set the GRAALVM_HOME environment variable to the directory where you installed
GraalVM:

```shell
# You need to adjust this for your installation of GraalVM
export GRAALVM_HOME=${HOME}/.local/graalvm-ce-java17-22.3.0
```

Set JAVA_HOME and PATH for the GraalVM tools (this might be optional):

```shell
export JAVA_HOME="${GRAALVM_HOME}"
export PATH="${GRAALVM_HOME}/bin:${PATH}"
```

## Build native executable

To create an executable that you can run directly from the command
line, use the following command:

```shell
mvn -Pnative -DskipTests native:compile # Use "package" if Spring Boot 2.6.3
```

You can run native executable directly:

```shell
./target/spring-boot-native
```

## Build native docker container image (two steps)

You can copy the native executable into your own custom Docker image
by creating your own [Dockerfile](Dockerfile). As an example, a
[Dockerfile](Dockerfile) has been provided that runs the executable inside a
minimal AlamaLinux 8 docker image.

To build the container image:

```shell
docker build --tag spring-boot-native:custom .
```

You run an instance of this container: 

```shell
docker run --network host --name native-custom --rm -p 8080:8080 spring-boot-native:custom
```

Since this container image is based on a minimal AlmaLinux 8 image, we can
connect to it and run a shell within the native container. For example,
you can run the curl command within the container to check the health:

```shell
[pabla@tamale spring-boot-native]$ docker exec -it native-custom /bin/sh
sh-4.4# curl http://localhost:8080/actuator/health; echo
{"status":"UP"}
sh-4.4# exit
```

## Build native docker container image (one step)

Instead of building a docker container image in two steps, we can use the
"spring-boot:build-image" to build the native executable and place it directly
into a docker container (no Dockerfile required):

```shell
mvn clean
mvn -Pnative spring-boot:build-image
```

To check that this initial image will run the service:

```shell
docker run --rm -p 8080:8080 spring-boot-native:0.0.2-SNAPSHOT
```

NOTE: The native container image is much smaller and starts much quicker
than the normal Java JAR based container. The size has shrunk from 277MB
earlier down to 97MB. It is also much smaller than the custom container we
created based off AlmaLinux 8 (162MB).

```shell
[tamale spring-boot-native]$ docker image ls | grep spring-boot-native
spring-boot-native         custom            ea0b9496a621  9 minutes ago  162MB
spring-boot-native         0.0.2-SNAPSHOT    59636b02536c  43 years ago   97.1MB
[tamale spring-boot-native]$ 
```

So, this method is both more convenient and produces a very lean docker image.
This is probably what you'll want most of the time.

However, since this image does not include things like a shell or curl. You may
find that using the two-step method for creating a custom image is beneficial
when troubleshooting.

# Kubernetes (microk8s)

In order to run the new container image in Kubernetes, you will first need to
make the image available to your Kubernetes installation.

If you are using a microk8s Kubernetes environment, you can use the following to
import the docker container image into your microk8s Kubernetes environment:

```shell
docker image save spring-boot-native:0.0.2-SNAPSHOT | microk8s ctr image import -
```

You can then generate a template deployment and service yaml file for kubernetes via:

```shell
kubectl create namespace demo -o=yaml --dry-run=client >| target/spring-boot-native.yaml
echo ---  >> target/spring-boot-native.yaml
kubectl create deployment spring-boot-native --namespace demo --image docker.io/library/spring-boot-native:0.0.2-SNAPSHOT -o=yaml --dry-run=client >> target/spring-boot-native.yaml
echo --- >> target/spring-boot-native.yaml
kubectl create service clusterip course-tracker-service --namespace demo --tcp 80:8080 -o=yaml --dry-run=client >> target/spring-boot-native.yaml
```

To apply these yaml files to your Kubernetes cluster:

```shell
kubectl apply -f target/spring-boot-native.yaml
```

To expose the service port of your container from Kubernetes for a quick test:

```shell
kubectl port-forward --namespace demo deployment/spring-boot-native 8080:8080
```

You should then be able to curl the health of the service on the exposed port:

```shell
[tamale ~]$ curl http://localhost:8080/actuator/health; echo
{"status":"UP"}
[tamale ~]$
```

To remove the deployment from your Kubernetes cluster:

```shell
kubectl delete -f target/spring-boot-native.yaml
```
