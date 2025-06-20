package com.example.coordinator.tools;

public class PromptTemplateTools {

     private static final String COORDINATOR_QUERY_REWRITE_TEMPLATE = """
            You are a task distribution planner in a multi-agent system.
            Task: "Develop and deploy a machine learning model to predict customer churn using historical CRM data."
            Agents:
            - Alice: skills = ["data preprocessing", "agent url", "feature engineering"]
            - Bob: skills = ["model training",  "agent url", "hyperparameter tuning"]
            - Charlie: skills = ["deployment",  "agent url", "CI/CD", "monitoring"]

            Distribute the task into subtasks based on agent skills.
            Return output as JSON with fields: "agent", "assigned_subtask", "reason", "agent url".

            Distrubute the folloing task to the agents:
            {user_query}
            """;

}
