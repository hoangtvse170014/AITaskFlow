package com.taskflow.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageDetailResponse {

    private PageResponse page;
    private List<BlockResponse> blocks;
    private LocalDateTime lastEditedAt;
    private String lastEditedBy;
}
