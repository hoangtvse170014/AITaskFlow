package com.taskflow.controller;

import com.taskflow.dto.request.CreateBlockRequest;
import com.taskflow.dto.request.ReorderBlockRequest;
import com.taskflow.dto.request.UpdateBlockRequest;
import com.taskflow.dto.response.ApiResponse;
import com.taskflow.dto.response.BlockResponse;
import com.taskflow.service.BlockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/pages/{pageId}/blocks")
@RequiredArgsConstructor
public class BlockController {

    private final BlockService blockService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BlockResponse>>> getBlocks(@PathVariable UUID pageId) {
        List<BlockResponse> blocks = blockService.getBlocksByPageId(pageId);
        return ResponseEntity.ok(ApiResponse.success(blocks));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BlockResponse>> createBlock(
            @PathVariable UUID pageId,
            @Valid @RequestBody CreateBlockRequest request) {
        BlockResponse block = blockService.createBlock(pageId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(block, "Block created successfully"));
    }

    @PutMapping("/{blockId}")
    public ResponseEntity<ApiResponse<BlockResponse>> updateBlock(
            @PathVariable UUID pageId,
            @PathVariable UUID blockId,
            @Valid @RequestBody UpdateBlockRequest request) {
        BlockResponse block = blockService.updateBlock(pageId, blockId, request);
        return ResponseEntity.ok(ApiResponse.success(block, "Block updated successfully"));
    }

    @DeleteMapping("/{blockId}")
    public ResponseEntity<ApiResponse<Void>> deleteBlock(
            @PathVariable UUID pageId,
            @PathVariable UUID blockId) {
        blockService.deleteBlock(pageId, blockId);
        return ResponseEntity.ok(ApiResponse.success(null, "Block deleted successfully"));
    }

    @PostMapping("/reorder")
    public ResponseEntity<ApiResponse<BlockResponse>> reorderBlocks(
            @PathVariable UUID pageId,
            @Valid @RequestBody ReorderBlockRequest request) {
        BlockResponse block = blockService.moveBlock(pageId, request);
        return ResponseEntity.ok(ApiResponse.success(block, "Block reordered successfully"));
    }

    @PostMapping("/duplicate")
    public ResponseEntity<ApiResponse<List<BlockResponse>>> duplicateBlocks(
            @PathVariable UUID pageId,
            @RequestBody List<String> blockIds) {
        List<BlockResponse> blocks = blockService.duplicateBlocks(pageId, blockIds);
        return ResponseEntity.ok(ApiResponse.success(blocks, "Blocks duplicated successfully"));
    }
}
