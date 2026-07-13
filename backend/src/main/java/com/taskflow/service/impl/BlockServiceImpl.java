package com.taskflow.service.impl;

import com.taskflow.dto.request.CreateBlockRequest;
import com.taskflow.dto.request.ReorderBlockRequest;
import com.taskflow.dto.request.UpdateBlockRequest;
import com.taskflow.dto.response.BlockResponse;
import com.taskflow.entity.Block;
import com.taskflow.entity.Page;
import com.taskflow.entity.User;
import com.taskflow.exception.ForbiddenException;
import com.taskflow.exception.ResourceNotFoundException;
import com.taskflow.repository.BlockRepository;
import com.taskflow.repository.PageRepository;
import com.taskflow.repository.UserRepository;
import com.taskflow.service.BlockService;
import com.taskflow.service.WorkspaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BlockServiceImpl implements BlockService {

    private final BlockRepository blockRepository;
    private final PageRepository pageRepository;
    private final UserRepository userRepository;
    private final WorkspaceService workspaceService;

    @Override
    public List<BlockResponse> getBlocksByPageId(UUID pageId) {
        User currentUser = getCurrentUser();
        Page page = pageRepository.findById(pageId)
                .orElseThrow(() -> new ResourceNotFoundException("Page", "id", pageId));

        if (!workspaceService.isWorkspaceMember(page.getWorkspace().getId(), currentUser.getId())) {
            throw new ForbiddenException("You are not a member of this workspace");
        }

        return blockRepository.findAllByPageIdOrderByPosition(pageId)
                .stream()
                .map(BlockResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public BlockResponse createBlock(UUID pageId, CreateBlockRequest request) {
        User currentUser = getCurrentUser();
        Page page = pageRepository.findById(pageId)
                .orElseThrow(() -> new ResourceNotFoundException("Page", "id", pageId));

        if (!workspaceService.isWorkspaceMember(page.getWorkspace().getId(), currentUser.getId())) {
            throw new ForbiddenException("You are not a member of this workspace");
        }

        Integer position;
        if (request.getOrderIndex() != null) {
            position = request.getOrderIndex();
            blockRepository.incrementOrderIndexFrom(pageId, position);
        } else {
            position = blockRepository.findMaxOrderIndex(pageId) + 1;
        }

        Block parentBlock = null;
        if (request.getParentBlockId() != null && !request.getParentBlockId().isEmpty()) {
            parentBlock = blockRepository.findById(UUID.fromString(request.getParentBlockId()))
                    .orElseThrow(() -> new ResourceNotFoundException("Parent block", "id", request.getParentBlockId()));
        }

        Block block = Block.builder()
                .page(page)
                .parentBlock(parentBlock)
                .type(request.getBlockType())
                .content(request.getContent())
                .position(position)
                .createdBy(currentUser)
                .updatedBy(currentUser)
                .build();

        block = blockRepository.save(block);

        page.setUpdatedBy(currentUser);
        pageRepository.save(page);

        return BlockResponse.fromEntity(block);
    }

    @Override
    @Transactional
    public BlockResponse updateBlock(UUID pageId, UUID blockId, UpdateBlockRequest request) {
        User currentUser = getCurrentUser();
        Page page = pageRepository.findById(pageId)
                .orElseThrow(() -> new ResourceNotFoundException("Page", "id", pageId));

        if (!workspaceService.isWorkspaceMember(page.getWorkspace().getId(), currentUser.getId())) {
            throw new ForbiddenException("You are not a member of this workspace");
        }

        Block block = blockRepository.findById(blockId)
                .orElseThrow(() -> new ResourceNotFoundException("Block", "id", blockId));

        if (!block.getPage().getId().equals(pageId)) {
            throw new ResourceNotFoundException("Block", "id", blockId);
        }

        if (request.getContent() != null) {
            block.setContent(request.getContent());
        }
        if (request.getOrderIndex() != null) {
            block.setPosition(request.getOrderIndex());
        }
        if (request.getParentBlockId() != null) {
            if (request.getParentBlockId().isEmpty()) {
                block.setParentBlock(null);
            } else {
                Block parentBlock = blockRepository.findById(UUID.fromString(request.getParentBlockId()))
                        .orElseThrow(() -> new ResourceNotFoundException("Parent block", "id", request.getParentBlockId()));
                block.setParentBlock(parentBlock);
            }
        }

        block.setUpdatedBy(currentUser);
        block = blockRepository.save(block);

        page.setUpdatedBy(currentUser);
        pageRepository.save(page);

        return BlockResponse.fromEntity(block);
    }

    @Override
    @Transactional
    public void deleteBlock(UUID pageId, UUID blockId) {
        User currentUser = getCurrentUser();
        Page page = pageRepository.findById(pageId)
                .orElseThrow(() -> new ResourceNotFoundException("Page", "id", pageId));

        if (!workspaceService.isWorkspaceMember(page.getWorkspace().getId(), currentUser.getId())) {
            throw new ForbiddenException("You are not a member of this workspace");
        }

        Block block = blockRepository.findById(blockId)
                .orElseThrow(() -> new ResourceNotFoundException("Block", "id", blockId));

        if (!block.getPage().getId().equals(pageId)) {
            throw new ResourceNotFoundException("Block", "id", blockId);
        }

        int deletedIndex = block.getPosition();
        blockRepository.delete(block);
        blockRepository.decrementOrderIndexAfter(pageId, deletedIndex);

        page.setUpdatedBy(currentUser);
        pageRepository.save(page);
    }

    @Override
    @Transactional
    public BlockResponse moveBlock(UUID pageId, ReorderBlockRequest request) {
        User currentUser = getCurrentUser();
        Page page = pageRepository.findById(pageId)
                .orElseThrow(() -> new ResourceNotFoundException("Page", "id", pageId));

        if (!workspaceService.isWorkspaceMember(page.getWorkspace().getId(), currentUser.getId())) {
            throw new ForbiddenException("You are not a member of this workspace");
        }

        Block block = blockRepository.findById(UUID.fromString(request.getBlockId()))
                .orElseThrow(() -> new ResourceNotFoundException("Block", "id", request.getBlockId()));

        if (!block.getPage().getId().equals(pageId)) {
            throw new ResourceNotFoundException("Block", "id", request.getBlockId());
        }

        int oldIndex = block.getPosition();
        int newIndex = request.getNewIndex() != null ? request.getNewIndex() : oldIndex;

        if (oldIndex != newIndex) {
            if (newIndex > oldIndex) {
                blockRepository.decrementOrderIndexAfter(pageId, oldIndex);
                blockRepository.incrementOrderIndexFrom(pageId, newIndex);
            } else {
                blockRepository.incrementOrderIndexFrom(pageId, newIndex);
            }
            block.setPosition(newIndex);
        }

        block.setUpdatedBy(currentUser);
        block = blockRepository.save(block);

        page.setUpdatedBy(currentUser);
        pageRepository.save(page);

        return BlockResponse.fromEntity(block);
    }

    @Override
    @Transactional
    public List<BlockResponse> duplicateBlocks(UUID pageId, List<String> blockIds) {
        User currentUser = getCurrentUser();
        Page page = pageRepository.findById(pageId)
                .orElseThrow(() -> new ResourceNotFoundException("Page", "id", pageId));

        if (!workspaceService.isWorkspaceMember(page.getWorkspace().getId(), currentUser.getId())) {
            throw new ForbiddenException("You are not a member of this workspace");
        }

        List<BlockResponse> duplicatedBlocks = new ArrayList<>();
        int maxOrder = blockRepository.findMaxOrderIndex(pageId);
        int offset = 1;

        for (String blockIdStr : blockIds) {
            Block original = blockRepository.findById(UUID.fromString(blockIdStr))
                    .orElseThrow(() -> new ResourceNotFoundException("Block", "id", blockIdStr));

            Block copy = Block.builder()
                    .page(page)
                    .parentBlock(original.getParentBlock())
                    .type(original.getType())
                    .content(original.getContent())
                    .position(maxOrder + offset)
                    .createdBy(currentUser)
                    .updatedBy(currentUser)
                    .build();

            copy = blockRepository.save(copy);
            duplicatedBlocks.add(BlockResponse.fromEntity(copy));
            offset++;
        }

        page.setUpdatedBy(currentUser);
        pageRepository.save(page);

        return duplicatedBlocks;
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }
}
