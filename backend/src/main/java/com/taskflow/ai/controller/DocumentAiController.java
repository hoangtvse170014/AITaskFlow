package com.taskflow.ai.controller;

import com.taskflow.ai.dto.DocumentAiResponse;
import com.taskflow.ai.service.DocumentAiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/ai/pages/{pageId}")
@RequiredArgsConstructor
@Slf4j
public class DocumentAiController {

    private final DocumentAiService documentAiService;

    @PostMapping("/summarize")
    public ResponseEntity<DocumentAiResponse> summarize(@PathVariable UUID pageId) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("Received summarize request for page: {}", pageId);
            DocumentAiResponse response = documentAiService.summarize(pageId);
            log.info("Summarize completed in {}ms", System.currentTimeMillis() - startTime);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid page ID: {}", pageId, e);
            return ResponseEntity.badRequest().body(DocumentAiResponse.error("Page not found: " + pageId));
        } catch (IllegalStateException e) {
            log.warn("Invalid page state: {}", pageId, e);
            return ResponseEntity.badRequest().body(DocumentAiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Summarize failed for {}: {}", pageId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(DocumentAiResponse.error("Summarization failed: " + e.getMessage()));
        }
    }

    @PostMapping("/rewrite")
    public ResponseEntity<DocumentAiResponse> rewrite(@PathVariable UUID pageId) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("Received rewrite request for page: {}", pageId);
            DocumentAiResponse response = documentAiService.rewrite(pageId);
            log.info("Rewrite completed in {}ms", System.currentTimeMillis() - startTime);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid page ID: {}", pageId, e);
            return ResponseEntity.badRequest().body(DocumentAiResponse.error("Page not found: " + pageId));
        } catch (IllegalStateException e) {
            log.warn("Invalid page state: {}", pageId, e);
            return ResponseEntity.badRequest().body(DocumentAiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Rewrite failed for {}: {}", pageId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(DocumentAiResponse.error("Rewrite failed: " + e.getMessage()));
        }
    }

    @PostMapping("/actions")
    public ResponseEntity<DocumentAiResponse> extractActions(@PathVariable UUID pageId) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("Received action items request for page: {}", pageId);
            DocumentAiResponse response = documentAiService.extractActions(pageId);
            log.info("Action extraction completed in {}ms", System.currentTimeMillis() - startTime);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid page ID: {}", pageId, e);
            return ResponseEntity.badRequest().body(DocumentAiResponse.error("Page not found: " + pageId));
        } catch (IllegalStateException e) {
            log.warn("Invalid page state: {}", pageId, e);
            return ResponseEntity.badRequest().body(DocumentAiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Action extraction failed for {}: {}", pageId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(DocumentAiResponse.error("Action extraction failed: " + e.getMessage()));
        }
    }

    @PostMapping("/meeting")
    public ResponseEntity<DocumentAiResponse> generateMeetingMinutes(@PathVariable UUID pageId) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("Received meeting minutes request for page: {}", pageId);
            DocumentAiResponse response = documentAiService.generateMeetingMinutes(pageId);
            log.info("Meeting minutes completed in {}ms", System.currentTimeMillis() - startTime);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid page ID: {}", pageId, e);
            return ResponseEntity.badRequest().body(DocumentAiResponse.error("Page not found: " + pageId));
        } catch (IllegalStateException e) {
            log.warn("Invalid page state: {}", pageId, e);
            return ResponseEntity.badRequest().body(DocumentAiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Meeting minutes failed for {}: {}", pageId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(DocumentAiResponse.error("Meeting minutes generation failed: " + e.getMessage()));
        }
    }

    @PostMapping("/requirements")
    public ResponseEntity<DocumentAiResponse> generateRequirements(@PathVariable UUID pageId) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("Received requirements request for page: {}", pageId);
            DocumentAiResponse response = documentAiService.generateRequirements(pageId);
            log.info("Requirements generation completed in {}ms", System.currentTimeMillis() - startTime);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid page ID: {}", pageId, e);
            return ResponseEntity.badRequest().body(DocumentAiResponse.error("Page not found: " + pageId));
        } catch (IllegalStateException e) {
            log.warn("Invalid page state: {}", pageId, e);
            return ResponseEntity.badRequest().body(DocumentAiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Requirements generation failed for {}: {}", pageId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(DocumentAiResponse.error("Requirements generation failed: " + e.getMessage()));
        }
    }
}
