#!/bin/bash

# Create directories
mkdir -p worker-b/src/main/java/com/example/workerb/service/model
mkdir -p worker-b/src/main/resources

# Create files
touch worker-b/pom.xml
touch worker-b/src/main/java/com/example/workerb/WorkerBApplication.java
touch worker-b/src/main/java/com/example/workerb/TextAnalyzerAgent.java
touch worker-b/src/main/java/com/example/workerb/service/TextAnalysisService.java
touch worker-b/src/main/java/com/example/workerb/service/model/AnalysisResult.java
touch worker-b/src/main/resources/application.yml

echo "Project structure for 'worker-b' created."

