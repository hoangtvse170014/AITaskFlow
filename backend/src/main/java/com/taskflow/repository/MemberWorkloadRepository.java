package com.taskflow.repository;

import com.taskflow.entity.MemberWorkload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MemberWorkloadRepository extends JpaRepository<MemberWorkload, UUID> {

    List<MemberWorkload> findByMemberIdOrderByDateDesc(UUID memberId);

    List<MemberWorkload> findByWorkspaceIdOrderByDateDesc(UUID workspaceId);

    Optional<MemberWorkload> findByMemberIdAndDate(UUID memberId, LocalDate date);

    @Query("SELECT mw FROM MemberWorkload mw WHERE mw.member.workspace.id = :workspaceId AND mw.date BETWEEN :startDate AND :endDate ORDER BY mw.date DESC")
    List<MemberWorkload> findByWorkspaceIdAndDateRange(
            @Param("workspaceId") UUID workspaceId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT mw FROM MemberWorkload mw WHERE mw.member.id = :memberId AND mw.date BETWEEN :startDate AND :endDate ORDER BY mw.date DESC")
    List<MemberWorkload> findByMemberIdAndDateRange(
            @Param("memberId") UUID memberId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT AVG(mw.workloadPercentage) FROM MemberWorkload mw WHERE mw.member.workspace.id = :workspaceId AND mw.date >= :startDate")
    Double findAverageWorkloadByWorkspace(@Param("workspaceId") UUID workspaceId, @Param("startDate") LocalDate startDate);

    @Query("SELECT COUNT(mw) FROM MemberWorkload mw WHERE mw.member.workspace.id = :workspaceId AND mw.status = :status AND mw.date >= :startDate")
    Long countByWorkspaceAndStatus(@Param("workspaceId") UUID workspaceId, @Param("status") String status, @Param("startDate") LocalDate startDate);
}
