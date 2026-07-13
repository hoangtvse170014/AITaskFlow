package com.taskflow.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DocumentAiResponse {

    private String summary;

    private String rewrittenContent;

    private List<String> actionItems;

    private String meetingMinutes;

    private String requirements;

    private List<String> keywords;

    private Double confidence;

    private Long processingTimeMs;

    private String error;

    public static DocumentAiResponse success(
            String summary,
            String rewrittenContent,
            List<String> actionItems,
            String meetingMinutes,
            String requirements,
            List<String> keywords,
            Double confidence,
            Long processingTimeMs) {
        return DocumentAiResponse.builder()
                .summary(summary)
                .rewrittenContent(rewrittenContent)
                .actionItems(actionItems)
                .meetingMinutes(meetingMinutes)
                .requirements(requirements)
                .keywords(keywords)
                .confidence(confidence)
                .processingTimeMs(processingTimeMs)
                .build();
    }

    public static DocumentAiResponse error(String error) {
        return DocumentAiResponse.builder()
                .error(error)
                .build();
    }
}
