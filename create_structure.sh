#!/bin/bash

# Create directories
mkdir -p coordinator-agent/src/main/java/com/example/coordinator/model
mkdir -p coordinator-agent/src/main/resources

# Create files
touch coordinator-agent/pom.xml
touch coordinator-agent/src/main/java/com/example/coordinator/CoordinatorApplication.java
touch coordinator-agent/src/main/java/com/example/coordinator/CoordinatorAgent.java
touch coordinator-agent/src/main/java/com/example/coordinator/TaskDispatcher.java
touch coordinator-agent/src/main/java/com/example/coordinator/model/WorkerTask.java
touch coordinator-agent/src/main/java/com/example/coordinator/model/AggregatedResult.java
touch coordinator-agent/src/main/resources/application.yml

echo "Project structure for 'coordinator-agent' created."

