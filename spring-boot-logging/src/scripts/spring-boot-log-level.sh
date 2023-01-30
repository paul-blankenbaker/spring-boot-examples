#!/bin/bash
#
#   Script to help adjust log levels on the fly for Spring-Boot applications that have
#   the /actuator/loggers endpoint enabled.
#
# MIT License (run in license mode: ./spring-boot-log-level license for full text)

declare LICENSE_TYPE="@license.name@"
declare LICENSE_URL="@license.url@"
declare COPYRIGHT_YEAR="@copyright.year@"
declare COPYRIGHT_HOLDER="@author.name@"
declare VERSION="@project.version@"
declare SCRIPT_NAME="$(basename "${0}")"

declare -i PORT=${MANAGEMENT_PORT:-8080}
declare CLASS_PATH="${CLASS_PATH:-ROOT}"
declare LEVEL="${LEVEL:-DEBUG}"
declare MODE="help"
declare LOG_LEVELS="TRACE|DEBUG|INFO|WARN|ERROR|FATAL|OFF"
declare SHOW="false"
declare K8S_NAMESPACE="${K8S_NAMESPACE}"
declare K8S_ITEM="${K8S_ITEM}"
declare K8S_SLEEP="${K8S_SLEEP:-0.5}"

show_help() {
    echo -e "Usage:

  ${SCRIPT_NAME} --help|-h|--version|--license

  ${SCRIPT_NAME} --set [--level|-l LEVEL] [--pkg PKG|CLASS|ROOT]
     [--port|-p PORT] [--show]
     [--namespace K8S_NAME --deployment K8S_NAME|--pod K8S_NAME]

  ${SCRIPT_NAME} --get [--pkg|-c PKG|CLASS|ROOT] [--port|-p PORT]
     [--show] [--namespace K8S_NS] [--deployment K8S_NAME]

Where:

  --help|-h
    Shows this help output to standard out and exits

  --version
    Shows version number of script to standard output

  --license
    Displays ${LICENSE_TYPE} license for this script by attempting
    a curl to ${LICENSE_URL}.

  --get
    Attempts to get the current log level for a running spring boot
    application from the /actuator/loggers endpoint.

  --set
    Attempts to set the log level for a running spring boot application.

  --level ${LOG_LEVELS} ($LEVEL if omitted)
    Used to specify a new log level. NOTE: Setting a FATAL log level
    may not be supported and reduced to ERROR level by your spring-boot
    application.
 
  --port|-p PORT (MANAGEMENT_PORT=${MANAGEMENT_PORT:-${PORT}} if omitted)
    The port number your spring-boot application is running on. Like
    8080 in http://localhost:8080/actuator/loggers.

  --pkg PKG|CLASS|ROOT (${CLASS_PATH} if omitted)
    Used to specify the package or specific Java class to apply the
    log level setting to. Use 'ROOT' to apply at top level.
    Use something like 'com.my.package' for a package level
    specification (includes classes and sub-packages) or use
    a class name like 'com.my.package.MyClass' for a specific
    class.

  --namespace K8S_NAME (K8S_NAMESPACE=${K8S_NAMESPACE}}
    If the service is running inside of Kubernetes, use this to specify
    the namespace of the container that you want to run the command
    on. You must also specify either the deployment or pod as well
    when using this option. You can set the K8S_NAMESPACE environment
    variable instead of specifying the command line argument modify
    desired.

  --deployment K8S_NAME|--pod K8S_NAME
    If you specified a Kubernetes namespace, you must also specify
    either the deployment name or the full pod ID where the
    service is running. If you need to use the full pod ID, you can
    use the command shown below to grep for it out of all the running
    pods:

    K8S_NAME=logging_example
    kubectl get --all-namespaces pod | grep $K8S_NAME

  --show
    Shows the curl request instead of executing it.

NOTE: If setting the port won't work for you spring-boot application,
you can set the environment variable ACTUATOR_LOGGERS instead of using
the port setting option. For example:

export ACTUATOR_LOGGERS='http://localhost:8080/actuator/loggers'

NOTE2: In order to communicate with a service running inside of Kubernetes
we need to run a port forward in the background similar to what is shown
belows. After setting up the port forward, we sleep the numer of seconds
that can be adjusted by setting the K8S_SLEEP (${K8S_SLEEP}) environment
variable. Depending on your system, you may need to increase this value.

kubectl port-forward --namespace ${K8S_NAMESPACE:-examples} ${K8S_ITEM:-deployment/my-service} ${PORT}:${PORT} &
"
    if [ "${1}" != "" ]; then
      echo -e "\n$1"
      exit 1
    fi
    exit 0
}

show_license() {

    echo -e "Copyright (c) $COPYRIGHT_YEAR $COPYRIGHT_HOLDER\n"

    if ! curl --silent "${LICENSE_URL}"; then
      echo -e "
Failed to curl the ${LICENSE_TYPE} license text. For the
full license text, please refer to:

${LICENSE_URL}
"
    fi
    exit
}


# Process and validate command line options
while ((${#@} > 0)); do
    declare arg="${1}"
    shift

    case "${arg}" in
    "-h"|"--help") show_help;;
    "--version") echo "${VERSION}"; exit 0;;
    "--license") show_license;;
    "--set") MODE="set";;
    "--get") MODE="get";;
    "-p"|"--port")
        declare -i PORT=${1}
        shift
        (( PORT > 0 )) || show_help "Missing package name, class name or 'ROOT' after '${arg}'"
        declare URL="${URL:-http://localhost:${PORT}${URL_PATH}}"
        ;;
    "-c"|"--pkg")
        declare CLASS_PATH="${1}"
        shift
        [ "${CLASS_PATH}" != "" ] || show_help "Missing package name, class name or 'ROOT' after '${arg}'"
        ;;
    "-l"|"--level")
        LEVEL="${1^^}"
        shift
        [[ "${LOG_LEVELS//${LEVEL}/}" != *"${LOG_LEVELS}" ]] || show_help "Level \"${LEVEL}\" not recognized, use one of ${LOG_LEVELS}"
        ;;
    "--namespace")
        K8S_NAMESPACE="${1}"
        shift
        ;;
    "--deployment")
        K8S_ITEM="deployment/${1}"
        shift
        ;;
    "--pod")
        K8S_ITEM="pod/${1}"
        shift
        ;;
    "--show") SHOW="true";;
    *) show_help "Unrecognized command line option ${arg}";;
  esac
done

# Build path to logger endpoint control 
declare URL_PATH="${URL_PATH:-/actuator/loggers}"
declare ACTUATOR_LOGGERS="${ACTUATOR_LOGGERS:-http://localhost:${PORT}${URL_PATH}/${CLASS_PATH}}"

run() {
    declare cmd="${1}"
    if [ "${SHOW}" == "true" ]; then
        echo "${cmd}"
    else
        declare -i K8S_PID=-1
        if [ "${K8S_ITEM}" != "" ] && [ "${K8S_NAMESPACE}" != "" ]; then
           # Set up Kubernetes tunnel
           kubectl port-forward --namespace "${K8S_NAMESPACE}" "${K8S_ITEM}" "${PORT}:${PORT}" &>/dev/null &
           K8S_PID="${!}";
           sleep "${K8S_SLEEP}";
        fi
        eval "${cmd}"
        exit_code=${?}
        echo # Add new line after any JSON dump
        if [ "${K8S_ITEM}" != "" ] && [ "${K8S_NAMESPACE}" != "" ]; then
           # Tear down Kubernetes tunnel
           kill "${K8S_PID}" &> /dev/null
           wait "${K8S_PID}" &> /dev/null
        fi
        return ${exit_code}
    fi
}

case "${MODE}" in
    "get") run "curl '${ACTUATOR_LOGGERS}'";;
    "set")
        run "curl -i -X POST -H 'Content-Type: application/json' -d '{\"configuredLevel\":\"${LEVEL}\"}' '${ACTUATOR_LOGGERS}'"
        ;;
    *) show_help;;
esac
