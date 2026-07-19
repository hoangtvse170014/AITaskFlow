package com.taskflow.service.impl;

import com.taskflow.dto.request.CreatePageRequest;
import com.taskflow.dto.request.UpdatePageRequest;
import com.taskflow.dto.response.*;
import com.taskflow.entity.*;
import com.taskflow.exception.BadRequestException;
import com.taskflow.exception.ForbiddenException;
import com.taskflow.exception.ResourceNotFoundException;
import com.taskflow.repository.*;
import com.taskflow.service.PageService;
import com.taskflow.service.WorkspaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PageServiceImpl implements PageService {

    private final PageRepository pageRepository;
    private final BlockRepository blockRepository;
    private final FavoriteRepository favoriteRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final WorkspaceService workspaceService;
    private final UserRepository userRepository;

    @Override
    public List<PageResponse> getAllPages(UUID workspaceId) {
        User currentUser = getCurrentUser();
        if (!workspaceService.isWorkspaceMember(workspaceId, currentUser.getId())) {
            throw new ForbiddenException("You are not a member of this workspace");
        }
        return pageRepository.findAllByWorkspaceIdOrderBySidebarOrder(workspaceId)
                .stream()
                .map(page -> enrichPageResponse(page, currentUser.getId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<PageResponse> getPageTree(UUID workspaceId) {
        User currentUser = getCurrentUser();
        if (!workspaceService.isWorkspaceMember(workspaceId, currentUser.getId())) {
            throw new ForbiddenException("You are not a member of this workspace");
        }
        List<Page> rootPages = pageRepository.findRootPagesByWorkspace(workspaceId);
        return rootPages.stream()
                .map(page -> buildPageTree(page, currentUser.getId()))
                .collect(Collectors.toList());
    }

    @Override
    public PageDetailResponse getPageById(UUID workspaceId, UUID pageId) {
        User currentUser = getCurrentUser();
        if (!workspaceService.isWorkspaceMember(workspaceId, currentUser.getId())) {
            throw new ForbiddenException("You are not a member of this workspace");
        }
        Page page = pageRepository.findByIdAndWorkspaceId(pageId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Page", "id", pageId));
        
        List<Block> blocks = blockRepository.findAllByPageIdOrderByPosition(pageId);
        List<BlockResponse> blockResponses = blocks.stream()
                .map(BlockResponse::fromEntity)
                .collect(Collectors.toList());

        return PageDetailResponse.builder()
                .page(enrichPageResponse(page, currentUser.getId()))
                .blocks(blockResponses)
                .lastEditedAt(page.getUpdatedAt())
                .lastEditedBy(page.getUpdatedBy() != null ? page.getUpdatedBy().getFullName() : null)
                .build();
    }

    @Override
    @Transactional
    public PageResponse createPage(UUID workspaceId, CreatePageRequest request) {
        User currentUser = getCurrentUser();
        if (!workspaceService.isWorkspaceMember(workspaceId, currentUser.getId())) {
            throw new ForbiddenException("You are not a member of this workspace");
        }
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace", "id", workspaceId));

        Page page = Page.builder()
                .workspace(workspace)
                .title(request.getTitle())
                .icon(request.getIcon())
                .coverUrl(request.getCoverUrl())
                .slug(request.getSlug())
                .isPublic(request.getIsPublic() != null ? request.getIsPublic() : false)
                .isArchived(false)
                .createdBy(currentUser)
                .updatedBy(currentUser)
                .build();

        if (request.getParentId() != null && !request.getParentId().isEmpty()) {
            Page parent = pageRepository.findByIdAndWorkspaceId(UUID.fromString(request.getParentId()), workspaceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Parent page", "id", request.getParentId()));
            page.setParent(parent);
            page.setSidebarOrder(pageRepository.findMaxSidebarOrderForChildPages(parent.getId()) + 1);
        } else {
            Integer maxOrder = pageRepository.findMaxSidebarOrderForRootPages(workspaceId);
            page.setSidebarOrder(maxOrder + 1);
        }

        page = pageRepository.save(page);
        return enrichPageResponse(page, currentUser.getId());
    }

    @Override
    @Transactional
    public PageResponse updatePage(UUID workspaceId, UUID pageId, UpdatePageRequest request) {
        User currentUser = getCurrentUser();
        if (!workspaceService.isWorkspaceMember(workspaceId, currentUser.getId())) {
            throw new ForbiddenException("You are not a member of this workspace");
        }
        Page page = pageRepository.findByIdAndWorkspaceId(pageId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Page", "id", pageId));

        if (request.getTitle() != null) {
            page.setTitle(request.getTitle());
        }
        if (request.getIcon() != null) {
            page.setIcon(request.getIcon());
        }
        if (request.getCoverUrl() != null) {
            page.setCoverUrl(request.getCoverUrl());
        }
        if (request.getSlug() != null) {
            page.setSlug(request.getSlug());
        }
        if (request.getIsPublic() != null) {
            page.setIsPublic(request.getIsPublic());
        }
        if (request.getIsArchived() != null) {
            page.setIsArchived(request.getIsArchived());
        }
        if (request.getSidebarOrder() != null) {
            page.setSidebarOrder(request.getSidebarOrder());
        }
        if (request.getParentId() != null) {
            if (request.getParentId().isEmpty()) {
                page.setParent(null);
                Integer maxOrder = pageRepository.findMaxSidebarOrderForRootPages(workspaceId);
                page.setSidebarOrder(maxOrder + 1);
            } else {
                Page parent = pageRepository.findByIdAndWorkspaceId(UUID.fromString(request.getParentId()), workspaceId)
                        .orElseThrow(() -> new ResourceNotFoundException("Parent page", "id", request.getParentId()));
                if (isDescendant(parent, pageId)) {
                    throw new BadRequestException("Cannot set a descendant page as parent");
                }
                page.setParent(parent);
            }
        }

        page.setUpdatedBy(currentUser);
        page = pageRepository.save(page);
        return enrichPageResponse(page, currentUser.getId());
    }

    @Override
    @Transactional
    public void deletePage(UUID workspaceId, UUID pageId) {
        User currentUser = getCurrentUser();
        if (!workspaceService.isWorkspaceMember(workspaceId, currentUser.getId())) {
            throw new ForbiddenException("You are not a member of this workspace");
        }
        Page page = pageRepository.findByIdAndWorkspaceId(pageId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Page", "id", pageId));
        pageRepository.delete(page);
    }

    @Override
    @Transactional
    public PageResponse archivePage(UUID workspaceId, UUID pageId) {
        User currentUser = getCurrentUser();
        if (!workspaceService.isWorkspaceMember(workspaceId, currentUser.getId())) {
            throw new ForbiddenException("You are not a member of this workspace");
        }
        Page page = pageRepository.findByIdAndWorkspaceId(pageId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Page", "id", pageId));
        page.setIsArchived(true);
        page.setUpdatedBy(currentUser);
        page = pageRepository.save(page);
        return enrichPageResponse(page, currentUser.getId());
    }

    @Override
    @Transactional
    public PageResponse restorePage(UUID workspaceId, UUID pageId) {
        User currentUser = getCurrentUser();
        if (!workspaceService.isWorkspaceMember(workspaceId, currentUser.getId())) {
            throw new ForbiddenException("You are not a member of this workspace");
        }
        Page page = pageRepository.findByIdAndWorkspaceId(pageId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Page", "id", pageId));
        page.setIsArchived(false);
        page.setUpdatedBy(currentUser);
        page = pageRepository.save(page);
        return enrichPageResponse(page, currentUser.getId());
    }

    @Override
    @Transactional
    public PageResponse movePage(UUID workspaceId, UUID pageId, String newParentId, Integer newOrder) {
        User currentUser = getCurrentUser();
        if (!workspaceService.isWorkspaceMember(workspaceId, currentUser.getId())) {
            throw new ForbiddenException("You are not a member of this workspace");
        }
        Page page = pageRepository.findByIdAndWorkspaceId(pageId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Page", "id", pageId));

        if (newParentId != null && !newParentId.isEmpty()) {
            Page parent = pageRepository.findByIdAndWorkspaceId(UUID.fromString(newParentId), workspaceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Parent page", "id", newParentId));
            if (isDescendant(parent, pageId)) {
                throw new BadRequestException("Cannot move page under its descendant");
            }
            page.setParent(parent);
        } else {
            page.setParent(null);
        }

        if (newOrder != null) {
            page.setSidebarOrder(newOrder);
        }

        page.setUpdatedBy(currentUser);
        page = pageRepository.save(page);
        return enrichPageResponse(page, currentUser.getId());
    }

    @Override
    @Transactional
    public PageResponse duplicatePage(UUID workspaceId, UUID pageId) {
        User currentUser = getCurrentUser();
        if (!workspaceService.isWorkspaceMember(workspaceId, currentUser.getId())) {
            throw new ForbiddenException("You are not a member of this workspace");
        }
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace", "id", workspaceId));
        Page original = pageRepository.findByIdAndWorkspaceId(pageId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Page", "id", pageId));

        Page copy = Page.builder()
                .workspace(workspace)
                .parent(original.getParent())
                .title(original.getTitle() + " (Copy)")
                .icon(original.getIcon())
                .coverUrl(original.getCoverUrl())
                .isPublic(original.getIsPublic())
                .isArchived(false)
                .createdBy(currentUser)
                .updatedBy(currentUser)
                .build();

        Integer maxOrder = original.getParent() != null 
                ? pageRepository.findMaxSidebarOrderForChildPages(original.getParent().getId())
                : pageRepository.findMaxSidebarOrderForRootPages(workspaceId);
        copy.setSidebarOrder(maxOrder + 1);

        copy = pageRepository.save(copy);

        List<Block> originalBlocks = blockRepository.findAllByPageIdOrderByPosition(pageId);
        for (Block originalBlock : originalBlocks) {
            Block blockCopy = Block.builder()
                    .page(copy)
                    .parentBlock(originalBlock.getParentBlock())
                    .type(originalBlock.getType())
                    .content(originalBlock.getContent())
                    .position(originalBlock.getPosition())
                    .createdBy(currentUser)
                    .updatedBy(currentUser)
                    .build();
            blockRepository.save(blockCopy);
        }

        return enrichPageResponse(copy, currentUser.getId());
    }

    @Override
    @Transactional
    public PageResponse toggleFavorite(UUID pageId) {
        User currentUser = getCurrentUser();
        Page page = pageRepository.findById(pageId)
                .orElseThrow(() -> new ResourceNotFoundException("Page", "id", pageId));

        Optional<Favorite> existing = favoriteRepository.findByUserIdAndPageId(currentUser.getId(), pageId);
        if (existing.isPresent()) {
            favoriteRepository.delete(existing.get());
        } else {
            Favorite favorite = Favorite.builder()
                    .user(currentUser)
                    .page(page)
                    .build();
            favoriteRepository.save(favorite);
        }

        return enrichPageResponse(page, currentUser.getId());
    }

    @Override
    public List<PageResponse> getFavoritePages() {
        User currentUser = getCurrentUser();
        return favoriteRepository.findAllByUserIdOrderByCreatedAtDesc(currentUser.getId())
                .stream()
                .map(fav -> enrichPageResponse(fav.getPage(), currentUser.getId()))
                .collect(Collectors.toList());
    }

    @Override
    public SearchResponse searchPages(UUID workspaceId, String query) {
        User currentUser = getCurrentUser();
        if (!workspaceService.isWorkspaceMember(workspaceId, currentUser.getId())) {
            throw new ForbiddenException("You are not a member of this workspace");
        }

        List<Page> titleMatches = pageRepository.searchByTitle(workspaceId, query);
        List<Page> contentMatches = pageRepository.searchByContent(workspaceId, query);

        Set<Page> uniquePages = new LinkedHashSet<>();
        uniquePages.addAll(titleMatches);
        uniquePages.addAll(contentMatches);

        List<SearchResponse.PageSearchResult> results = uniquePages.stream()
                .map(page -> SearchResponse.PageSearchResult.builder()
                        .id(page.getId().toString())
                        .title(page.getTitle())
                        .icon(page.getIcon())
                        .workspaceId(page.getWorkspace().getId().toString())
                        .parentId(page.getParent() != null ? page.getParent().getId().toString() : null)
                        .matchType(titleMatches.contains(page) ? "title" : "content")
                        .build())
                .collect(Collectors.toList());

        return SearchResponse.builder()
                .pages(results)
                .totalCount(results.size())
                .build();
    }

    private PageResponse enrichPageResponse(Page page, UUID userId) {
        boolean isFavorite = favoriteRepository.existsByUserIdAndPageId(userId, page.getId());
        PageResponse response = PageResponse.fromEntity(page);
        response.setIsFavorite(isFavorite);
        return response;
    }

    private PageResponse buildPageTree(Page page, UUID userId) {
        PageResponse response = enrichPageResponse(page, userId);
        List<Page> children = pageRepository.findAllByParentIdOrderBySidebarOrder(page.getId());
        if (!children.isEmpty()) {
            response.setChildren(children.stream()
                    .map(child -> buildPageTree(child, userId))
                    .collect(Collectors.toList()));
        }
        return response;
    }

    private boolean isDescendant(Page potentialDescendant, UUID pageId) {
        if (potentialDescendant.getId().equals(pageId)) {
            return true;
        }
        List<Page> children = pageRepository.findAllByParentIdOrderBySidebarOrder(potentialDescendant.getId());
        for (Page child : children) {
            if (isDescendant(child, pageId)) {
                return true;
            }
        }
        return false;
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }
}
