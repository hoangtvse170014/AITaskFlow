package com.taskflow.service;

import com.taskflow.dto.response.WorkloadResponse;
import com.taskflow.dto.response.WorkloadResponse.*;
import com.taskflow.entity.*;
import com.taskflow.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkloadService {

    private final MemberWorkloadRepository workloadRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final TaskRepository taskRepository;
    private final WorkspaceRepository workspaceRepository;

    public WorkloadResponse getWorkloadOverview(UUID workspaceId) {
        WorkloadResponse response = new WorkloadResponse();
        response.setWorkspaceId(workspaceId);
        
        List<WorkspaceMember> members = memberRepository.findAllByWorkspaceId(workspaceId);
        List<MemberWorkloadData> memberData = new ArrayList<>();
        
        LocalDate today = LocalDate.now();
        LocalDate weekAgo = today.minusDays(6);
        
        int totalOverloaded = 0;
        int totalUnderutilized = 0;
        int totalBalanced = 0;
        double totalHoursLogged = 0;
        double totalHoursEstimated = 0;
        
        for (WorkspaceMember member : members) {
            UUID memberId = member.getUser().getId();
            
            List<Task> memberTasks = taskRepository.findAll().stream()
                    .filter(t -> t.getAssignee() != null && t.getAssignee().getId().equals(memberId))
                    .collect(Collectors.toList());
            
            int openTasks = (int) memberTasks.stream().filter(t -> t.getStatus() != TaskStatus.DONE).count();
            int inProgressTasks = (int) memberTasks.stream().filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS).count();
            int completedTasks = (int) memberTasks.stream().filter(t -> t.getStatus() == TaskStatus.DONE).count();
            
            String status;
            if (openTasks == 0) {
                status = "UNDERUTILIZED";
                totalUnderutilized++;
            } else if (openTasks <= 5) {
                status = "BALANCED";
                totalBalanced++;
            } else {
                status = "OVERLOADED";
                totalOverloaded++;
            }
            
            int workloadPercentage = Math.min(100, (openTasks * 20));
            
            List<DailyWorkload> weeklyWorkload = new ArrayList<>();
            for (int i = 6; i >= 0; i--) {
                LocalDate date = today.minusDays(i);
                List<Task> tasksOnDate = memberTasks.stream()
                        .filter(t -> t.getCreatedAt() != null && 
                                   t.getCreatedAt().toLocalDate().equals(date))
                        .collect(Collectors.toList());
                
                int completedOnDate = (int) memberTasks.stream()
                        .filter(t -> t.getStatus() == TaskStatus.DONE &&
                                   t.getUpdatedAt() != null &&
                                   t.getUpdatedAt().toLocalDate().equals(date))
                        .count();
                
                weeklyWorkload.add(DailyWorkload.builder()
                        .date(date)
                        .openTasks(openTasks)
                        .completedTasks(completedOnDate)
                        .workloadPercentage(workloadPercentage)
                        .build());
            }
            
            memberData.add(MemberWorkloadData.builder()
                    .memberId(memberId)
                    .memberName(member.getUser().getFullName())
                    .email(member.getUser().getEmail())
                    .avatarUrl(member.getUser().getAvatarUrl())
                    .role(member.getRole() != null ? member.getRole().getName() : "MEMBER")
                    .openTasks(openTasks)
                    .inProgressTasks(inProgressTasks)
                    .completedTasks(completedTasks)
                    .blockedTasks(0)
                    .hoursEstimated(0.0)
                    .hoursLogged(0.0)
                    .workloadPercentage(workloadPercentage)
                    .status(status)
                    .weeklyWorkload(weeklyWorkload)
                    .build());
        }
        
        response.setMembers(memberData);
        response.setHeatmap(generateHeatmap(memberData, weekAgo, today));
        
        WorkloadSummary summary = WorkloadSummary.builder()
                .averageWorkload(memberData.isEmpty() ? 0 : 
                        memberData.stream().mapToInt(MemberWorkloadData::getWorkloadPercentage).average().orElse(0))
                .overloadedMembers(totalOverloaded)
                .underutilizedMembers(totalUnderutilized)
                .balancedMembers(totalBalanced)
                .totalHoursLogged(totalHoursLogged)
                .totalHoursEstimated(totalHoursEstimated)
                .build();
        response.setSummary(summary);
        
        return response;
    }

    private WorkloadHeatmap generateHeatmap(List<MemberWorkloadData> members, LocalDate startDate, LocalDate endDate) {
        List<String> dates = new ArrayList<>();
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            dates.add(current.toString());
            current = current.plusDays(1);
        }
        
        List<HeatmapRow> rows = members.stream()
                .map(member -> {
                    List<Integer> values = new ArrayList<>();
                    List<String> statuses = new ArrayList<>();
                    for (DailyWorkload daily : member.getWeeklyWorkload()) {
                        values.add(daily.getWorkloadPercentage());
                        statuses.add(daily.getWorkloadPercentage() > 80 ? "overloaded" :
                                    daily.getWorkloadPercentage() > 40 ? "balanced" : "light");
                    }
                    return HeatmapRow.builder()
                            .memberId(member.getMemberId())
                            .memberName(member.getMemberName())
                            .avatarUrl(member.getAvatarUrl())
                            .values(values)
                            .statuses(statuses)
                            .build();
                })
                .collect(Collectors.toList());
        
        return WorkloadHeatmap.builder()
                .dates(dates)
                .rows(rows)
                .build();
    }

    public void recalculateWorkloads(UUID workspaceId) {
        List<WorkspaceMember> members = memberRepository.findAllByWorkspaceId(workspaceId);
        LocalDate today = LocalDate.now();
        
        for (WorkspaceMember member : members) {
            UUID memberId = member.getUser().getId();
            
            List<Task> memberTasks = taskRepository.findAll().stream()
                    .filter(t -> t.getAssignee() != null && t.getAssignee().getId().equals(memberId))
                    .collect(Collectors.toList());
            
            int openTasks = (int) memberTasks.stream().filter(t -> t.getStatus() != TaskStatus.DONE).count();
            int inProgressTasks = (int) memberTasks.stream().filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS).count();
            int completedTasks = (int) memberTasks.stream().filter(t -> t.getStatus() == TaskStatus.DONE).count();
            
            String status;
            if (openTasks == 0) status = "UNDERUTILIZED";
            else if (openTasks <= 5) status = "BALANCED";
            else status = "OVERLOADED";
            
            int workloadPercentage = Math.min(100, (openTasks * 20));
            
            MemberWorkload workload = workloadRepository.findByMemberIdAndDate(memberId, today)
                    .orElse(MemberWorkload.builder()
                            .member(member)
                            .workspaceId(workspaceId)
                            .date(today)
                            .build());
            
            workload.setOpenTasks(openTasks);
            workload.setInProgressTasks(inProgressTasks);
            workload.setCompletedTasks(completedTasks);
            workload.setWorkloadPercentage(workloadPercentage);
            workload.setStatus(status);
            
            workloadRepository.save(workload);
        }
    }
}
