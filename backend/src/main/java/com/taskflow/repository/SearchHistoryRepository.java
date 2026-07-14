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

    /**
     * Returns the most recent distinct queries for a user. PostgreSQL does
     * not allow ORDER BY on a column that is not part of the SELECT DISTINCT
     * list, so we use a sub-query: pick the max(created_at) per query, then
     * select distinct queries ordered by that max timestamp. Native SQL is
     * required because JPQL cannot express this cleanly.
     */
    @Query(value = "SELECT q FROM (" +
            "  SELECT s.query AS q, MAX(s.created_at) AS last_used " +
            "  FROM search_history s " +
            "  WHERE s.user_id = :userId " +
            "  GROUP BY s.query" +
            ") AS grouped " +
            "ORDER BY grouped.last_used DESC " +
            "LIMIT :#{#pageable.pageSize}",
            nativeQuery = true)
    List<String> findRecentQueriesByUserId(@Param("userId") UUID userId, Pageable pageable);

    List<SearchHistory> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    void deleteByUserId(UUID userId);
}
