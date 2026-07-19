package com.taskflow.ai.dto;

/**
 * The set of documents the AI Documentation module can generate. Each value
 * corresponds to a dedicated REST endpoint so that the frontend can hit them
 * individually while still sharing a single backend pipeline.
 */
public enum DocumentType {

    /** Software Requirements Specification for a project or topic. */
    SRS,

    /** User Stories describing what the actor wants and why. */
    USER_STORIES,

    /** Acceptance Criteria for an existing project / topic. */
    ACCEPTANCE_CRITERIA,

    /** Meeting Minutes derived from a page or topic. */
    MEETING_MINUTES,

    /** Sprint Review summary - what was planned vs. delivered. */
    SPRINT_REVIEW,

    /** Sprint Retrospective - what went well / poorly / improvements. */
    RETROSPECTIVE,

    /** Technical Specification - architecture, modules, data model. */
    TECHNICAL_SPEC
}
