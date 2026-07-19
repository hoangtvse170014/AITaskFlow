package com.taskflow.dto.response;

import com.taskflow.entity.Block;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockResponse {

    private String id;
    private String pageId;
    private String parentBlockId;
    private String blockType;
    private String content;
    private Integer position;
    private UserResponse createdBy;
    private UserResponse updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static BlockResponse fromEntity(Block block) {
        return BlockResponse.builder()
                .id(block.getId().toString())
                .pageId(block.getPage().getId().toString())
                .parentBlockId(block.getParentBlock() != null ? block.getParentBlock().getId().toString() : null)
                .blockType(block.getType())
                .content(block.getContent())
                .position(block.getPosition())
                .createdBy(UserResponse.fromEntity(block.getCreatedBy()))
                .updatedBy(block.getUpdatedBy() != null ? UserResponse.fromEntity(block.getUpdatedBy()) : null)
                .createdAt(block.getCreatedAt())
                .updatedAt(block.getUpdatedAt())
                .build();
    }
}
