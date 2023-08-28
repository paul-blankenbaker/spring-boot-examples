class ExampleClient {

    // Setup outputs for information we receive from WebSocket server
    constructor() {
        this.status = document.createElement("input");
        this.status.value = "fetching ...";
        document.body.appendChild(this.status);
        this.die = document.createElement("input");
        this.die.value = "waiting ...";
        document.body.appendChild(this.die);
    }

    // Start WebSocket connection to server and set handlers for WebSocket events
    connect() {
        const self = this;
        const socket = new WebSocket("ws://localhost:8080/websocket/json/messages");
        socket.onopen = function () {
            self.onopen(socket);
        }
        socket.onmessage = function (event) {
            self.onmessage(socket, event);
        }
        socket.onclose = function () {
            console.log("WebSocket connection closed");
            self.status.value = "Closed";
        }
        socket.onerror = function (event) {
            console.log("WebSocket error " + event);
            self.status.value = "ERROR";
        }
    }

    onopen(socket) {
        console.log("WebSocket connection opened");
        this.status.value = "Connected"
        ExampleClient.requestDiceRoll(socket);
        // Better to use JSON.stringify(object) for safety, but strings will work
        socket.send("{ \"request\": 0 }");

        setInterval(function () {
            ExampleClient.requestDiceRoll(socket);
        }, 1000);
    }

    static requestDiceRoll(socket) {
        // Prefer JSON stringify so you don't have to even think about escaping.
        socket.send(JSON.stringify({"request": 1, "dice": 1, "sides": 10}));
    }

    // Processes messages received from WebSocket server (if/else on id does not
    // scale well if there are many message types - this is just a quick example)
    onmessage(socket, event) {
        const message = event.data; // Assuming the server sends plain text messages

        // Handle the incoming message
        console.log("Received message:", message);
        const parts = JSON.parse(message);
        const content = parts["content"];

        if (content) {
            const id = parts["id"];
            if (id === 200) {
                this.status.value = content["sessionCount"] + " client(s)";
            } else if (id === 1) {
                this.die.value = content["rolls"][0];
            }
        }
    }

}