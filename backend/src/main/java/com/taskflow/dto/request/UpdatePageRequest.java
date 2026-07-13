package com.taskflow.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePageRequest {

    @Size(max = 255, message = "Title must be less than 255 characters")
    private String title;

    private String icon;

    private String coverUrl;

    private String slug;

    private String parentId;

    private Boolean isPublic;

    private Boolean isArchived;

    private Integer sidebarOrder;
}
