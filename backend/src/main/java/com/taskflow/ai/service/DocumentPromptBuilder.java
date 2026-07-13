package com.taskflow.ai.service;

import com.taskflow.entity.Block;
import com.taskflow.entity.Page;
import com.taskflow.entity.Workspace;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class DocumentPromptBuilder {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    public String build(Page page, List<Block> blocks, String authorName, String instruction, String schema) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("Act as a Senior Business Analyst, Senior Technical Writer, and Senior Scrum Master.\n\n");
        prompt.append("DOCUMENT CONTEXT\n");
        prompt.append("═══════════════════════════════════════\n");
        prompt.append("Title: ").append(nullSafe(page.getTitle())).append("\n");
        prompt.append("Workspace: ").append(nullSafe(workspaceName(page))).append("\n");
        prompt.append("Author: ").append(nullSafe(authorName)).append("\n");
        prompt.append("Created: ").append(page.getCreatedAt() != null ? page.getCreatedAt().format(DATE_FORMAT) : "N/A").append("\n\n");

        prompt.append("DOCUMENT CONTENT\n");
        prompt.append("═══════════════════════════════════════\n");

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

    private String workspaceName(Page page) {
        Workspace workspace = page.getWorkspace();
        return workspace != null && workspace.getName() != null ? workspace.getName() : "N/A";
    }

    private String nullSafe(String value) {
        return value != null ? value : "N/A";
    }
}
