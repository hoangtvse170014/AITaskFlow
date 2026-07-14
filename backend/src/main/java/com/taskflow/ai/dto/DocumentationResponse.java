package com.taskflow.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response payload for AI Documentation generation.
 *
 * <p>The {@code markdown} field is the canonical output - human-readable,
 * copy/paste ready. The structured {@code sections} list is the parsed outline
 * so the frontend can build a table of contents and anchor links without
 * re-parsing the Markdown.
 *
 * <p>{@code source} tells the caller whether the response was produced by
 * the real LLM ({@code GROQ}), the deterministic local generator
 * ({@code LOCAL_FALLBACK}) or the demo mock ({@code DEMO_MODE}) so the UI
 * can show a "demo data" badge when appropriate.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DocumentationResponse {

    private DocumentType documentType;

    /** Canonical Markdown output. Always present. */
    private String markdown;

    /** Title for the document, derived from scope. */
    private String title;

    /** Parsed outline - first-level and second-level headings with anchors. */
    private List<DocSection> sections;

    /** Free-form tags / keywords that describe the document. */
    private List<String> keywords;

    /** Source of the markdown - GROQ / LOCAL_FALLBACK / DEMO_MODE. */
    private String source;

    /** 0.0-1.0 self-reported confidence from the generator. */
    private Double confidence;

    /** Processing time in milliseconds. */
    private Long processingTimeMs;

    /** If non-null the response is an error envelope. */
    private String error;

    public static DocumentationResponse error(DocumentType type, String message) {
        return DocumentationResponse.builder()
                .documentType(type)
                .error(message)
                .build();
    }
}
