package com.taskflow.repository;

import com.taskflow.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CommentRepository extends JpaRepository<Comment, UUID> {

    List<Comment> findAllByPageIdOrderByCreatedAtAsc(UUID pageId);

    List<Comment> findAllByBlockIdOrderByCreatedAtAsc(UUID blockId);

    int countByPageId(UUID pageId);
}
