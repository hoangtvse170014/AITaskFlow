package com.taskflow.service;

import com.taskflow.dto.request.CreateBlockRequest;
import com.taskflow.dto.request.ReorderBlockRequest;
import com.taskflow.dto.request.UpdateBlockRequest;
import com.taskflow.dto.response.BlockResponse;

import java.util.List;
import java.util.UUID;

public interface BlockService {

    List<BlockResponse> getBlocksByPageId(UUID pageId);

    BlockResponse createBlock(UUID pageId, CreateBlockRequest request);

    BlockResponse updateBlock(UUID pageId, UUID blockId, UpdateBlockRequest request);

    void deleteBlock(UUID pageId, UUID blockId);

    BlockResponse moveBlock(UUID pageId, ReorderBlockRequest request);

    List<BlockResponse> duplicateBlocks(UUID pageId, List<String> blockIds);
}
