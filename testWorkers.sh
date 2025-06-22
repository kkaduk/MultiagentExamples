# Test Worker A (Data Processor)
curl -X POST http://localhost:8082/agent/message \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "method": "message/send",
    "id": "test-data",
    "params": {
      "message": {
        "kind": "message",
        "messageId": "msg-data",
        "role": "user",
        "taskId": "process-data",
        "contextId": "ctx-data",
        "parts": [
          {
            "kind": "text",
            "text": "Process these sales numbers: 100, 150, 200, 175, 225"
          }
        ]
      },
      "configuration": {
        "acceptedOutputModes": ["text/plain"],
        "blocking": true
      }
    }
  }'

# Test Worker B (Text Processor)
curl -X POST http://localhost:8083/agent/message \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "method": "message/send",
    "id": "test-text",
    "params": {
      "message": {
        "kind": "message",
        "messageId": "msg-text",
        "role": "user",
        "taskId": "process-text",
        "contextId": "ctx-text",
        "parts": [
          {
            "kind": "text",
            "text": "The customers love our new product! Sales are amazing and feedback is excellent. This is the best product we have ever launched."
          }
        ]
      },
      "configuration": {
        "acceptedOutputModes": ["text/plain"],
        "blocking": true
      }
    }
  }'
