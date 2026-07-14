package com.taskflow.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Parsed outline section of a generated document.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocSection {
    /** Heading level - 1 or 2. */
    private int level;
    /** Heading text. */
    private String heading;
    /** Anchor derived from the heading for in-page links. */
    private String anchor;
}
