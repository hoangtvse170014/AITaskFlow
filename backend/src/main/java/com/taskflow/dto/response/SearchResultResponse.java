package com.taskflow.dto.response;

import lombok.*;

import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchResultResponse {
    
    private String query;
    private String timestamp;
    private List<TaskResult> tasks;
    private List<PageResult> pages;
    private List<ProjectResult> projects;
    private List<MemberResult> members;
    private List<String> suggestions;

    public int getTotalCount() {
        int count = 0;
        if (tasks != null) count += tasks.size();
        if (pages != null) count += pages.size();
        if (projects != null) count += projects.size();
        if (members != null) count += members.size();
        return count;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TaskResult {
        private String id;
        private String title;
        private String status;
        private String priority;
        private String projectName;
        private String projectKey;
        private String taskKey;
        private String createdAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PageResult {
        private String id;
        private String title;
        private String icon;
        private String slug;
        private String createdAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProjectResult {
        private String id;
        private String name;
        private String key;
        private String color;
        private String description;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MemberResult {
        private String id;
        private String fullName;
        private String email;
        private String avatarUrl;
    }
}
