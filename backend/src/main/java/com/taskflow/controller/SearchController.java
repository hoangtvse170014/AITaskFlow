package com.taskflow.controller;

import com.taskflow.dto.response.ApiResponse;
import com.taskflow.dto.response.SearchResultResponse;
import com.taskflow.entity.User;
import com.taskflow.exception.ResourceNotFoundException;
import com.taskflow.repository.UserRepository;
import com.taskflow.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<SearchResultResponse>> search(
            @RequestParam UUID workspaceId,
            @RequestParam String q,
            @RequestParam(required = false) String entityType) {
        
        User currentUser = getCurrentUser();
        SearchResultResponse result = searchService.search(workspaceId, q, entityType, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/suggestions")
    public ResponseEntity<ApiResponse<Map<String, List<String>>>> getSuggestions(
            @RequestParam UUID workspaceId,
            @RequestParam String q) {
        
        List<String> recentSearches = searchService.getRecentSearches(getCurrentUser().getId(), 5);
        return ResponseEntity.ok(ApiResponse.success(Map.of("suggestions", recentSearches)));
    }

    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<Map<String, List<String>>>> getRecentSearches(
            @RequestParam(defaultValue = "10") int limit) {
        
        User currentUser = getCurrentUser();
        List<String> recentSearches = searchService.getRecentSearches(currentUser.getId(), limit);
        return ResponseEntity.ok(ApiResponse.success(Map.of("recentSearches", recentSearches)));
    }

    @DeleteMapping("/history")
    public ResponseEntity<ApiResponse<Void>> clearSearchHistory() {
        User currentUser = getCurrentUser();
        searchService.clearSearchHistory(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }
}
