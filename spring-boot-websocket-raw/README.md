# General

This example demonstrates how to create a spring-boot service that enables raw (text)
based WebSocket connections. This is not a recommendation of using raw WebSockets over
the STOMP based approach, it is just an example of using WebSockets without STOMP.

A [session service](src/main/java/com/redali/example/service/SessionService.java) definition and implementation
are provided with the following goals:

* To handle the addition and removal of WebSocket clients.
* To handle messages posted by the WebSocket clients.
* To provide a mechanism for server side tasks get the list of active clients.
* To provide a mechanism for server side tasks to send messages to clients.

In this example, messages are passed as JSON messages and parsed by hand. A non-example implementation would likely opt for a JSON to Java mapping
approach.

A [controller](src/main/java/com/redali/example/controller/JsonMessageWebSocketController.java) is provided that
manages WebSocket connections and messages coming into the system. The controller allows the
clients to send a message to the server over the WebSocket connection.

A [server side task](src/main/java/com/redali/example/task/PeriodicHealth.java) is provided to demonstrate
a periodic server side task that pushes messages directly to clients on its own.

## Building

```shell
mvn package
```

## Running

```shell
java -jar target/*.jar
```

# Clients

Two simple WebSocket client implementations are provided to test the service.

## [websocket-client.py](src/examples/python/websocket-client.py)

The [websocket-client.py](src/examples/python/websocket-client.py) file contains a Python WebSocket client
implementation that will:

* Connect to the server.
* Request information from the server.
* Display all messages received from the server.

If you have the websocket-client Python3 module installed, you should be able to run the
example directly on the command line via:

```shell
python3 src/examples/python/websocket-client.py
```

You can run the example and get pretty JSON output using jq:

```shell
python3 src/examples/python/websocket-client.py | jq
```

If you don't have jq available, you can try substituting the json.tool module:

```shell
alias json-format='while read LINE; do echo "${LINE}" | python3 -m json.tool; done'
python3 src/examples/python/websocket-client.py | json-format
```
Regardless of your formatting choice, you should see an initial message (stderr)
indicating that the WebSocket connection was made.

```
WebSocket opened, requesting info
```

That should be immediately following by two response messages to the client's
request for server information and a dice roll. After that, periodic health reports
posted directly by the server will start occurring about 9 seconds apart.

```
{
    "id": 0,
    "content": {
        "server": {
            "address": "0:0:0:0:0:0:0:1",
            "port": 8080,
            "host": "localhost",
            "resolved": true
        },
        "client": {
            "address": "0:0:0:0:0:0:0:1",
            "port": 52032,
            "host": "localhost",
            "resolved": true
        },
        "epochMillis": 1693847859462
    }
}
{
    "id": 1,
    "content": {
        "dice": 5,
        "sides": 6,
        "rolls": [
            3,
            3,
            3,
            3,
            4
        ]
    }
}
{
    "id": 200,
    "content": {
        "sessionCount": 1,
        "status": "UP"
    }
}
{
    "id": 200,
    "content": {
        "sessionCount": 1,
        "status": "UP"
    }
}
```

## [websocket-client.js](src/examples/javascript/websocket-client.js)

The [websocket-client.py](src/examples/javascript/websocket-client.js) file contains a JavaScript WebSocket client
implementation that will:

* Connect to the server
* Repeatedly request information and a dice roll from the server.
* Log every message received to the JavaScript console.
* Display the current number of clients connected to the server and value of the last dice roll.

The [websocket-client.html](src/examples/javascript/websocket-client.html) file should allow you
run the JavaScript code in your browser.

If you open up multiple instances of this client, or run the Python client example, you should
see the client count update to match the current number of clients connected to the server.
