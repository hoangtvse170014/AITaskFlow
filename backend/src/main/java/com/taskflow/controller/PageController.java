package com.taskflow.controller;

import com.taskflow.dto.request.CreatePageRequest;
import com.taskflow.dto.request.UpdatePageRequest;
import com.taskflow.dto.response.ApiResponse;
import com.taskflow.dto.response.PageDetailResponse;
import com.taskflow.dto.response.PageResponse;
import com.taskflow.dto.response.SearchResponse;
import com.taskflow.service.PageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/pages")
@RequiredArgsConstructor
public class PageController {

    private final PageService pageService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PageResponse>>> getAllPages(@PathVariable UUID workspaceId) {
        List<PageResponse> pages = pageService.getAllPages(workspaceId);
        return ResponseEntity.ok(ApiResponse.success(pages));
    }

    @GetMapping("/tree")
    public ResponseEntity<ApiResponse<List<PageResponse>>> getPageTree(@PathVariable UUID workspaceId) {
        List<PageResponse> pages = pageService.getPageTree(workspaceId);
        return ResponseEntity.ok(ApiResponse.success(pages));
    }

    @GetMapping("/{pageId}")
    public ResponseEntity<ApiResponse<PageDetailResponse>> getPageById(
            @PathVariable UUID workspaceId,
            @PathVariable UUID pageId) {
        PageDetailResponse page = pageService.getPageById(workspaceId, pageId);
        return ResponseEntity.ok(ApiResponse.success(page));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PageResponse>> createPage(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody CreatePageRequest request) {
        PageResponse page = pageService.createPage(workspaceId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(page, "Page created successfully"));
    }

    @PutMapping("/{pageId}")
    public ResponseEntity<ApiResponse<PageResponse>> updatePage(
            @PathVariable UUID workspaceId,
            @PathVariable UUID pageId,
            @Valid @RequestBody UpdatePageRequest request) {
        PageResponse page = pageService.updatePage(workspaceId, pageId, request);
        return ResponseEntity.ok(ApiResponse.success(page, "Page updated successfully"));
    }

    @DeleteMapping("/{pageId}")
    public ResponseEntity<ApiResponse<Void>> deletePage(
            @PathVariable UUID workspaceId,
            @PathVariable UUID pageId) {
        pageService.deletePage(workspaceId, pageId);
        return ResponseEntity.ok(ApiResponse.success(null, "Page deleted successfully"));
    }

    @PostMapping("/{pageId}/archive")
    public ResponseEntity<ApiResponse<PageResponse>> archivePage(
            @PathVariable UUID workspaceId,
            @PathVariable UUID pageId) {
        PageResponse page = pageService.archivePage(workspaceId, pageId);
        return ResponseEntity.ok(ApiResponse.success(page, "Page archived successfully"));
    }

    @PostMapping("/{pageId}/restore")
    public ResponseEntity<ApiResponse<PageResponse>> restorePage(
            @PathVariable UUID workspaceId,
            @PathVariable UUID pageId) {
        PageResponse page = pageService.restorePage(workspaceId, pageId);
        return ResponseEntity.ok(ApiResponse.success(page, "Page restored successfully"));
    }

    @PostMapping("/{pageId}/duplicate")
    public ResponseEntity<ApiResponse<PageResponse>> duplicatePage(
            @PathVariable UUID workspaceId,
            @PathVariable UUID pageId) {
        PageResponse page = pageService.duplicatePage(workspaceId, pageId);
        return ResponseEntity.ok(ApiResponse.success(page, "Page duplicated successfully"));
    }

    @PostMapping("/{pageId}/favorite")
    public ResponseEntity<ApiResponse<PageResponse>> toggleFavorite(@PathVariable UUID pageId) {
        PageResponse page = pageService.toggleFavorite(pageId);
        return ResponseEntity.ok(ApiResponse.success(page));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<SearchResponse>> searchPages(
            @PathVariable UUID workspaceId,
            @RequestParam String query) {
        SearchResponse results = pageService.searchPages(workspaceId, query);
        return ResponseEntity.ok(ApiResponse.success(results));
    }
}
