#!/usr/bin/python3
"""

Python example implementation that connects to example spring-boot WebSocket service.

"""

import sys
import websocket

def on_message(ws, message):
    print(message)

def on_error(ws, error):
    print(error, file=sys.stderr)


def on_close(ws, close_status_code, close_msg):
    print(f'WebSocket closed ({close_status_code}: {close_msg}))', file=sys.stderr)
    sys.exit(0)


def on_open(ws):
    print(f'WebSocket opened, requesting info', file=sys.stderr)
    ws.send('{ "request": 0 }')
    ws.send('{ "request": 1, "dice": 5 }')

if __name__ == "__main__":
    websocket.enableTrace(False) # Set to True for more diagnostics
    ws = websocket.WebSocketApp("ws://localhost:8080/websocket/json/messages",
                              on_open=on_open,
                              on_message=on_message,
                              on_error=on_error,
                              on_close=on_close)
    ws.run_forever(ping_interval=60, ping_timeout=10)
