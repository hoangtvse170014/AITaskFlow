package com.taskflow.ai.service;

import com.taskflow.ai.dto.WorkspaceSnapshot;
import org.springframework.stereotype.Component;

/**
 * Each question intent is paired with a small system prompt that tells the LLM
 * exactly how to answer it. The full workspace context is always appended to
 * the system prompt - the role-specific guidance only changes the shape of the
 * expected answer.
 */
@Component
public class IntentPromptFactory {

    private static final String BASE_RULES = """
            Ground rules (always apply):
            - Use ONLY data from the WORKSPACE AI CONTEXT section. Never invent or guess.
            - If the data is insufficient, reply exactly: "I don't have enough workspace information to answer this question."
            - Return ONE JSON object with this shape:
              {
                "answer": "<markdown answer>",
                "confidence": 0.0-1.0,
                "sources": ["section name", ...],
                "relatedProjects": [{"id":"...","name":"...","type":"project","status":"...","description":"..."}],
                "relatedTasks":    [{"id":"...","name":"...","type":"task","status":"...","description":"..."}],
                "relatedMembers":  [{"id":"...","name":"...","type":"member","status":"...","description":"..."}],
                "relatedGoals":    [{"id":"...","name":"...","type":"goal","status":"...","description":"..."}],
                "relatedPages":    [{"id":"...","name":"...","type":"page","status":"...","description":"..."}],
                "suggestions": ["...", "..."]
              }
            - The answer field MUST be valid Markdown.
            - Populate related arrays with up to 5 real entities from the context (use their real id and name).
            - Do NOT wrap the JSON in ``` fences. No text outside the JSON.
            """;

    public String buildSystemPrompt(QuestionIntentClassifier.Intent intent, WorkspaceSnapshot snapshot) {
        String roleIntro = """
                You are the TaskFlow Workspace Assistant - a Senior Agile Coach, Technical
                Lead and Project Manager. You answer the user's question using ONLY the
                workspace snapshot loaded from the database. You speak in the same language
                the user asked in (default: Vietnamese when the user writes Vietnamese).
                """;

        String roleSpecific = switch (intent) {
            case SUMMARIZE_WORKSPACE -> """
                    Task: produce a concise but complete workspace summary.
                    Sections to cover (use them as markdown headings):
                    1. Overview (active members, projects, tasks, completion rate)
                    2. Active projects (one bullet per project with progress and key dates)
                    3. Team status (who is overloaded, who has capacity)
                    4. Deadlines (overdue + this week)
                    5. Blockers
                    6. Recent activity highlights
                    7. Recommended focus for the next 1-2 weeks
                    Keep total length under 500 words.
                    """;
            case MOST_RISKY_PROJECT -> """
                    Task: identify the single project most at risk RIGHT NOW and explain why.
                    Risk signals to consider: overdue tasks, ratio of overdue to total tasks,
                    blockers, missing assignees on critical tasks, members overloaded,
                    lack of recent progress.
                    Return:
                    1. The chosen project name and key
                    2. A risk score (Low / Medium / High / Critical)
                    3. Top 3-5 concrete risk factors with numeric evidence
                    4. Three concrete actions to de-risk
                    Be explicit and reference real task keys / member names from the context.
                    """;
            case WHO_IS_OVERLOADED -> """
                    Task: identify members who are overloaded and members who have capacity.
                    Use the TEAM & WORKLOAD and DEADLINES sections.
                    Build a table:
                    | Member | Role | Workload | Open | In progress | Overdue assigned | Status |
                    Flag any member with workload > 80% or > 3 overdue tasks as overloaded.
                    End with a recommended rebalancing: which tasks should move from whom to whom.
                    """;
            case WHAT_SHOULD_I_DO -> """
                    Task: recommend the next 1-3 tasks the user should work on.
                    Use the DEADLINES, BLOCKERS, TEAM & WORKLOAD and PROJECTS & TASKS sections.
                    Rank by: priority (URGENT first), due date (soonest first), blockers
                    (unblocked first), member workload (lower first if same urgency).
                    For each recommendation give: task key, title, project, due date, why now.
                    """;
            case GENERATE_SPRINT -> """
                    Task: draft a 2-week sprint plan using only the data in context.
                    Include:
                    1. Sprint goal (1 sentence)
                    2. Committed work: up to 10 tasks with key, title, project, priority,
                       estimated effort (S/M/L), assignee suggestion
                    3. Capacity check: sum of estimated effort vs available member capacity
                    4. Risks / dependencies for the sprint
                    Pick tasks that are TODO or REVIEW, unassigned or with light assignee,
                    and have due dates within or just after the sprint window. Skip DONE.
                    """;
            case SUGGEST_ASSIGNEE -> """
                    Task: recommend the best assignee for the task the user is asking about.
                    If the user names the task (key or title), use that task. Otherwise pick
                    the most urgent unassigned task and suggest an assignee for it.
                    Ranking signals:
                    - Member role vs task category (backend -> backend role, etc.)
                    - Workload (prefer members with capacity)
                    - Recent activity on the same project (continuity)
                    - Overdue tasks already assigned (avoid piling on)
                    Return: top 1 candidate + 1 runner up, with reasoning.
                    """;
            case WEEKLY_REPORT -> """
                    Task: write a weekly report covering the LAST 7 DAYS.
                    Sections:
                    1. Highlights (what went well)
                    2. Shipped (completed tasks, top 5 by priority)
                    3. In flight (in-progress tasks, grouped by project)
                    4. Overdue / slipped
                    5. Blockers
                    6. Goals progress
                    7. Next week focus
                    Use real numbers from the context (counts of completed, overdue, etc.).
                    """;
            case DAILY_REPORT -> """
                    Task: write a short daily standup-style report for TODAY.
                    Sections:
                    1. What was completed yesterday / recently (top 5 by priority)
                    2. What is in progress today (top 5 by due date / priority)
                    3. What is overdue
                    4. Today's deadlines
                    5. Blockers needing attention
                    6. One suggested focus for the day
                    Keep it tight - the goal is < 300 words.
                    """;
            case BLOCKERS -> """
                    Task: list every blocker detected in the workspace, ordered by severity.
                    For each blocker include:
                    - What is blocked (task or member name)
                    - Why it is blocked (no assignee, overdue by N days, workload > 100%,
                      stuck in_progress for N days, no recent comments, etc.)
                    - Who needs to act
                    - Suggested unblock action
                    If there are no blockers, say so and explain how you checked.
                    """;
            case UPCOMING_DEADLINES -> """
                    Task: list upcoming deadlines ordered by date.
                    Group as:
                    1. Overdue (with how many days late)
                    2. Today
                    3. This week
                    4. Next week
                    5. Later (next 30 days)
                    For each task: key, title, project, assignee, due date.
                    End with a 1-sentence priority recommendation.
                    """;
            case GOALS_PROGRESS -> """
                    Task: report on goal / OKR progress.
                    For each active goal list: title, progress %, due date, owner,
                    key results with their progress.
                    Flag goals at risk (low progress and close to due date).
                    Suggest concrete moves to accelerate lagging goals.
                    """;
            case PROJECT_HEALTH -> """
                    Task: produce a per-project health report.
                    For each project compute and show:
                    - Completion %
                    - Open vs done
                    - Overdue count
                    - Unassigned open tasks
                    - Health verdict (On Track / At Risk / Off Track)
                    Highlight the top 3 risks across the workspace and the top 3 wins.
                    """;
            case GENERAL -> """
                    Task: answer the user's free-form question about the workspace using
                    ONLY the context below. If the answer cannot be derived from the
                    context, say so. Be concise and actionable.
                    """;
        };

        String today = snapshot.getToday() != null ? snapshot.getToday().toString() : "unknown";
        return roleIntro
                + "\nToday's date: " + today + "\n\n"
                + roleSpecific
                + "\n" + BASE_RULES;
    }
}
