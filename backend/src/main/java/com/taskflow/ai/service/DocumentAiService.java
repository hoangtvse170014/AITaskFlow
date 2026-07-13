package com.taskflow.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.ai.config.GeminiConfig;
import com.taskflow.ai.dto.DocumentAiResponse;
import com.taskflow.ai.exception.AiApiException;
import com.taskflow.ai.exception.AiException;
import com.taskflow.ai.exception.AiTimeoutException;
import com.taskflow.entity.Block;
import com.taskflow.entity.Page;
import com.taskflow.entity.User;
import com.taskflow.entity.Workspace;
import com.taskflow.repository.BlockRepository;
import com.taskflow.repository.PageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentAiService {

    private static final String SYSTEM_PROMPT = """
            You are a senior business analyst, technical writer, and scrum master.
            You analyze document content and produce structured insights.
            Return ONLY valid JSON, no markdown, no explanation.
            """;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final AiService aiService;
    private final GeminiConfig geminiConfig;
    private final PageRepository pageRepository;
    private final BlockRepository blockRepository;
    private final ObjectMapper objectMapper;

    public DocumentAiResponse summarize(UUID pageId) {
        long startTime = System.currentTimeMillis();

        PageContext context = loadContextTx(pageId);

        String prompt = buildPrompt(
                context,
                DocumentMode.SUMMARIZE,
                "Summarize the following document into a concise executive summary.",
                """
                {
                  "summary": "",
                  "keywords": [],
                  "confidence": 0.0
                }
                """
        );

        String aiResponse = callGemini(prompt);
        long processingTime = System.currentTimeMillis() - startTime;

        return parseDocumentResponse(aiResponse, processingTime, DocumentMode.SUMMARIZE);
    }

    public DocumentAiResponse rewrite(UUID pageId) {
        long startTime = System.currentTimeMillis();

        PageContext context = loadContextTx(pageId);

        String prompt = buildPrompt(
                context,
                DocumentMode.REWRITE,
                "Rewrite the following document content to be more professional, clear, and well-structured while preserving all original meaning.",
                """
                {
                  "rewrittenContent": "",
                  "keywords": [],
                  "confidence": 0.0
                }
                """
        );

        String aiResponse = callGemini(prompt);
        long processingTime = System.currentTimeMillis() - startTime;

        return parseDocumentResponse(aiResponse, processingTime, DocumentMode.REWRITE);
    }

    public DocumentAiResponse extractActions(UUID pageId) {
        long startTime = System.currentTimeMillis();

        PageContext context = loadContextTx(pageId);

        String prompt = buildPrompt(
                context,
                DocumentMode.ACTIONS,
                "Extract all action items from the following document. Each action item must be specific and assignable.",
                """
                {
                  "actionItems": [],
                  "keywords": [],
                  "confidence": 0.0
                }
                """
        );

        String aiResponse = callGemini(prompt);
        long processingTime = System.currentTimeMillis() - startTime;

        return parseDocumentResponse(aiResponse, processingTime, DocumentMode.ACTIONS);
    }

    public DocumentAiResponse generateMeetingMinutes(UUID pageId) {
        long startTime = System.currentTimeMillis();

        PageContext context = loadContextTx(pageId);

        String prompt = buildPrompt(
                context,
                DocumentMode.MEETING,
                "Generate professional meeting minutes from the following document. Include attendees, topics discussed, decisions, and action items.",
                """
                {
                  "meetingMinutes": "",
                  "keywords": [],
                  "confidence": 0.0
                }
                """
        );

        String aiResponse = callGemini(prompt);
        long processingTime = System.currentTimeMillis() - startTime;

        return parseDocumentResponse(aiResponse, processingTime, DocumentMode.MEETING);
    }

    public DocumentAiResponse generateRequirements(UUID pageId) {
        long startTime = System.currentTimeMillis();

        PageContext context = loadContextTx(pageId);

        String prompt = buildPrompt(
                context,
                DocumentMode.REQUIREMENTS,
                "Generate structured software requirements from the following document. Include functional and non-functional requirements where possible.",
                """
                {
                  "requirements": "",
                  "keywords": [],
                  "confidence": 0.0
                }
                """
        );

        String aiResponse = callGemini(prompt);
        long processingTime = System.currentTimeMillis() - startTime;

        return parseDocumentResponse(aiResponse, processingTime, DocumentMode.REQUIREMENTS);
    }

    private PageContext loadContext(UUID pageId) {
        Page page = pageRepository.findById(pageId)
                .orElseThrow(() -> new IllegalArgumentException("Page not found: " + pageId));

        if (page.getWorkspace() == null) {
            throw new IllegalStateException("Page has no associated workspace");
        }

        List<Block> blocks = blockRepository.findAllByPageIdOrderByPosition(pageId);

        String authorName = "Unknown";
        if (page.getCreatedBy() != null) {
            authorName = nullSafe(page.getCreatedBy().getFullName());
        }

        return new PageContext(page, blocks, authorName);
    }

    /**
     * Transactional loader kept separate from the public AI entry points so that
     * the blocking Gemini call (up to 120s + retries) does NOT hold a DB connection.
     */
    @Transactional(readOnly = true)
    protected PageContext loadContextTx(UUID pageId) {
        return loadContext(pageId);
    }

    private String buildPrompt(PageContext context, DocumentMode mode, String instruction, String schema) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("Act as a Senior Business Analyst, Senior Technical Writer, and Senior Scrum Master.\n\n");
        prompt.append("DOCUMENT CONTEXT\n");
        prompt.append("═══════════════════════════════════════\n");
        prompt.append("Title: ").append(nullSafe(context.page().getTitle())).append("\n");
        prompt.append("Workspace: ").append(nullSafe(context.workspaceName())).append("\n");
        prompt.append("Author: ").append(nullSafe(context.authorName())).append("\n");
        prompt.append("Created: ").append(context.page().getCreatedAt() != null ? context.page().getCreatedAt().format(DATE_FORMAT) : "N/A").append("\n\n");

        prompt.append("DOCUMENT CONTENT\n");
        prompt.append("═══════════════════════════════════════\n");

        List<Block> blocks = context.blocks();
        if (blocks == null || blocks.isEmpty()) {
            prompt.append("[Empty document]\n");
        } else {
            for (Block block : blocks) {
                String type = nullSafe(block.getType());
                String content = nullSafe(block.getContent());
                if (content != null && !content.isBlank()) {
                    prompt.append("[").append(type).append("]\n").append(content).append("\n\n");
                }
            }
        }

        prompt.append("\nTASK\n");
        prompt.append("═══════════════════════════════════════\n");
        prompt.append(instruction).append("\n\n");

        prompt.append("OUTPUT SCHEMA\n");
        prompt.append("═══════════════════════════════════════\n");
        prompt.append(schema).append("\n\n");

        prompt.append("Rules:\n");
        prompt.append("- confidence: 0.0-1.0 based on document clarity and completeness\n");
        prompt.append("- keywords: 3-7 key topics or terms\n");
        prompt.append("- Return ONLY JSON, no markdown or explanation\n");

        return prompt.toString();
    }

    private String callGemini(String prompt) {
        if (!geminiConfig.isValid()) {
            throw new AiException("Gemini API key is not configured or invalid");
        }

        try {
            return aiService.generate(SYSTEM_PROMPT + "\n\n" + prompt);
        } catch (AiTimeoutException e) {
            log.error("Gemini request timed out during document AI", e);
            throw e;
        } catch (AiException e) {
            log.error("Gemini API error during document AI: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during document AI", e);
            throw new AiException("Failed to process document with AI: " + e.getMessage(), e);
        }
    }

    private DocumentAiResponse parseDocumentResponse(String aiResponse, long processingTime, DocumentMode mode) {
        try {
            String json = extractJson(aiResponse);

            if (json == null || json.isBlank()) {
                return buildDefaultResponse("Empty response from AI", processingTime, mode);
            }

            JsonNode root = objectMapper.readTree(json);

            switch (mode) {
                case SUMMARIZE -> {
                    String summary = root.path("summary").asText(null);
                    List<String> keywords = parseStringList(root.path("keywords"));
                    Double confidence = root.path("confidence").asDouble(0.0);
                    if (summary == null || summary.isBlank()) {
                        return buildDefaultResponse("Missing summary in AI response", processingTime, mode);
                    }
                    return DocumentAiResponse.success(summary, null, null, null, null, keywords, confidence, processingTime);
                }
                case REWRITE -> {
                    String rewritten = root.path("rewrittenContent").asText(null);
                    List<String> keywords = parseStringList(root.path("keywords"));
                    Double confidence = root.path("confidence").asDouble(0.0);
                    if (rewritten == null || rewritten.isBlank()) {
                        return buildDefaultResponse("Missing rewritten content in AI response", processingTime, mode);
                    }
                    return DocumentAiResponse.success(null, rewritten, null, null, null, keywords, confidence, processingTime);
                }
                case ACTIONS -> {
                    List<String> actionItems = parseStringList(root.path("actionItems"));
                    List<String> keywords = parseStringList(root.path("keywords"));
                    Double confidence = root.path("confidence").asDouble(0.0);
                    if (actionItems.isEmpty()) {
                        return buildDefaultResponse("No action items found in AI response", processingTime, mode);
                    }
                    return DocumentAiResponse.success(null, null, actionItems, null, null, keywords, confidence, processingTime);
                }
                case MEETING -> {
                    String meetingMinutes = root.path("meetingMinutes").asText(null);
                    List<String> keywords = parseStringList(root.path("keywords"));
                    Double confidence = root.path("confidence").asDouble(0.0);
                    if (meetingMinutes == null || meetingMinutes.isBlank()) {
                        return buildDefaultResponse("Missing meeting minutes in AI response", processingTime, mode);
                    }
                    return DocumentAiResponse.success(null, null, null, meetingMinutes, null, keywords, confidence, processingTime);
                }
                case REQUIREMENTS -> {
                    String requirements = root.path("requirements").asText(null);
                    List<String> keywords = parseStringList(root.path("keywords"));
                    Double confidence = root.path("confidence").asDouble(0.0);
                    if (requirements == null || requirements.isBlank()) {
                        return buildDefaultResponse("Missing requirements in AI response", processingTime, mode);
                    }
                    return DocumentAiResponse.success(null, null, null, null, requirements, keywords, confidence, processingTime);
                }
                default -> {
                    return buildDefaultResponse("Unsupported document AI mode", processingTime, mode);
                }
            }
        } catch (AiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse document AI response: {}", e.getMessage());
            return buildDefaultResponse("Failed to parse AI response: " + e.getMessage(), processingTime, mode);
        }
    }

    private List<String> parseStringList(JsonNode node) {
        List<String> list = new ArrayList<>();
        if (node.isArray()) {
            for (JsonNode item : node) {
                String text = item.asText();
                if (text != null && !text.isBlank()) {
                    list.add(text);
                }
            }
        }
        return list;
    }

    private String extractJson(String text) {
        if (text == null) {
            return null;
        }

        String trimmed = text.trim();

        int jsonStart = trimmed.indexOf('{');
        int jsonEnd = trimmed.lastIndexOf('}');

        if (jsonStart >= 0 && jsonEnd > jsonStart) {
            return trimmed.substring(jsonStart, jsonEnd + 1);
        }

        return trimmed;
    }

    private DocumentAiResponse buildDefaultResponse(String errorMessage, long processingTime, DocumentMode mode) {
        return switch (mode) {
            case SUMMARIZE -> DocumentAiResponse.success(
                    "AI summary temporarily unavailable. " + errorMessage,
                    null, null, null, null, null, 0.0, processingTime);
            case REWRITE -> DocumentAiResponse.success(
                    null,
                    "Document content is temporarily unavailable for rewrite. " + errorMessage,
                    null, null, null, null, 0.0, processingTime);
            case ACTIONS -> DocumentAiResponse.success(
                    null, null,
                    Collections.singletonList("Unable to generate action items. " + errorMessage),
                    null, null, null, 0.0, processingTime);
            case MEETING -> DocumentAiResponse.success(
                    null, null, null,
                    "Meeting minutes generation is temporarily unavailable. " + errorMessage,
                    null, null, 0.0, processingTime);
            case REQUIREMENTS -> DocumentAiResponse.success(
                    null, null, null, null,
                    "Software requirements generation is temporarily unavailable. " + errorMessage,
                    null, 0.0, processingTime);
        };
    }

    private String nullSafe(String value) {
        return value != null ? value : "N/A";
    }

    private record PageContext(Page page, List<Block> blocks, String authorName) {
        String workspaceName() {
            Workspace workspace = page.getWorkspace();
            return workspace != null && workspace.getName() != null ? workspace.getName() : "N/A";
        }
    }

    private enum DocumentMode {
        SUMMARIZE,
        REWRITE,
        ACTIONS,
        MEETING,
        REQUIREMENTS
    }
}
