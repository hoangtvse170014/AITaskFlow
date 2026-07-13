package com.taskflow.repository;

import com.taskflow.entity.Block;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BlockRepository extends JpaRepository<Block, UUID> {

    List<Block> findAllByPageIdOrderByPosition(UUID pageId);

    @Query("SELECT COALESCE(MAX(b.position), -1) FROM Block b WHERE b.page.id = :pageId")
    Integer findMaxOrderIndex(@Param("pageId") UUID pageId);

    @Modifying
    @Query("UPDATE Block b SET b.position = b.position + 1 WHERE b.page.id = :pageId AND b.position >= :fromIndex")
    void incrementOrderIndexFrom(@Param("pageId") UUID pageId, @Param("fromIndex") int fromIndex);

    @Modifying
    @Query("UPDATE Block b SET b.position = b.position - 1 WHERE b.page.id = :pageId AND b.position > :fromIndex")
    void decrementOrderIndexAfter(@Param("pageId") UUID pageId, @Param("fromIndex") int fromIndex);

    void deleteAllByPageId(UUID pageId);
}
