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
public class ReorderBlockRequest {

    @NotBlank(message = "Block ID is required")
    private String blockId;

    private String afterBlockId;

    private Integer newIndex;
}
