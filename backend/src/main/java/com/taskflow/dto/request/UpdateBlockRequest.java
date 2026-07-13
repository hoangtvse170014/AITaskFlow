package com.taskflow.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBlockRequest {

    private String content;

    private String properties;

    private Integer orderIndex;

    private Boolean isCollapsed;

    private String parentBlockId;
}
