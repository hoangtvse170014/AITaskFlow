package com.taskflow.repository;

import com.taskflow.entity.SearchHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SearchHistoryRepository extends JpaRepository<SearchHistory, UUID> {

    @Query("SELECT DISTINCT s.query FROM SearchHistory s WHERE s.user.id = :userId ORDER BY s.createdAt DESC")
    List<String> findRecentQueriesByUserId(@Param("userId") UUID userId, Pageable pageable);

    List<SearchHistory> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    void deleteByUserId(UUID userId);
}
