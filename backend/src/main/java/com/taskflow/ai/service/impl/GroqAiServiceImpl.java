package com.taskflow.ai.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.ai.config.GroqConfig;
import com.taskflow.ai.dto.AiResponse;
import com.taskflow.ai.exception.AiApiException;
import com.taskflow.ai.exception.AiException;
import com.taskflow.ai.exception.AiTimeoutException;
import com.taskflow.ai.service.AiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@org.springframework.context.annotation.Primary
public class GroqAiServiceImpl implements AiService {

    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";
    
    private static final Random RANDOM = new Random(42);

    private final GroqConfig groqConfig;
    private final ObjectMapper objectMapper;

    @Override
    public String generate(String prompt) {
        if (!groqConfig.isValid()) {
            log.info("Running in DEMO MODE - generating demo AI response");
            return generateDemoResponse(prompt);
        }

        long startTime = System.currentTimeMillis();

        try {
            WebClient webClient = createWebClient();
            Map<String, Object> requestBody = buildRequestBody(prompt);

            log.debug("Sending request to Groq API...");

            String response = webClient
                    .post()
                    .uri(GROQ_API_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + groqConfig.getApiKey())
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(groqConfig.getTimeoutSeconds()))
                    .retryWhen(Retry.backoff(groqConfig.getMaxRetries(), Duration.ofSeconds(1))
                            .filter(this::isRetryable)
                            .doBeforeRetry(signal -> log.warn("Retrying Groq request: {}", signal.failure().getMessage())))
                    .doOnError(e -> log.error("Groq request failed: {}", e.getMessage()))
                    .block();

            long processingTime = System.currentTimeMillis() - startTime;
            return extractContent(response, processingTime);

        } catch (WebClientResponseException e) {
            log.error("Groq API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw handleApiError(e);
        } catch (WebClientRequestException e) {
            if (isTimeoutException(e)) {
                throw new AiTimeoutException("Groq request timed out after " + groqConfig.getTimeoutSeconds() + "s",
                        groqConfig.getTimeoutSeconds());
            }
            throw new AiException("Failed to connect to Groq API: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error during Groq request", e);
            throw new AiException("Unexpected error: " + e.getMessage(), e);
        }
    }

    @Override
    public String generateWithSystemPrompt(String systemPrompt, String userPrompt) {
        if (!groqConfig.isValid()) {
            log.info("Running in DEMO MODE - generating demo AI response");
            return generateDemoResponse(userPrompt);
        }

        long startTime = System.currentTimeMillis();

        try {
            WebClient webClient = createWebClient();
            Map<String, Object> requestBody = buildRequestBodyWithCustomSystem(systemPrompt, userPrompt);

            log.debug("Sending request to Groq API with custom system prompt...");

            String response = webClient
                    .post()
                    .uri(GROQ_API_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + groqConfig.getApiKey())
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(groqConfig.getTimeoutSeconds()))
                    .retryWhen(Retry.backoff(groqConfig.getMaxRetries(), Duration.ofSeconds(1))
                            .filter(this::isRetryable)
                            .doBeforeRetry(signal -> log.warn("Retrying Groq request: {}", signal.failure().getMessage())))
                    .doOnError(e -> log.error("Groq request failed: {}", e.getMessage()))
                    .block();

            long processingTime = System.currentTimeMillis() - startTime;
            return extractContent(response, processingTime);

        } catch (WebClientResponseException e) {
            log.error("Groq API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw handleApiError(e);
        } catch (WebClientRequestException e) {
            if (isTimeoutException(e)) {
                throw new AiTimeoutException("Groq request timed out after " + groqConfig.getTimeoutSeconds() + "s",
                        groqConfig.getTimeoutSeconds());
            }
            throw new AiException("Failed to connect to Groq API: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error during Groq request", e);
            throw new AiException("Unexpected error: " + e.getMessage(), e);
        }
    }
    
    private String generateDemoResponse(String prompt) {
        String lowerPrompt = prompt.toLowerCase();
        
        if (lowerPrompt.contains("project") && lowerPrompt.contains("analysis")) {
            return generateProjectAnalysisDemo();
        }
        
        if (lowerPrompt.contains("task") && lowerPrompt.contains("recommend")) {
            return generateTaskRecommendationDemo(prompt);
        }
        
        if (lowerPrompt.contains("document") || lowerPrompt.contains("summar") || lowerPrompt.contains("page")) {
            return generateDocumentSummaryDemo();
        }
        
        if (lowerPrompt.contains("workspace") || lowerPrompt.contains("team") || lowerPrompt.contains("summary")) {
            return generateWorkspaceSummaryDemo();
        }
        
        if (lowerPrompt.contains("workload") || lowerPrompt.contains("overload") || lowerPrompt.contains("busy")) {
            return generateWorkloadAnalysisDemo();
        }
        
        if (lowerPrompt.contains("overdue") || lowerPrompt.contains("late")) {
            return generateOverdueAnalysisDemo();
        }
        
        if (lowerPrompt.contains("goal") || lowerPrompt.contains("okr")) {
            return generateGoalAnalysisDemo();
        }
        
        if (lowerPrompt.contains("requirement") || lowerPrompt.contains("spec")) {
            return generateRequirementsDemo();
        }
        
        if (lowerPrompt.contains("priorit") || lowerPrompt.contains("urgent")) {
            return generatePriorityAnalysisDemo();
        }
        
        return generateGeneralResponse(prompt);
    }
    
    private String generateProjectAnalysisDemo() {
        String[] analyses = {
            "## Project Health Analysis\n\n" +
            "**TaskFlow AI Platform** - Status: 🟡 At Risk\n\n" +
            "### Metrics:\n" +
            "- Completion Rate: 25% (5/20 tasks)\n" +
            "- Team Velocity: 3.2 tasks/week\n" +
            "- Risk Level: Medium\n\n" +
            "### Key Issues:\n" +
            "1. 4 team members are overloaded (>80% capacity)\n" +
            "2. 12 tasks are overdue\n" +
            "3. AI development requires specialized skills\n\n" +
            "### Recommendations:\n" +
            "- Redistribute tasks from Nguyễn Minh to other members\n" +
            "- Consider extending deadline by 1 week\n" +
            "- Prioritize core AI features over nice-to-haves"
        };
        return analyses[RANDOM.nextInt(analyses.length)];
    }
    
    private String generateTaskRecommendationDemo(String prompt) {
        return "## AI Task Assignment Recommendations\n\n" +
               "Based on team workload analysis:\n\n" +
               "### Recommended Assignees:\n\n" +
               "| Task | Recommended | Reason | Current Load |\n" +
               "|------|-------------|--------|---------------|\n" +
               "| Build chatbot assistant | **Nguyễn Huy** | Available, frontend expertise | 38% |\n" +
               "| Risk detection system | **Võ Long** | Backend specialist | 81% ⚠️ |\n" +
               "| Smart search | **Nguyễn Minh 2** | Available capacity | 55% |\n" +
               "| AI dashboard | **Đặng Hải** | Testing background | 50% |\n\n" +
               "### Workload Alert:\n" +
               "⚠️ Nguyễn Minh has 92% workload - avoid assigning more tasks";
    }
    
    private String generateDocumentSummaryDemo() {
        return "## Document Summary\n\n" +
               "### Key Points:\n\n" +
               "1. **Core Features**: AI-powered task recommendations, smart workload balancing, and predictive risk detection\n\n" +
               "2. **Technical Requirements**:\n" +
               "   - React 18+ with TypeScript\n" +
               "   - Spring Boot 3.x microservices\n" +
               "   - PostgreSQL 15+ database\n" +
               "   - Redis caching layer\n\n" +
               "3. **Success Metrics**:\n" +
               "   - Reduce task assignment time by 50%\n" +
               "   - Improve deadline adherence by 30%\n" +
               "   - Increase team productivity by 25%\n\n" +
               "### Action Items:\n" +
               "- [ ] Schedule meeting with stakeholders\n" +
               "- [ ] Review technical architecture\n" +
               "- [ ] Define MVP scope";
    }
    
    private String generateWorkspaceSummaryDemo() {
        return "## TechNova Solutions Workspace Summary\n\n" +
               "### Overview:\n" +
               "- **12** active team members\n" +
               "- **5** ongoing projects\n" +
               "- **80** total tasks (24 completed)\n" +
               "- **30%** completion rate\n\n" +
               "### Team Status:\n" +
               "| Member | Role | Workload | Status |\n" +
               "|--------|------|----------|--------|\n" +
               "| Nguyễn Minh | PM | 92% | 🔴 Overloaded |\n" +
               "| Võ Long | Backend | 81% | 🔴 Overloaded |\n" +
               "| Hồ Khanh | DevOps | 75% | 🟡 High |\n" +
               "| An Tử | PO | 70% | 🟡 High |\n" +
               "| Lê Dũng | QA | 66% | 🟢 Balanced |\n" +
               "| Others | Various | 25-58% | 🟢 Normal |\n\n" +
               "### Top Risks:\n" +
               "1. 25 overdue tasks need attention\n" +
               "2. 4 team members are overloaded\n" +
               "3. Mobile Banking App security audit pending\n\n" +
               "### Recommended Actions:\n" +
               "1. Redistribute 5-8 tasks from overloaded members\n" +
               "2. Extend Hospital Management System timeline\n" +
               "3. Prioritize CRM completion bonuses";
    }
    
    private String generateWorkloadAnalysisDemo() {
        return "## Workload Analysis Report\n\n" +
               "### Overloaded Members (Need Attention):\n\n" +
               "🔴 **Nguyễn Minh** - 92% (PM)\n" +
               "- 9 open tasks, 7 in progress\n" +
               "- Recommendation: Delegate 3-4 tasks to team\n\n" +
               "🔴 **Võ Long** - 81% (Backend Developer)\n" +
               "- 7 open tasks\n" +
               "- Recommendation: Prioritize critical tasks only\n\n" +
               "🔴 **Hồ Khanh** - 75% (DevOps)\n" +
               "- 6 open tasks, 5 in progress\n" +
               "- Recommendation: Schedule deployment tasks carefully\n\n" +
               "🔴 **An Tử** - 70% (Product Owner)\n" +
               "- 5 open tasks, 5 in progress\n" +
               "- Recommendation: Focus on roadmap alignment\n\n" +
               "### Available Capacity:\n" +
               "🟢 **Phạm Trang** - 25% (UI/UX)\n" +
               "🟢 **Nguyễn Huy** - 38% (Frontend)\n" +
               "🟢 **Phùng Lan** - 35% (Business Analyst)\n\n" +
               "### Recommendation:\n" +
               "**Immediate action**: Redistribute 5-8 tasks from overloaded to available members.";
    }
    
    private String generateOverdueAnalysisDemo() {
        return "## Overdue Tasks Analysis\n\n" +
               "### Summary:\n" +
               "📊 **25 tasks** are currently overdue\n\n" +
               "### Critical Overdue (Past 7+ days):\n" +
               "- AI-3: Build risk detection system (14 days late)\n" +
               "- AI-5: Implement smart search (10 days late)\n" +
               "- MBA-26: Create loan application flow (12 days late)\n\n" +
               "### By Project:\n" +
               "| Project | Overdue | Total | % Late |\n" +
               "|---------|---------|-------|--------|\n" +
               "| TaskFlow AI | 8 | 20 | 40% |\n" +
               "| Mobile Banking | 7 | 18 | 39% |\n" +
               "| Hospital Management | 6 | 15 | 40% |\n\n" +
               "### Root Causes:\n" +
               "1. Team members are overloaded\n" +
               "2. Scope creep in AI features\n" +
               "3. Dependencies on external systems\n\n" +
               "### Recommended Actions:\n" +
               "1. Reassign 5 tasks to available members\n" +
               "2. Extend deadlines by 1-2 weeks\n" +
               "3. Cancel or defer low-priority tasks";
    }
    
    private String generateGoalAnalysisDemo() {
        return "## Goals Status Report\n\n" +
               "### Active Goals:\n\n" +
               "🎯 **TaskFlow AI Platform Launch** - 65% complete\n" +
               "- Key Results: 3/5 on track\n" +
               "- Status: 🟡 On Track\n\n" +
               "🎯 **Mobile Banking App Launch** - 45% complete\n" +
               "- Key Results: 2/5 on track\n" +
               "- Status: 🟡 On Track\n\n" +
               "🎯 **Engineering Excellence** - 25% complete ⚠️\n" +
               "- Key Results: 1/5 on track\n" +
               "- Status: 🔴 Behind Schedule\n\n" +
               "🎯 **Quality Assurance** - 70% complete\n" +
               "- Key Results: 4/5 on track\n" +
               "- Status: 🟢 On Track\n\n" +
               "✅ **Customer Success Q2** - 100% complete\n" +
               "- All 5 key results achieved\n" +
               "- Status: 🟢 Completed\n\n" +
               "### Recommended Actions:\n" +
               "1. Focus on Engineering Excellence goal\n" +
               "2. Schedule bi-weekly OKR reviews\n" +
               "3. Celebrate Customer Success win!";
    }
    
    private String generateRequirementsDemo() {
        return "## AI-Generated Software Requirements\n\n" +
               "### Project: TaskFlow AI Enhancement\n\n" +
               "#### 1. Functional Requirements\n\n" +
               "| ID | Requirement | Priority | Module |\n" +
               "|----|-------------|----------|--------|\n" +
               "| FR-001 | Real-time task suggestions | High | AI Engine |\n" +
               "| FR-002 | Workload auto-balancing | High | AI Engine |\n" +
               "| FR-003 | Risk prediction alerts | Medium | Analytics |\n" +
               "| FR-004 | Meeting transcription | Medium | Integration |\n" +
               "| FR-005 | Smart search with filters | High | Search |\n\n" +
               "#### 2. Non-Functional Requirements\n\n" +
               "- Response time: <500ms for AI suggestions\n" +
               "- Availability: 99.9% uptime\n" +
               "- Accuracy: >85% for task recommendations\n\n" +
               "#### 3. Acceptance Criteria\n\n" +
               "1. AI recommends task assignments within 500ms\n" +
               "2. Workload rebalancing completes without data loss\n" +
               "3. Risk alerts trigger at 70% deadline threshold";
    }
    
    private String generatePriorityAnalysisDemo() {
        return "## Priority Analysis\n\n" +
               "### Immediate Priorities (This Week):\n\n" +
               "🔴 **URGENT**:\n" +
               "1. Fix authentication bug in Mobile Banking\n" +
               "2. Complete security audit fixes\n\n" +
               "🟠 **HIGH** (Due within 3 days):\n" +
               "1. Deploy AI recommendation engine\n" +
               "2. Complete payment integration testing\n" +
               "3. Submit app store application\n\n" +
               "🟡 **MEDIUM** (Due within 1 week):\n" +
               "1. Build AI dashboard\n" +
               "2. Document API endpoints\n" +
               "3. User testing sessions\n\n" +
               "🟢 **LOW** (Backlog):\n" +
               "1. Gamification features\n" +
               "2. Advanced analytics\n" +
               "3. Mobile dark mode\n\n" +
               "### Recommended Focus:\n" +
               "1. Clear URGENT blockers first\n" +
               "2. Protect HIGH priority team members\n" +
               "3. Delegate MEDIUM tasks to available capacity";
    }
    
    private String generateGeneralResponse(String prompt) {
        return "## AI Analysis\n\n" +
               "Based on the TechNova Solutions workspace data:\n\n" +
               "### Key Insights:\n\n" +
               "1. **Team Health**: 4 of 12 members are overloaded (>70% capacity)\n" +
               "   - Nguyễn Minh (PM): 92%\n" +
               "   - Võ Long (Backend): 81%\n" +
               "   - Hồ Khanh (DevOps): 75%\n" +
               "   - An Tử (PO): 70%\n\n" +
               "2. **Project Status**:\n" +
               "   - TaskFlow AI Platform: 25% complete (at risk)\n" +
               "   - Mobile Banking: 30% complete\n" +
               "   - Hospital Management: 0% complete (planning)\n" +
               "   - E-learning: 50% complete\n" +
               "   - CRM: 100% complete\n\n" +
               "3. **Action Items**:\n" +
               "   - Review and redistribute overloaded workloads\n" +
               "   - Focus on AI Platform critical path\n" +
               "   - Schedule risk assessment meeting\n\n" +
               "### Recommendations:\n" +
               "Would you like me to generate a detailed report on any specific aspect?";
    }

    private WebClient createWebClient() {
        return WebClient.builder()
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    private Map<String, Object> buildRequestBody(String prompt) {
        Map<String, Object> systemMessage = new LinkedHashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", "You are TaskFlow AI, a helpful assistant for project management and team collaboration.");

        Map<String, Object> userMessage = new LinkedHashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);

        return Map.of(
                "model", groqConfig.getModel(),
                "messages", List.of(systemMessage, userMessage),
                "temperature", groqConfig.getTemperature()
        );
    }

    private Map<String, Object> buildRequestBodyWithCustomSystem(String systemPrompt, String userPrompt) {
        Map<String, Object> systemMessage = new LinkedHashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemPrompt);

        Map<String, Object> userMessage = new LinkedHashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", userPrompt);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", groqConfig.getModel());
        body.put("messages", List.of(systemMessage, userMessage));
        body.put("temperature", groqConfig.getTemperature());
        body.put("max_tokens", groqConfig.getMaxTokens());

        return body;
    }

    private String extractContent(String response, long processingTime) {
        if (response == null || response.isBlank()) {
            log.warn("Groq returned empty response");
            throw new AiException("Groq returned empty response");
        }

        log.info("RAW GROQ RESPONSE received in {}ms", processingTime);

        try {
            JsonNode root = objectMapper.readTree(response);

            JsonNode choices = root.path("choices");
            if (choices.isMissingNode() || !choices.isArray() || choices.isEmpty()) {
                throw new AiException("No choices in Groq response");
            }

            JsonNode message = choices.get(0).path("message");
            if (message.isMissingNode()) {
                throw new AiException("No message in Groq response");
            }

            String content = message.path("content").asText("");
            if (content.isEmpty()) {
                throw new AiException("Empty content in Groq response");
            }

            String cleanedContent = cleanGroqResponse(content);
            log.info("CLEANED RESPONSE length: {}", cleanedContent.length());

            // Log usage for cost monitoring
            JsonNode usage = root.path("usage");
            if (!usage.isMissingNode() && !usage.isNull()) {
                int promptTokens = usage.path("prompt_tokens").asInt(0);
                int completionTokens = usage.path("completion_tokens").asInt(0);
                int totalTokens = usage.path("total_tokens").asInt(0);
                log.info("Groq token usage - prompt={}, completion={}, total={} ({}ms)",
                        promptTokens, completionTokens, totalTokens, processingTime);
            } else {
                log.info("Groq response received in {}ms", processingTime);
            }

            return cleanedContent;

        } catch (AiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error parsing Groq response: {}", e.getMessage());
            throw new AiException("Failed to parse Groq response: " + e.getMessage(), e);
        }
    }

    private String cleanGroqResponse(String content) {
        String cleaned = content.trim();
        
        // Remove markdown code blocks if present
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        
        cleaned = cleaned.trim();
        
        return cleaned;
    }

    private boolean isRetryable(Throwable throwable) {
        if (throwable instanceof WebClientResponseException e) {
            int status = e.getStatusCode().value();
            // Retry on 429 (rate limit), 500, 502, 503 (server errors)
            return status == 429 || status == 500 || status == 502 || status == 503;
        }
        if (throwable instanceof WebClientRequestException) {
            return isTimeoutException((WebClientRequestException) throwable);
        }
        return false;
    }

    private boolean isTimeoutException(WebClientRequestException e) {
        String message = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        return message.contains("timeout") || message.contains("timed out");
    }

    private AiApiException handleApiError(WebClientResponseException e) {
        int statusCode = e.getStatusCode().value();
        String errorType = "API_ERROR";
        String message = e.getMessage();

        try {
            JsonNode errorBody = objectMapper.readTree(e.getResponseBodyAsString());
            
            // Groq error format
            JsonNode errorNode = errorBody.path("error");
            if (!errorNode.isMissingNode()) {
                errorType = errorNode.path("type").asText(errorType);
                String errorMessage = errorNode.path("message").asText(message);
                message = errorMessage;
            }
        } catch (Exception ex) {
            log.debug("Could not parse error response body");
        }

        return new AiApiException(message, statusCode, errorType);
    }
}
