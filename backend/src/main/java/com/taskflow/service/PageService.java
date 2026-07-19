package com.taskflow.service;

import com.taskflow.dto.request.CreatePageRequest;
import com.taskflow.dto.request.UpdatePageRequest;
import com.taskflow.dto.response.PageDetailResponse;
import com.taskflow.dto.response.PageResponse;
import com.taskflow.dto.response.SearchResponse;

import java.util.List;
import java.util.UUID;

public interface PageService {

    List<PageResponse> getAllPages(UUID workspaceId);

    List<PageResponse> getPageTree(UUID workspaceId);

    PageDetailResponse getPageById(UUID workspaceId, UUID pageId);

    PageResponse createPage(UUID workspaceId, CreatePageRequest request);

    PageResponse updatePage(UUID workspaceId, UUID pageId, UpdatePageRequest request);

    void deletePage(UUID workspaceId, UUID pageId);

    PageResponse archivePage(UUID workspaceId, UUID pageId);

    PageResponse restorePage(UUID workspaceId, UUID pageId);

    PageResponse movePage(UUID workspaceId, UUID pageId, String newParentId, Integer newOrder);

    PageResponse duplicatePage(UUID workspaceId, UUID pageId);

    PageResponse toggleFavorite(UUID pageId);

    List<PageResponse> getFavoritePages();

    SearchResponse searchPages(UUID workspaceId, String query);
}
