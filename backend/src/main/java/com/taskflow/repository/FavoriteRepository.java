package com.taskflow.repository;

import com.taskflow.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, UUID> {

    List<Favorite> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<Favorite> findByUserIdAndPageId(UUID userId, UUID pageId);

    boolean existsByUserIdAndPageId(UUID userId, UUID pageId);

    void deleteByUserIdAndPageId(UUID userId, UUID pageId);
}
