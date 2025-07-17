// src/main/java/com/example/workera/WorkerAApplication.java
package com.example.workera;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
    "com.example.coordinator",
    "io.a2a.receptionist" // <--- Include this explicitly
})
public class WorkerAApplication {
    public static void main(String[] args) {
        SpringApplication.run(WorkerAApplication.class, args);
    }
}