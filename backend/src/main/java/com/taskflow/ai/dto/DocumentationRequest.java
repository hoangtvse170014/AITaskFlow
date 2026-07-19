package com.taskflow.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request payload for AI Documentation generation.
 *
 * <p>All fields except {@code workspaceId} and {@code documentType} are optional.
 * The AI uses the {@code workspaceId} to pull the authoritative workspace state
 * from the database and then writes a Markdown document derived purely from
 * that state. Nothing is ever persisted - this is a read-only generative call.
 *
 * <p>Targeting options:
 * <ul>
 *   <li>{@code projectId} - restrict the doc to a single project (SRS,
 *       Technical Spec, Acceptance Criteria, User Stories)</li>
 *   <li>{@code sprintId} / {@code sprintName} - restrict to a sprint (Sprint
 *       Review, Retrospective)</li>
 *   <li>{@code pageId} - use the content of an existing page as additional
 *       source material (Meeting Minutes, Retrospective)</li>
 *   <li>{@code topic} - free-form seed for SRS / Technical Spec when no
 *       project exists yet</li>
 * </ul>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DocumentationRequest {

    private String workspaceId;

    private DocumentType documentType;

    /** Optional project scope - SRS / User Stories / Tech Spec. */
    private String projectId;

    /** Optional sprint scope - Sprint Review / Retrospective. */
    private String sprintId;

    /** Optional sprint label - used as scope if no sprintId. */
    private String sprintName;

    /** Optional page source - Meeting Minutes often come from a transcript page. */
    private String pageId;

    /** Optional free-form seed - SRS / Tech Spec / Meeting topic. */
    private String topic;

    /** Optional author / meeting facilitator name used in headers. */
    private String author;

    /** Optional duration in days for sprint-related docs. */
    private Integer durationDays;

    /** Optional attendee list for Meeting Minutes. */
    private List<String> attendees;

    /** Audience for the document - DEV, QA, PM, STAKEHOLDER. */
    private String audience;
}
