package com.taskflow.ai.service;

public interface AiService {

    String generate(String prompt);

    String generateWithSystemPrompt(String systemPrompt, String userPrompt);

    // Future implementations
    // String analyzeProject(String projectData);
    // String summarizeDocument(String documentContent);
    // String assignTasks(String taskData);
    // String chat(String conversation);
}
