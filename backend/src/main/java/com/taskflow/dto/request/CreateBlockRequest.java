package com.taskflow.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBlockRequest {

    @NotBlank(message = "Block type is required")
    private String blockType;

    private String content;

    private String properties;

    private String parentBlockId;

    private Integer orderIndex;
}
