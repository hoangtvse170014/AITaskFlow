package com.taskflow.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResponse {

    private List<PageSearchResult> pages;
    private int totalCount;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PageSearchResult {
        private String id;
        private String title;
        private String icon;
        private String workspaceId;
        private String parentId;
        private String matchType;
        private String snippet;
    }
}
