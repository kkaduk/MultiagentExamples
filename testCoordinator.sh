curl -X POST http://localhost:8080/agent/message \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "method": "message/send",
    "id": "test-123",
    "params": {
      "message": {
        "kind": "message",
        "messageId": "msg-456",
        "role": "user",
        "taskId": "coordinate-task",
        "contextId": "ctx-789",
        "parts": [
          {
            "kind": "text",
            "text": "Analyze the sales data: 100, 150, 200, 175, 225 and provide insights about the performance trends and customer sentiment from the text: The customers love our new product! Sales are amazing and feedback is excellent."
          }
        ]
      },
      "configuration": {
        "acceptedOutputModes": ["text/plain"],
        "blocking": true
      }
    }
  }'
