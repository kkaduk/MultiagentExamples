package com.example.coordinator.workflow.config;


import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import net.kaduk.a2a.A2AAgent;

import com.oracle.a2a.client.A2AClient;

@Configuration
public class A2AClientConfiguration {
    
    @Value("${a2a.financial-report.url}")
    private String financialReportUrl;
    
    @Value("${a2a.financial-data-sources.url}")
    private String financialDataSourcesUrl;
    
    @Value("${a2a.email-sender.url}")
    private String emailSenderUrl;
    
    @Value("${a2a.default.url}")
    private String defaultUrl;
    
    @Bean
    public Map<String, A2AAgent> agentClients() {
        return Map.of(
            "financial report", new A2AClient(financialReportUrl),
            "financialdatasources", new A2AClient(financialDataSourcesUrl),
            "email sender", new A2AClient(emailSenderUrl),
            "default", new A2AClient(defaultUrl)
        );
    }
}
