package com.taskflow.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.ai.dto.WorkspaceAnswerResponse;
import com.taskflow.ai.dto.WorkspaceAnswerResponse.RelatedItem;
import com.taskflow.ai.dto.WorkspaceSnapshot;
import com.taskflow.entity.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Deterministic, rules-based answer generator used as a safety net for:
 * <ul>
 *   <li>Demo mode (no Groq API key configured)</li>
 *   <li>LLM failures or timeouts</li>
 * </ul>
 *
 * <p>The whole point is to NEVER make things up: every number, name and date
 * in the output must be derivable from the {@link WorkspaceSnapshot} passed in.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WorkspaceAssistantLocalAnswerer {

    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    // =====================================================================
    // Public entry
    // =====================================================================

    public WorkspaceAnswerResponse answer(WorkspaceSnapshot snapshot,
                                         String question,
                                         QuestionIntentClassifier.Intent intent,
                                         long processingTimeMs) {
        try {
            String markdown = switch (intent) {
                case SUMMARIZE_WORKSPACE -> summarizeWorkspace(snapshot);
                case MOST_RISKY_PROJECT -> mostRiskyProject(snapshot);
                case WHO_IS_OVERLOADED -> whoIsOverloaded(snapshot);
                case WHAT_SHOULD_I_DO -> whatShouldIDo(snapshot);
                case GENERATE_SPRINT -> generateSprint(snapshot);
                case SUGGEST_ASSIGNEE -> suggestAssignee(snapshot, question);
                case WEEKLY_REPORT -> weeklyReport(snapshot);
                case DAILY_REPORT -> dailyReport(snapshot);
                case BLOCKERS -> blockersReport(snapshot);
                case UPCOMING_DEADLINES -> upcomingDeadlines(snapshot);
                case GOALS_PROGRESS -> goalsProgress(snapshot);
                case PROJECT_HEALTH -> projectHealth(snapshot);
                case GENERAL -> general(snapshot, question);
            };

            List<RelatedItem> relatedProjects = topProjects(snapshot, 5);
            List<RelatedItem> relatedTasks = topTasks(snapshot, 10);
            List<RelatedItem> relatedMembers = topMembers(snapshot, 5);
            List<RelatedItem> relatedGoals = topGoals(snapshot, 5);
            List<RelatedItem> relatedPages = topPages(snapshot, 5);
            List<String> sources = List.of(sourcesForIntent(intent));
            List<String> suggestions = suggestionsFor(intent, snapshot);

            return WorkspaceAnswerResponse.builder()
                    .answer(markdown)
                    .confidence(0.9)
                    .sources(sources)
                    .relatedProjects(relatedProjects)
                    .relatedTasks(relatedTasks)
                    .relatedMembers(relatedMembers)
                    .relatedGoals(relatedGoals)
                    .relatedPages(relatedPages)
                    .suggestions(suggestions)
                    .intent(intent.name())
                    .processingTimeMs(processingTimeMs)
                    .build();
        } catch (Exception e) {
            log.error("Local answerer failed", e);
            return WorkspaceAnswerResponse.builder()
                    .answer("I don't have enough workspace information to answer this question.")
                    .confidence(0.0)
                    .sources(List.of("error"))
                    .intent(intent.name())
                    .processingTimeMs(processingTimeMs)
                    .build();
        }
    }

    /** Used by the LLM mode to parse the model's JSON envelope into our DTO. */
    public WorkspaceAnswerResponse parseLlmEnvelope(String raw,
                                                    QuestionIntentClassifier.Intent intent,
                                                    long processingTimeMs) {
        if (raw == null || raw.isBlank()) {
            return emptyAnswer(intent, processingTimeMs);
        }
        try {
            String json = extractJson(raw);
            if (json == null) {
                return WorkspaceAnswerResponse.builder()
                        .answer(raw.trim())
                        .intent(intent.name())
                        .processingTimeMs(processingTimeMs)
                        .build();
            }
            JsonNode root = objectMapper.readTree(json);
            String answer = root.path("answer").asText("I don't have enough workspace information to answer this question.");
            double confidence = root.path("confidence").asDouble(0.7);

            return WorkspaceAnswerResponse.builder()
                    .answer(answer)
                    .confidence(confidence)
                    .sources(toStringList(root.path("sources")))
                    .relatedProjects(parseRelated(root.path("relatedProjects"), "project"))
                    .relatedTasks(parseRelated(root.path("relatedTasks"), "task"))
                    .relatedMembers(parseRelated(root.path("relatedMembers"), "member"))
                    .relatedGoals(parseRelated(root.path("relatedGoals"), "goal"))
                    .relatedPages(parseRelated(root.path("relatedPages"), "page"))
                    .suggestions(toStringList(root.path("suggestions")))
                    .intent(intent.name())
                    .processingTimeMs(processingTimeMs)
                    .build();
        } catch (Exception e) {
            log.warn("Failed to parse LLM JSON envelope, falling back to raw text: {}", e.getMessage());
            return WorkspaceAnswerResponse.builder()
                    .answer(raw)
                    .intent(intent.name())
                    .processingTimeMs(processingTimeMs)
                    .build();
        }
    }

    // =====================================================================
    // Intent-specific generators
    // =====================================================================

    private String summarizeWorkspace(WorkspaceSnapshot s) {
        StringBuilder sb = new StringBuilder();
        int memberCount = s.getMembers() != null ? s.getMembers().size() : 0;
        int projectCount = s.getProjects() != null ? s.getProjects().size() : 0;
        List<Task> tasks = nz(s.getTasks());
        int done = (int) tasks.stream().filter(t -> t.getStatus() == TaskStatus.DONE).count();
        int overdue = (int) tasks.stream().filter(Task::isOverdue).count();
        int unassigned = (int) tasks.stream().filter(t -> t.getAssignee() == null && t.getStatus() != TaskStatus.DONE).count();

        sb.append("# ").append(safeName(s.getWorkspace())).append("\n\n");
        sb.append("**Tổng quan workspace**\n\n");
        sb.append("- Thành viên đang hoạt động: **").append(memberCount).append("**\n");
        sb.append("- Dự án: **").append(projectCount).append("**\n");
        sb.append("- Tổng task: **").append(tasks.size()).append("** (đã xong **").append(done).append("**, quá hạn **").append(overdue).append("**, chưa giao **").append(unassigned).append("**)\n");

        if (s.getProjects() != null) {
            sb.append("\n## Dự án đang chạy\n");
            for (Project p : s.getProjects()) {
                sb.append("- **").append(safeName(p)).append("** [").append(safeKey(p)).append("]: ")
                        .append(projectProgress(p, tasks)).append('\n');
            }
        }

        sb.append('\n');
        sb.append("## Tình trạng team\n");
        appendTeamStatusTable(sb, s);

        sb.append("\n## Deadline sắp tới\n");
        appendUpcomingTable(sb, tasks, s.getToday(), 10);

        sb.append("\n## Đề xuất trọng tâm 1-2 tuần tới\n");
        sb.append("- Giải phóng các task quá hạn đang có assignee có workload > 80%.\n");
        sb.append("- Giao lại các task đang `UNASSIGNED` ở mức URGENT/HIGH.\n");
        sb.append("- Đánh giá lại tiến độ các dự án có tỉ lệ overdue > 20%.\n");
        return sb.toString();
    }

    private String mostRiskyProject(WorkspaceSnapshot s) {
        List<Task> tasks = nz(s.getTasks());
        if (s.getProjects() == null || s.getProjects().isEmpty()) {
            return "Workspace chưa có dự án nào để đánh giá rủi ro.";
        }

        record RiskRow(Project project, int total, int overdue, int unassigned, double loadSum, int blockedMembers) {}
        List<RiskRow> rows = new ArrayList<>();
        for (Project p : s.getProjects()) {
            List<Task> pt = tasks.stream()
                    .filter(t -> t.getProject() != null && t.getProject().getId().equals(p.getId()))
                    .toList();
            int overdue = (int) pt.stream().filter(Task::isOverdue).count();
            int unassigned = (int) pt.stream().filter(t -> t.getAssignee() == null && t.getStatus() != TaskStatus.DONE).count();
            double loadSum = pt.stream()
                    .filter(t -> t.getAssignee() != null && t.getStatus() != TaskStatus.DONE)
                    .mapToInt(t -> 1)
                    .sum();
            int blockedMembers = 0; // per-project blocked members not tracked; use global below
            rows.add(new RiskRow(p, pt.size(), overdue, unassigned, loadSum, blockedMembers));
        }
        // Score = 3*overdue + 2*unassigned + total
        rows.sort((a, b) -> Integer.compare(
                3 * b.overdue() + 2 * b.unassigned() + b.total(),
                3 * a.overdue() + 2 * a.unassigned() + a.total()));

        RiskRow top = rows.get(0);
        StringBuilder sb = new StringBuilder();
        sb.append("# Dự án rủi ro nhất: **").append(safeName(top.project())).append("** [").append(safeKey(top.project())).append("]\n\n");
        int score = 3 * top.overdue() + 2 * top.unassigned() + top.total();
        String verdict;
        if (top.total() == 0) verdict = "Không xác định (chưa có task)";
        else if (top.overdue() * 100 / Math.max(1, top.total()) >= 30) verdict = "🔴 Critical";
        else if (top.overdue() * 100 / Math.max(1, top.total()) >= 15) verdict = "🟠 High";
        else if (top.unassigned() > 3) verdict = "🟡 Medium";
        else verdict = "🟢 Low";
        sb.append("- Risk score: **").append(verdict).append("** (raw ").append(score).append(")\n");
        sb.append("- Tổng task: ").append(top.total()).append('\n');
        sb.append("- Quá hạn: **").append(top.overdue()).append("**\n");
        sb.append("- Chưa giao (open): **").append(top.unassigned()).append("**\n\n");

        sb.append("## Yếu tố rủi ro chính\n");
        if (top.overdue() > 0) {
            tasks.stream()
                    .filter(t -> t.getProject() != null && t.getProject().getId().equals(top.project().getId()))
                    .filter(Task::isOverdue)
                    .sorted(Comparator.comparing(Task::getDueDate))
                    .limit(5)
                    .forEach(t -> sb.append("- ").append(t.getTaskKey()).append(" ").append(safeName(t))
                            .append(" (overdue ").append(ChronoUnit.DAYS.between(t.getDueDate(), s.getToday())).append(" ngày)\n"));
        }
        if (top.unassigned() > 0) {
            sb.append("- Có ").append(top.unassigned()).append(" task open chưa có assignee.\n");
        }
        if (top.total() == 0) {
            sb.append("- Dự án chưa có task nào.\n");
        }

        sb.append("\n## Hành động đề xuất\n");
        sb.append("1. Phân công lại ngay các task open chưa có assignee có priority HIGH/URGENT.\n");
        sb.append("2. Review nguyên nhân quá hạn với assignee và đặt lại due date thực tế.\n");
        sb.append("3. Cân nhắc cắt giảm scope hoặc thêm nguồn lực cho dự án.\n");
        return sb.toString();
    }

    private String whoIsOverloaded(WorkspaceSnapshot s) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Tình trạng workload thành viên\n\n");

        if (s.getMembers() == null || s.getMembers().isEmpty()) {
            return sb.append("Chưa có dữ liệu thành viên.").toString();
        }

        Map<UUID, MemberWorkload> workloadMap = s.getWorkloads() != null
                ? s.getWorkloads().stream().collect(Collectors.toMap(w -> w.getMember().getId(), w -> w, (a, b) -> a))
                : Map.of();

        Map<UUID, int[]> openByMember = new HashMap<>();
        if (s.getTasks() != null) {
            for (Task t : s.getTasks()) {
                if (t.getAssignee() != null && t.getStatus() != TaskStatus.DONE) {
                    int[] c = openByMember.computeIfAbsent(t.getAssignee().getId(), k -> new int[3]);
                    c[0]++;
                    if (t.getStatus() == TaskStatus.IN_PROGRESS) c[1]++;
                    if (t.isOverdue()) c[2]++;
                }
            }
        }

        sb.append("| Thành viên | Role | Workload | Open | In progress | Quá hạn | Trạng thái |\n");
        sb.append("|---|---|---|---|---|---|---|\n");

        List<MemberRow> rows = new ArrayList<>();
        for (WorkspaceMember m : s.getMembers()) {
            if (m.getUser() == null) continue;
            MemberWorkload w = workloadMap.get(m.getId());
            int[] counts = openByMember.getOrDefault(m.getUser().getId(), new int[3]);
            int pct = w != null && w.getWorkloadPercentage() != null ? w.getWorkloadPercentage() : 0;
            String status = w != null && w.getStatus() != null ? w.getStatus() : "BALANCED";
            rows.add(new MemberRow(m, pct, status, counts[0], counts[1], counts[2]));
        }
        rows.sort((a, b) -> Integer.compare(b.workload(), a.workload()));
        for (MemberRow r : rows) {
            sb.append("| ").append(safeName(r.member().getUser()))
                    .append(" | ").append(r.member().getRole() != null ? r.member().getRole().getName() : "-")
                    .append(" | ").append(r.workload()).append("%")
                    .append(" | ").append(r.open()).append(" | ").append(r.inProgress()).append(" | ").append(r.overdue())
                    .append(" | ").append(r.workload() > 80 ? "🔴 Overloaded"
                            : r.workload() > 60 ? "🟡 High" : "🟢 OK")
                    .append(" |\n");
        }

        sb.append("\n## Đề xuất phân bổ lại\n");
        List<MemberRow> overloaded = rows.stream().filter(r -> r.workload() > 80).toList();
        List<MemberRow> available = rows.stream().filter(r -> r.workload() < 50).toList();
        if (!overloaded.isEmpty()) {
            sb.append("Thành viên đang quá tải: ");
            sb.append(overloaded.stream().map(r -> safeName(r.member().getUser())).collect(Collectors.joining(", ")));
            sb.append('\n');
        }
        if (!available.isEmpty()) {
            sb.append("Thành viên có dư capacity: ");
            sb.append(available.stream().map(r -> safeName(r.member().getUser())).collect(Collectors.joining(", ")));
            sb.append('\n');
        }
        if (!overloaded.isEmpty() && !available.isEmpty()) {
            sb.append("→ Chuyển các task ưu tiên thấp (LOW/MEDIUM) từ ").append(overloaded.get(0).member().getUser().getFullName());
            sb.append(" sang ").append(available.get(0).member().getUser().getFullName()).append(".\n");
        } else if (overloaded.isEmpty()) {
            sb.append("Không phát hiện thành viên vượt ngưỡng 80% workload.\n");
        } else {
            sb.append("Không có thành viên nào dưới 50% workload để nhận thêm.\n");
        }
        return sb.toString();
    }

    private String whatShouldIDo(WorkspaceSnapshot s) {
        List<Task> candidates = nz(s.getTasks()).stream()
                .filter(t -> t.getStatus() != TaskStatus.DONE)
                .filter(t -> t.getAssignee() != null && t.getAssignee().getId() != null)
                .sorted(Comparator.<Task>comparingInt(t -> priorityRank(t.getPriority()))
                        .thenComparing(Task::getDueDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(3)
                .toList();
        StringBuilder sb = new StringBuilder();
        sb.append("# Đề xuất task nên làm tiếp theo\n\n");
        if (candidates.isEmpty()) {
            sb.append("Hiện chưa có task mở nào để đề xuất. Hãy tạo task mới hoặc giao lại các task unassigned.\n");
            return sb.toString();
        }
        for (Task t : candidates) {
            sb.append("- **").append(t.getTaskKey()).append(" ").append(safeName(t)).append("**\n");
            sb.append("  - Project: ").append(t.getProject() != null ? safeName(t.getProject()) : "-").append('\n');
            sb.append("  - Priority: ").append(t.getPriority()).append('\n');
            sb.append("  - Due: ").append(t.getDueDate() != null ? t.getDueDate().format(DATE_FORMAT) : "chưa có").append('\n');
            sb.append("  - Lý do nên làm: ");
            sb.append(t.getPriority() == TaskPriority.URGENT ? "URGENT và "
                    : t.getPriority() == TaskPriority.HIGH ? "HIGH và " : "");
            sb.append(t.isOverdue() ? "đã quá hạn." : "đến hạn sớm nhất trong nhóm còn lại.");
            sb.append("\n\n");
        }
        return sb.toString();
    }

    private String generateSprint(WorkspaceSnapshot s) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Đề xuất Sprint (2 tuần)\n\n");
        List<Task> pool = nz(s.getTasks()).stream()
                .filter(t -> t.getStatus() == TaskStatus.TODO || t.getStatus() == TaskStatus.REVIEW)
                .sorted(Comparator.<Task>comparingInt(t -> priorityRank(t.getPriority()))
                        .thenComparing(Task::getDueDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(10)
                .toList();
        if (pool.isEmpty()) {
            sb.append("Không có task TODO/REVIEW nào để lên sprint. Có thể workspace đang rỗng hoặc tất cả task đã xong.\n");
            return sb.toString();
        }

        Map<UUID, MemberWorkload> loadMap = s.getWorkloads() != null
                ? s.getWorkloads().stream().collect(Collectors.toMap(w -> w.getMember().getId(), w -> w, (a, b) -> a))
                : Map.of();

        sb.append("**Sprint goal**: đẩy nhanh ").append(pool.size()).append(" task quan trọng nhất, tập trung URGENT/HIGH trước.\n\n");
        sb.append("| Task | Project | Priority | Effort | Assignee |\n");
        sb.append("|---|---|---|---|---|\n");
        for (Task t : pool) {
            String assignee = t.getAssignee() != null ? safeName(t.getAssignee()) : "—";
            sb.append("| ").append(t.getTaskKey()).append(" ").append(safeName(t))
                    .append(" | ").append(t.getProject() != null ? safeName(t.getProject()) : "-")
                    .append(" | ").append(t.getPriority())
                    .append(" | M")
                    .append(" | ").append(assignee).append(" |\n");
        }

        sb.append("\n## Capacity check\n");
        if (s.getMembers() != null) {
            for (WorkspaceMember m : s.getMembers()) {
                if (m.getUser() == null) continue;
                MemberWorkload w = loadMap.get(m.getId());
                int pct = w != null && w.getWorkloadPercentage() != null ? w.getWorkloadPercentage() : 0;
                sb.append("- ").append(safeName(m.getUser())).append(" hiện tại ").append(pct).append("% → ");
                sb.append(pct < 70 ? "có thể nhận thêm." : pct < 90 ? "sức chứa hạn chế." : "đã quá tải, không nên nhận thêm.").append('\n');
            }
        }

        sb.append("\n## Rủi ro / phụ thuộc\n");
        long unassigned = pool.stream().filter(t -> t.getAssignee() == null).count();
        sb.append("- Task chưa giao: ").append(unassigned).append('\n');
        long overdue = pool.stream().filter(Task::isOverdue).count();
        sb.append("- Task đã quá hạn trong pool: ").append(overdue).append('\n');
        return sb.toString();
    }

    private String suggestAssignee(WorkspaceSnapshot s, String question) {
        Task target = pickTargetTask(s, question);
        StringBuilder sb = new StringBuilder();
        if (target == null) {
            return "Hiện không có task phù hợp để gợi ý assignee. Hãy nêu rõ task key hoặc tiêu đề.";
        }
        sb.append("# Gợi ý assignee cho **").append(target.getTaskKey()).append(" ").append(safeName(target)).append("**\n\n");

        String detectedSkill = new com.taskflow.ai.service.SkillResolver().detectSkill(
                target.getTitle(), target.getDescription(), null);

        List<RankedMember> ranked = new ArrayList<>();
        if (s.getMembers() != null) {
            for (WorkspaceMember m : s.getMembers()) {
                if (m.getUser() == null) continue;
                int score = 100;
                String reason = "";
                int workload = computeWorkloadForUser(m, s);
                score -= workload / 2; // overloaded members lose points
                if (workload > 80) reason += "workload cao (" + workload + "%); ";
                else if (workload < 60) reason += "còn dư capacity (" + workload + "%); ";
                else reason += "workload vừa phải (" + workload + "%); ";
                if (m.getRole() != null && new com.taskflow.ai.service.SkillResolver()
                        .roleMatchesSkill(m.getRole().getName(), detectedSkill)) {
                    score += 30;
                    reason += "role " + m.getRole().getName() + " khớp skill " + detectedSkill + "; ";
                }
                // continuity: already has tasks in same project
                long sameProject = nz(s.getTasks()).stream()
                        .filter(t -> t.getAssignee() != null && t.getAssignee().getId().equals(m.getUser().getId()))
                        .filter(t -> t.getProject() != null && target.getProject() != null
                                && t.getProject().getId().equals(target.getProject().getId()))
                        .filter(t -> t.getStatus() != TaskStatus.DONE)
                        .count();
                if (sameProject > 0) {
                    score += 5;
                    reason += "đã có " + sameProject + " task trong cùng project; ";
                }
                ranked.add(new RankedMember(m, score, reason.trim()));
            }
        }
        ranked.sort((a, b) -> Integer.compare(b.score(), a.score()));
        if (ranked.isEmpty()) {
            return sb.append("Không có thành viên nào để đề xuất.").toString();
        }
        sb.append("**Top 1**: ").append(safeName(ranked.get(0).member().getUser()))
                .append(" (score ").append(ranked.get(0).score()).append(") — ")
                .append(ranked.get(0).reason()).append("\n\n");
        if (ranked.size() > 1) {
            sb.append("**Runner up**: ").append(safeName(ranked.get(1).member().getUser()))
                    .append(" (score ").append(ranked.get(1).score()).append(") — ")
                    .append(ranked.get(1).reason()).append("\n");
        }
        return sb.toString();
    }

    private String weeklyReport(WorkspaceSnapshot s) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Báo cáo tuần\n\n");
        LocalDate weekAgo = s.getToday().minusDays(7);
        List<Task> recentDone = nz(s.getTasks()).stream()
                .filter(t -> t.getStatus() == TaskStatus.DONE)
                .filter(t -> t.getUpdatedAt() != null && t.getUpdatedAt().toLocalDate().isAfter(weekAgo))
                .sorted(Comparator.comparing(Task::getUpdatedAt).reversed())
                .limit(10)
                .toList();
        sb.append("## Đã hoàn thành (7 ngày qua)\n");
        if (recentDone.isEmpty()) sb.append("- Không có task nào được đóng trong tuần này.\n");
        else recentDone.forEach(t -> sb.append("- ").append(t.getTaskKey()).append(" ").append(safeName(t)).append('\n'));

        sb.append("\n## Đang chạy (top theo priority)\n");
        nz(s.getTasks()).stream()
                .filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS)
                .sorted(Comparator.comparingInt(t -> priorityRank(t.getPriority())))
                .limit(10)
                .forEach(t -> sb.append("- ").append(t.getTaskKey()).append(" ").append(safeName(t)).append('\n'));

        sb.append("\n## Quá hạn\n");
        long overdue = nz(s.getTasks()).stream().filter(Task::isOverdue).count();
        sb.append("- Tổng: ").append(overdue).append('\n');
        nz(s.getTasks()).stream().filter(Task::isOverdue).limit(5)
                .forEach(t -> sb.append("  - ").append(t.getTaskKey()).append(" ").append(safeName(t)).append('\n'));

        sb.append("\n## Trọng tâm tuần tới\n");
        sb.append("- Giải quyết các task quá hạn có priority URGENT trước.\n");
        sb.append("- Đóng các task TODO có due date trong 7 ngày tới.\n");
        return sb.toString();
    }

    private String dailyReport(WorkspaceSnapshot s) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Daily report — ").append(s.getToday()).append("\n\n");
        sb.append("## Đang chạy hôm nay\n");
        nz(s.getTasks()).stream()
                .filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS)
                .sorted(Comparator.comparing(Task::getDueDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(5)
                .forEach(t -> sb.append("- ").append(t.getTaskKey()).append(" ").append(safeName(t)).append('\n'));

        sb.append("\n## Deadline hôm nay\n");
        nz(s.getTasks()).stream()
                .filter(t -> t.getDueDate() != null && t.getDueDate().equals(s.getToday()))
                .forEach(t -> sb.append("- ").append(t.getTaskKey()).append(" ").append(safeName(t)).append('\n'));

        sb.append("\n## Quá hạn cần xử lý\n");
        long overdue = nz(s.getTasks()).stream().filter(Task::isOverdue).count();
        sb.append("- Tổng quá hạn: ").append(overdue).append('\n');

        sb.append("\n## Focus hôm nay\n");
        Task focus = nz(s.getTasks()).stream()
                .filter(t -> t.getStatus() != TaskStatus.DONE)
                .min(Comparator.<Task>comparingInt(t -> priorityRank(t.getPriority()))
                        .thenComparing(Task::getDueDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
        if (focus != null) {
            sb.append("- ").append(focus.getTaskKey()).append(" ").append(safeName(focus)).append('\n');
        } else {
            sb.append("- Tất cả task đã hoàn thành hoặc không có task mở.\n");
        }
        return sb.toString();
    }

    private String blockersReport(WorkspaceSnapshot s) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Blockers\n\n");
        boolean any = false;
        // unassigned urgent/high
        List<Task> unassigned = nz(s.getTasks()).stream()
                .filter(t -> t.getAssignee() == null && t.getStatus() != TaskStatus.DONE
                        && (t.getPriority() == TaskPriority.URGENT || t.getPriority() == TaskPriority.HIGH))
                .toList();
        if (!unassigned.isEmpty()) {
            any = true;
            sb.append("## Task quan trọng chưa có assignee\n");
            unassigned.forEach(t -> sb.append("- ").append(t.getTaskKey()).append(" ").append(safeName(t))
                    .append(" (priority ").append(t.getPriority()).append(")\n"));
        }
        // overdue assigned
        List<Task> overdueAssigned = nz(s.getTasks()).stream()
                .filter(Task::isOverdue)
                .filter(t -> t.getAssignee() != null)
                .sorted(Comparator.comparing(Task::getDueDate))
                .limit(10)
                .toList();
        if (!overdueAssigned.isEmpty()) {
            any = true;
            sb.append("\n## Task quá hạn đã phân công\n");
            overdueAssigned.forEach(t -> sb.append("- ").append(t.getTaskKey()).append(" ").append(safeName(t))
                    .append(" (overdue, assignee: ").append(safeName(t.getAssignee())).append(")\n"));
        }
        // stuck in-progress
        LocalDateTime threshold = LocalDateTime.now().minusDays(7);
        List<Task> stuck = nz(s.getTasks()).stream()
                .filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS)
                .filter(t -> t.getUpdatedAt() != null && t.getUpdatedAt().isBefore(threshold))
                .toList();
        if (!stuck.isEmpty()) {
            any = true;
            sb.append("\n## Task kẹt trong IN_PROGRESS > 7 ngày\n");
            stuck.forEach(t -> sb.append("- ").append(t.getTaskKey()).append(" ").append(safeName(t)).append('\n'));
        }
        // blocked members
        if (s.getWorkloads() != null) {
            List<MemberWorkload> blockedMembers = s.getWorkloads().stream()
                    .filter(w -> "BLOCKED".equalsIgnoreCase(w.getStatus()))
                    .filter(w -> w.getMember() != null && w.getMember().getUser() != null)
                    .toList();
            if (!blockedMembers.isEmpty()) {
                any = true;
                sb.append("\n## Thành viên bị BLOCKED\n");
                blockedMembers.forEach(w -> sb.append("- ").append(safeName(w.getMember().getUser()))
                        .append(" (blocked tasks: ").append(w.getBlockedTasks() == null ? 0 : w.getBlockedTasks()).append(")\n"));
            }
        }
        if (!any) sb.append("Không phát hiện blocker đáng kể.\n");
        return sb.toString();
    }

    private String upcomingDeadlines(WorkspaceSnapshot s) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Deadline sắp tới\n\n");
        appendUpcomingTable(sb, nz(s.getTasks()), s.getToday(), 25);
        return sb.toString();
    }

    private String goalsProgress(WorkspaceSnapshot s) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Tiến độ Goals\n\n");
        if (s.getGoals() == null || s.getGoals().isEmpty()) {
            return sb.append("Workspace chưa có goal nào.").toString();
        }
        for (Goal g : s.getGoals()) {
            sb.append("## ").append(safeName(g)).append(" [").append(g.getStatus()).append("]\n");
            sb.append("- Progress: **").append(g.getProgressPercentage()).append("%**\n");
            if (g.getDueDate() != null) sb.append("- Due: ").append(g.getDueDate().format(DATE_FORMAT)).append('\n');
            if (g.getOwner() != null) sb.append("- Owner: ").append(safeName(g.getOwner())).append('\n');
            if (g.getKeyResults() != null && !g.getKeyResults().isEmpty()) {
                sb.append("- Key results:\n");
                g.getKeyResults().forEach(kr -> sb.append("  - ").append(safeName(kr))
                        .append(" (").append(kr.getProgressPercentage() != null ? kr.getProgressPercentage() : 0).append("%)\n"));
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    private String projectHealth(WorkspaceSnapshot s) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Project health\n\n");
        if (s.getProjects() == null || s.getProjects().isEmpty()) {
            return sb.append("Chưa có dự án nào.").toString();
        }
        sb.append("| Project | Done/Total | Overdue | Unassigned open | Health |\n");
        sb.append("|---|---|---|---|---|\n");
        for (Project p : s.getProjects()) {
            List<Task> pt = nz(s.getTasks()).stream()
                    .filter(t -> t.getProject() != null && t.getProject().getId().equals(p.getId()))
                    .toList();
            int done = (int) pt.stream().filter(t -> t.getStatus() == TaskStatus.DONE).count();
            int overdue = (int) pt.stream().filter(Task::isOverdue).count();
            int unassigned = (int) pt.stream().filter(t -> t.getAssignee() == null && t.getStatus() != TaskStatus.DONE).count();
            int total = pt.size();
            String health;
            if (total == 0) health = "—";
            else if (overdue * 100 / total >= 30 || unassigned > 5) health = "🔴 Off Track";
            else if (overdue * 100 / total >= 15) health = "🟡 At Risk";
            else health = "🟢 On Track";
            sb.append("| ").append(safeName(p)).append(" [").append(safeKey(p)).append("]")
                    .append(" | ").append(done).append('/').append(total)
                    .append(" | ").append(overdue)
                    .append(" | ").append(unassigned)
                    .append(" | ").append(health).append(" |\n");
        }
        return sb.toString();
    }

    private String general(WorkspaceSnapshot s, String question) {
        return "# Trả lời\n\n"
                + "Dựa trên dữ liệu workspace:\n\n"
                + "- Tổng dự án: " + (s.getProjects() != null ? s.getProjects().size() : 0) + "\n"
                + "- Tổng task: " + (s.getTasks() != null ? s.getTasks().size() : 0) + "\n"
                + "- Quá hạn: " + nz(s.getTasks()).stream().filter(Task::isOverdue).count() + "\n"
                + "- Câu hỏi của bạn: " + (question == null ? "" : question) + "\n\n"
                + "Hãy thử một trong các gợi ý: \"Tóm tắt workspace\", \"Task quá hạn\", \"Thành viên quá tải\", \"Lên sprint\"...\n";
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    private void appendTeamStatusTable(StringBuilder sb, WorkspaceSnapshot s) {
        if (s.getMembers() == null) return;
        Map<UUID, MemberWorkload> map = s.getWorkloads() != null
                ? s.getWorkloads().stream().collect(Collectors.toMap(w -> w.getMember().getId(), w -> w, (a, b) -> a))
                : Map.of();
        sb.append("| Thành viên | Role | Workload | Trạng thái |\n|---|---|---|---|\n");
        s.getMembers().stream()
                .filter(m -> m.getUser() != null)
                .sorted(Comparator.comparing(
                        (WorkspaceMember m) -> {
                            MemberWorkload w = map.get(m.getId());
                            return w != null && w.getWorkloadPercentage() != null ? -w.getWorkloadPercentage() : 0;
                        }))
                .limit(10)
                .forEach(m -> {
                    MemberWorkload w = map.get(m.getId());
                    int pct = w != null && w.getWorkloadPercentage() != null ? w.getWorkloadPercentage() : 0;
                    sb.append("| ").append(safeName(m.getUser()))
                            .append(" | ").append(m.getRole() != null ? m.getRole().getName() : "-")
                            .append(" | ").append(pct).append("%")
                            .append(" | ").append(pct > 80 ? "🔴" : pct > 60 ? "🟡" : "🟢").append(" |\n");
                });
    }

    private void appendUpcomingTable(StringBuilder sb, List<Task> tasks, LocalDate today, int limit) {
        sb.append("| Task | Project | Assignee | Due | Status |\n|---|---|---|---|---|\n");
        tasks.stream()
                .filter(t -> t.getDueDate() != null)
                .sorted(Comparator.comparing(Task::getDueDate))
                .limit(limit)
                .forEach(t -> sb.append("| ").append(t.getTaskKey()).append(" ").append(safeName(t))
                        .append(" | ").append(t.getProject() != null ? safeName(t.getProject()) : "-")
                        .append(" | ").append(t.getAssignee() != null ? safeName(t.getAssignee()) : "—")
                        .append(" | ").append(t.getDueDate().format(DATE_FORMAT))
                        .append(t.isOverdue() ? " ⚠️" : "")
                        .append(" | ").append(t.getStatus()).append(" |\n"));
    }

    private Task pickTargetTask(WorkspaceSnapshot s, String question) {
        List<Task> tasks = nz(s.getTasks());
        if (question != null && !question.isBlank()) {
            String q = question.toLowerCase(Locale.ROOT);
            for (Task t : tasks) {
                if (t.getTitle() != null && q.contains(t.getTitle().toLowerCase())) return t;
                if (q.contains(t.getTaskKey().toLowerCase())) return t;
            }
        }
        return tasks.stream()
                .filter(t -> t.getStatus() != TaskStatus.DONE)
                .filter(t -> t.getAssignee() == null)
                .min(Comparator.comparingInt(t -> priorityRank(t.getPriority())))
                .orElseGet(() -> tasks.stream()
                        .filter(t -> t.getStatus() != TaskStatus.DONE)
                        .min(Comparator.comparingInt(t -> priorityRank(t.getPriority())))
                        .orElse(null));
    }

    private int computeWorkloadForUser(WorkspaceMember m, WorkspaceSnapshot s) {
        if (s.getWorkloads() != null) {
            for (MemberWorkload w : s.getWorkloads()) {
                if (w.getMember() != null && w.getMember().getId().equals(m.getId())) {
                    return w.getWorkloadPercentage() != null ? w.getWorkloadPercentage() : 0;
                }
            }
        }
        return 0;
    }

    private int projectProgress(Project p, List<Task> tasks) {
        List<Task> pt = tasks.stream()
                .filter(t -> t.getProject() != null && t.getProject().getId().equals(p.getId()))
                .toList();
        if (pt.isEmpty()) return 0;
        int done = (int) pt.stream().filter(t -> t.getStatus() == TaskStatus.DONE).count();
        return Math.round(done * 100f / pt.size());
    }

    private int priorityRank(TaskPriority priority) {
        return switch (priority) {
            case URGENT -> 0;
            case HIGH -> 1;
            case MEDIUM -> 2;
            case LOW -> 3;
        };
    }

    private String safeName(Object o) {
        if (o == null) return "—";
        if (o instanceof User u) return u.getFullName() != null ? u.getFullName() : "—";
        if (o instanceof Project p) return p.getName() != null ? p.getName() : "—";
        if (o instanceof Task t) return t.getTitle() != null ? t.getTitle() : "—";
        if (o instanceof Goal g) return g.getTitle() != null ? g.getTitle() : "—";
        if (o instanceof Page p) return p.getTitle() != null ? p.getTitle() : "—";
        if (o instanceof KeyResult kr) return kr.getTitle() != null ? kr.getTitle() : "—";
        if (o instanceof Workspace w) return w.getName() != null ? w.getName() : "—";
        return o.toString();
    }

    private String safeKey(Project p) {
        return p.getKey() != null ? p.getKey() : "?";
    }

    private List<Task> nz(List<Task> in) {
        return in != null ? in : List.of();
    }

    private List<RelatedItem> topProjects(WorkspaceSnapshot s, int limit) {
        if (s.getProjects() == null) return List.of();
        return s.getProjects().stream()
                .limit(limit)
                .map(p -> RelatedItem.builder()
                        .id(p.getId() != null ? p.getId().toString() : null)
                        .name(safeName(p))
                        .type("project")
                        .status(p.getKey())
                        .description(p.getDescription())
                        .build())
                .toList();
    }

    private List<RelatedItem> topTasks(WorkspaceSnapshot s, int limit) {
        return nz(s.getTasks()).stream()
                .filter(t -> t.getStatus() != TaskStatus.DONE)
                .sorted(Comparator.comparing(Task::getDueDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(limit)
                .map(t -> RelatedItem.builder()
                        .id(t.getId() != null ? t.getId().toString() : null)
                        .name(t.getTitle())
                        .type("task")
                        .status(t.getStatus().name())
                        .description(t.getTaskKey())
                        .build())
                .toList();
    }

    private List<RelatedItem> topMembers(WorkspaceSnapshot s, int limit) {
        if (s.getMembers() == null) return List.of();
        return s.getMembers().stream()
                .filter(m -> m.getUser() != null)
                .limit(limit)
                .map(m -> RelatedItem.builder()
                        .id(m.getId() != null ? m.getId().toString() : null)
                        .name(safeName(m.getUser()))
                        .type("member")
                        .status(m.getRole() != null ? m.getRole().getName() : null)
                        .description(m.getUser().getEmail())
                        .build())
                .toList();
    }

    private List<RelatedItem> topGoals(WorkspaceSnapshot s, int limit) {
        if (s.getGoals() == null) return List.of();
        return s.getGoals().stream()
                .filter(g -> g.getStatus() == Goal.GoalStatus.ACTIVE)
                .limit(limit)
                .map(g -> RelatedItem.builder()
                        .id(g.getId() != null ? g.getId().toString() : null)
                        .name(safeName(g))
                        .type("goal")
                        .status(g.getStatus().name())
                        .description(g.getDescription())
                        .build())
                .toList();
    }

    private List<RelatedItem> topPages(WorkspaceSnapshot s, int limit) {
        if (s.getPages() == null) return List.of();
        return s.getPages().stream()
                .limit(limit)
                .map(p -> RelatedItem.builder()
                        .id(p.getId() != null ? p.getId().toString() : null)
                        .name(safeName(p))
                        .type("page")
                        .status(p.getCreatedBy() != null ? p.getCreatedBy().getFullName() : null)
                        .description(p.getSlug())
                        .build())
                .toList();
    }

    private List<RelatedItem> parseRelated(JsonNode node, String type) {
        List<RelatedItem> list = new ArrayList<>();
        if (node != null && node.isArray()) {
            for (JsonNode item : node) {
                RelatedItem ri = RelatedItem.builder()
                        .id(item.path("id").asText(null))
                        .name(item.path("name").asText(null))
                        .type(item.path("type").asText(type))
                        .status(item.path("status").asText(null))
                        .description(item.path("description").asText(null))
                        .build();
                if (ri.getName() != null) list.add(ri);
            }
        }
        return list;
    }

    private List<String> toStringList(JsonNode node) {
        List<String> out = new ArrayList<>();
        if (node != null && node.isArray()) {
            for (JsonNode n : node) {
                String v = n.asText();
                if (v != null && !v.isBlank()) out.add(v);
            }
        }
        return out;
    }

    private String sourcesForIntent(QuestionIntentClassifier.Intent intent) {
        return switch (intent) {
            case SUMMARIZE_WORKSPACE -> "WORKSPACE;TEAM & WORKLOAD;PROJECTS & TASKS;DEADLINES;RECENT ACTIVITY";
            case MOST_RISKY_PROJECT -> "PROJECTS & TASKS;DEADLINES;BLOCKERS;TEAM & WORKLOAD";
            case WHO_IS_OVERLOADED -> "TEAM & WORKLOAD;PROJECTS & TASKS;DEADLINES";
            case WHAT_SHOULD_I_DO -> "PROJECTS & TASKS;DEADLINES;BLOCKERS;TEAM & WORKLOAD";
            case GENERATE_SPRINT -> "PROJECTS & TASKS;TEAM & WORKLOAD;DEADLINES";
            case SUGGEST_ASSIGNEE -> "TEAM & WORKLOAD;PROJECTS & TASKS;SUBTASKS";
            case WEEKLY_REPORT -> "PROJECTS & TASKS;DEADLINES;BLOCKERS;GOALS & OKRs;RECENT ACTIVITY";
            case DAILY_REPORT -> "PROJECTS & TASKS;DEADLINES;BLOCKERS";
            case BLOCKERS -> "BLOCKERS;PROJECTS & TASKS;TEAM & WORKLOAD";
            case UPCOMING_DEADLINES -> "DEADLINES;PROJECTS & TASKS";
            case GOALS_PROGRESS -> "GOALS & OKRs";
            case PROJECT_HEALTH -> "PROJECTS & TASKS;DEADLINES;TEAM & WORKLOAD";
            case GENERAL -> "WORKSPACE";
        };
    }

    private List<String> suggestionsFor(QuestionIntentClassifier.Intent intent, WorkspaceSnapshot s) {
        return switch (intent) {
            case SUMMARIZE_WORKSPACE -> List.of(
                    "Dự án nào đang rủi ro nhất?",
                    "Ai đang quá tải?",
                    "Lên sprint tuần tới");
            case MOST_RISKY_PROJECT -> List.of(
                    "Phân công lại các task unassigned của dự án này",
                    "Review deadline với team",
                    "Xem blockers chi tiết");
            case WHO_IS_OVERLOADED -> List.of(
                    "Lên sprint cân bằng workload",
                    "Đề xuất assignee cho task open",
                    "Báo cáo tuần");
            case WHAT_SHOULD_I_DO -> List.of(
                    "Lên sprint 2 tuần",
                    "Đề xuất assignee cho task này",
                    "Xem blockers");
            case GENERATE_SPRINT -> List.of(
                    "Đề xuất assignee",
                    "Báo cáo daily",
                    "Blockers hiện tại");
            case SUGGEST_ASSIGNEE -> List.of(
                    "Lên sprint",
                    "Tóm tắt workspace",
                    "Ai quá tải");
            case WEEKLY_REPORT -> List.of(
                    "Báo cáo daily hôm nay",
                    "Dự án rủi ro nhất",
                    "Blockers");
            case DAILY_REPORT -> List.of(
                    "Deadline tuần này",
                    "Báo cáo weekly",
                    "Tóm tắt workspace");
            case BLOCKERS -> List.of(
                    "Phân công lại task unassigned",
                    "Review với thành viên bị BLOCKED",
                    "Lên sprint tiếp theo");
            case UPCOMING_DEADLINES -> List.of(
                    "Task nào nên làm trước",
                    "Báo cáo daily",
                    "Blockers");
            case GOALS_PROGRESS -> List.of(
                    "Dự án rủi ro nhất",
                    "Tóm tắt workspace",
                    "Báo cáo weekly");
            case PROJECT_HEALTH -> List.of(
                    "Deadline sắp tới",
                    "Ai quá tải",
                    "Blockers");
            case GENERAL -> List.of(
                    "Tóm tắt workspace",
                    "Dự án rủi ro nhất",
                    "Lên sprint");
        };
    }

    private WorkspaceAnswerResponse emptyAnswer(QuestionIntentClassifier.Intent intent, long ms) {
        return WorkspaceAnswerResponse.builder()
                .answer("I don't have enough workspace information to answer this question.")
                .confidence(0.0)
                .intent(intent.name())
                .processingTimeMs(ms)
                .build();
    }

    private String extractJson(String text) {
        if (text == null) return null;
        String trimmed = text.trim();
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) return trimmed.substring(start, end + 1);
        return null;
    }

    private record MemberRow(WorkspaceMember member, int workload, String status,
                             int open, int inProgress, int overdue) {}
    private record RankedMember(WorkspaceMember member, int score, String reason) {}
}
