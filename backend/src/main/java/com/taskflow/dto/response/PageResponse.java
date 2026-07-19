package com.taskflow.dto.response;

import com.taskflow.entity.Page;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse {

    private String id;
    private String workspaceId;
    private String parentId;
    private String title;
    private String icon;
    private String coverUrl;
    private String slug;
    private boolean isPublic;
    private boolean isArchived;
    private Boolean isFavorite;
    private Integer favoriteOrder;
    private Integer sidebarOrder;
    private UserResponse createdBy;
    private UserResponse updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<PageResponse> children;
    private int childCount;
    private int blockCount;

    public static PageResponse fromEntity(Page page) {
        return fromEntity(page, false);
    }

    public static PageResponse fromEntity(Page page, boolean includeChildren) {
        return fromEntity(page, includeChildren, false);
    }

    public static PageResponse fromEntity(Page page, boolean includeChildren, boolean includeBlocks) {
        PageResponseBuilder builder = PageResponse.builder()
                .id(page.getId().toString())
                .workspaceId(page.getWorkspace().getId().toString())
                .parentId(page.getParent() != null ? page.getParent().getId().toString() : null)
                .title(page.getTitle())
                .icon(page.getIcon())
                .coverUrl(page.getCoverUrl())
                .slug(page.getSlug())
                .isPublic(page.getIsPublic())
                .isArchived(page.getIsArchived())
                .isFavorite(null)
                .favoriteOrder(page.getFavoriteOrder())
                .sidebarOrder(page.getSidebarOrder())
                .createdBy(UserResponse.fromEntity(page.getCreatedBy()))
                .updatedBy(page.getUpdatedBy() != null ? UserResponse.fromEntity(page.getUpdatedBy()) : null)
                .createdAt(page.getCreatedAt())
                .updatedAt(page.getUpdatedAt())
                .childCount(page.getChildren() != null ? page.getChildren().size() : 0)
                .blockCount(page.getBlocks() != null ? page.getBlocks().size() : 0);

        if (includeChildren && page.getChildren() != null) {
            builder.children(page.getChildren().stream()
                    .map(child -> fromEntity(child, true, false))
                    .collect(Collectors.toList()));
        }

        return builder.build();
    }
}
