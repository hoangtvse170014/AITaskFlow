package com.taskflow.service;

import com.taskflow.dto.response.SearchResultResponse;
import com.taskflow.entity.SearchHistory;
import com.taskflow.entity.*;
import com.taskflow.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final PageRepository pageRepository;
    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final SearchHistoryRepository searchHistoryRepository;

    @Transactional
    public SearchResultResponse search(UUID workspaceId, String query, String entityType, UUID userId) {
        SearchResultResponse response = new SearchResultResponse();
        response.setQuery(query);
        response.setTimestamp(java.time.LocalDateTime.now().toString());

        if (query == null || query.trim().isEmpty()) {
            return response;
        }

        String searchQuery = query.trim().toLowerCase();

        if (entityType == null || entityType.isEmpty() || entityType.contains("task")) {
            response.setTasks(searchTasks(workspaceId, searchQuery));
        }
        if (entityType == null || entityType.isEmpty() || entityType.contains("page")) {
            response.setPages(searchPages(workspaceId, searchQuery));
        }
        if (entityType == null || entityType.isEmpty() || entityType.contains("project")) {
            response.setProjects(searchProjects(workspaceId, searchQuery));
        }
        if (entityType == null || entityType.isEmpty() || entityType.contains("member")) {
            response.setMembers(searchMembers(searchQuery));
        }

        // Save search history
        if (userId != null && response.getTotalCount() > 0) {
            saveSearchHistory(userId, query, response.getTotalCount());
        }

        return response;
    }

    private List<SearchResultResponse.TaskResult> searchTasks(UUID workspaceId, String query) {
        return taskRepository.findAll().stream()
                .filter(task -> {
                    String title = task.getTitle() != null ? task.getTitle().toLowerCase() : "";
                    String description = task.getDescription() != null ? task.getDescription().toLowerCase() : "";
                    return title.contains(query) || description.contains(query) ||
                           fuzzyMatch(title, query) || fuzzyMatch(description, query);
                })
                .filter(task -> task.getProject() != null && 
                       task.getProject().getWorkspace() != null &&
                       task.getProject().getWorkspace().getId().equals(workspaceId))
                .limit(20)
                .map(task -> {
                    SearchResultResponse.TaskResult result = new SearchResultResponse.TaskResult();
                    result.setId(task.getId().toString());
                    result.setTitle(task.getTitle());
                    result.setStatus(task.getStatus() != null ? task.getStatus().name() : null);
                    result.setPriority(task.getPriority() != null ? task.getPriority().name() : null);
                    result.setProjectName(task.getProject() != null ? task.getProject().getName() : null);
                    result.setProjectKey(task.getProject() != null ? task.getProject().getKey() : null);
                    result.setTaskKey(result.getProjectKey() + "-" + task.getTaskNumber());
                    result.setCreatedAt(task.getCreatedAt() != null ? task.getCreatedAt().toString() : null);
                    return result;
                })
                .collect(Collectors.toList());
    }

    private List<SearchResultResponse.PageResult> searchPages(UUID workspaceId, String query) {
        return pageRepository.findAll().stream()
                .filter(page -> {
                    String title = page.getTitle() != null ? page.getTitle().toLowerCase() : "";
                    return title.contains(query) || fuzzyMatch(title, query);
                })
                .filter(page -> page.getWorkspace() != null && 
                       page.getWorkspace().getId().equals(workspaceId))
                .limit(20)
                .map(page -> {
                    SearchResultResponse.PageResult result = new SearchResultResponse.PageResult();
                    result.setId(page.getId().toString());
                    result.setTitle(page.getTitle());
                    result.setIcon(page.getIcon());
                    result.setSlug(page.getSlug());
                    result.setCreatedAt(page.getCreatedAt() != null ? page.getCreatedAt().toString() : null);
                    return result;
                })
                .collect(Collectors.toList());
    }

    private List<SearchResultResponse.ProjectResult> searchProjects(UUID workspaceId, String query) {
        return projectRepository.findAllByWorkspaceIdOrderByCreatedAtDesc(workspaceId).stream()
                .filter(project -> {
                    String name = project.getName() != null ? project.getName().toLowerCase() : "";
                    String description = project.getDescription() != null ? project.getDescription().toLowerCase() : "";
                    String key = project.getKey() != null ? project.getKey().toLowerCase() : "";
                    return name.contains(query) || description.contains(query) || key.contains(query) ||
                           fuzzyMatch(name, query);
                })
                .limit(10)
                .map(project -> {
                    SearchResultResponse.ProjectResult result = new SearchResultResponse.ProjectResult();
                    result.setId(project.getId().toString());
                    result.setName(project.getName());
                    result.setKey(project.getKey());
                    result.setColor(project.getColor());
                    result.setDescription(project.getDescription());
                    return result;
                })
                .collect(Collectors.toList());
    }

    private List<SearchResultResponse.MemberResult> searchMembers(String query) {
        return userRepository.searchUsers(query).stream()
                .limit(10)
                .map(user -> {
                    SearchResultResponse.MemberResult result = new SearchResultResponse.MemberResult();
                    result.setId(user.getId().toString());
                    result.setFullName(user.getFullName());
                    result.setEmail(user.getEmail());
                    result.setAvatarUrl(user.getAvatarUrl());
                    return result;
                })
                .collect(Collectors.toList());
    }

    private boolean fuzzyMatch(String text, String query) {
        if (text == null || query == null) return false;
        int matches = 0;
        int queryIndex = 0;
        for (int i = 0; i < text.length() && queryIndex < query.length(); i++) {
            if (text.charAt(i) == query.charAt(queryIndex)) {
                matches++;
                queryIndex++;
            }
        }
        return matches >= query.length() * 0.7;
    }

    @Transactional
    public void saveSearchHistory(UUID userId, String query, int resultCount) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;

        SearchHistory history = SearchHistory.builder()
                .user(user)
                .query(query)
                .resultCount(resultCount)
                .build();

        searchHistoryRepository.save(history);
    }

    public List<String> getRecentSearches(UUID userId, int limit) {
        return searchHistoryRepository.findRecentQueriesByUserId(userId, PageRequest.of(0, limit));
    }

    @Transactional
    public void clearSearchHistory(UUID userId) {
        searchHistoryRepository.deleteByUserId(userId);
    }
}
