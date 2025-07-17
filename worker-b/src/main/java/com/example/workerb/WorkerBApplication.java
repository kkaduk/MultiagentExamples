// src/main/java/com/example/workerb/WorkerBApplication.java
package com.example.workerb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
    "com.example.coordinator",
    "io.a2a.receptionist" // <--- Include this explicitly
})
public class WorkerBApplication {
    public static void main(String[] args) {
        SpringApplication.run(WorkerBApplication.class, args);
    }
}