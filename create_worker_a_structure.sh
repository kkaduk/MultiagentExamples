#!/bin/bash

# Create directories
mkdir -p worker-a/src/main/java/com/example/workera/service
mkdir -p worker-a/src/main/resources

# Create files
touch worker-a/pom.xml
touch worker-a/src/main/java/com/example/workera/WorkerAApplication.java
touch worker-a/src/main/java/com/example/workera/TextTransformerAgent.java
touch worker-a/src/main/java/com/example/workera/service/TextTransformationService.java
touch worker-a/src/main/resources/application.yml

echo "Project structure for 'worker-a' created."

