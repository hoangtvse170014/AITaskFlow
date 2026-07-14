package com.taskflow.ai.service;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Light-weight intent classifier for the Workspace Assistant. It scans the user
 * question for keywords / phrases and returns a single {@link Intent} bucket
 * that drives which specialised prompt and post-processing is used.
 *
 * <p>The classifier is intentionally rule-based (no model calls) so it works
 * consistently regardless of whether the LLM is reachable. Ties are broken in
 * declaration order - earlier rules win.
 */
@Component
public class QuestionIntentClassifier {

    public enum Intent {
        SUMMARIZE_WORKSPACE,
        MOST_RISKY_PROJECT,
        WHO_IS_OVERLOADED,
        WHAT_SHOULD_I_DO,
        GENERATE_SPRINT,
        SUGGEST_ASSIGNEE,
        WEEKLY_REPORT,
        DAILY_REPORT,
        BLOCKERS,
        UPCOMING_DEADLINES,
        GOALS_PROGRESS,
        PROJECT_HEALTH,
        GENERAL
    }

    private static final Pattern[] SUMMARIZE = {
            Pattern.compile("(?i)\\b(summar(y|ize|ise)|overview|tổng quan|tóm tắt)\\b.*\\b(workspace|project|team|work|the)?\\b"),
            Pattern.compile("(?i)\\b(workspace|project|team)\\b.*\\b(summar|overview|tổng quan)\\b"),
            Pattern.compile("(?i)\\bhow\\s+is\\s+(the\\s+)?(workspace|project|everything)\\b"),
            Pattern.compile("(?i)\\bwhat'?s\\s+happening\\b")
    };

    private static final Pattern[] RISKY = {
            Pattern.compile("(?i)\\b(riskiest|risk(y|iest)|most\\s+at\\s+risk|high\\s+risk)\\b.*\\bproject"),
            Pattern.compile("(?i)\\bproject.*\\b(riskiest|risk(y|iest)|at\\s+risk|in\\s+trouble)"),
            Pattern.compile("(?i)\\b(nguy\\s*hi[eể]m|rủi\\s*ro)\\b")
    };

    private static final Pattern[] OVERLOADED = {
            Pattern.compile("(?i)\\b(overload(ed)?|overworked|too\\s+busy|too\\s+many\\s+tasks|quá\\s*tải)"),
            Pattern.compile("(?i)\\bwho\\s+is\\s+(overloaded|busy|overworked)"),
            Pattern.compile("(?i)\\b(workload|capacity).*\\b(high|over|exceeded|max)"),
            Pattern.compile("(?i)\\b(high|heavy)\\s+workload"),
            Pattern.compile("(?i)\\bai\\s+.*\\b(overload|busy)"),
            Pattern.compile("(?i)\\b(quá tải|quá nhiều việc)"),
            Pattern.compile("(?i)\\bnhân viên.*\\b(bận|quá tải|overload)"),
            Pattern.compile("(?i)\\b(thành viên|ai).*\\b(quá tải|overload|bận)"),
    };

    private static final Pattern[] WHAT_TO_DO = {
            Pattern.compile("(?i)\\bwhat\\s+(should|can|do)\\s+I\\s+(do|start|work\\s+on|tackle|take|focus)"),
            Pattern.compile("(?i)\\b(which|what)\\s+task\\s+(should|do)\\s+I\\s+(do|start|pick|take)"),
            Pattern.compile("(?i)\\b(my|next)\\s+(priority|task|step|action)"),
            Pattern.compile("(?i)\\brecommend.*\\b(task|next|priority)"),
            Pattern.compile("(?i)\\bsuggest.*\\b(task|next)"),
            Pattern.compile("(?i)\\btôi\\s+nên\\s+(làm|làm gì|bắt đầu)"),
            Pattern.compile("(?i)\\bnên\\s+làm\\s+gì"),
            Pattern.compile("(?i)\\bưu\\s*tiên\\s+gì"),
    };

    private static final Pattern[] SPRINT = {
            Pattern.compile("(?i)\\b(generate|plan|create|build|propose|suggest)\\b.*\\bsprint"),
            Pattern.compile("(?i)\\bsprint\\s+(plan|planning|backlog|goal)"),
            Pattern.compile("(?i)\\bnext\\s+sprint"),
            Pattern.compile("(?i)\\b(sprint|lập) (kế hoạch|plan)")
    };

    private static final Pattern[] ASSIGNEE = {
            Pattern.compile("(?i)\\b(who\\s+should|assignee|suggest\\s+(a\\s+)?(member|person|user|owner))"),
            Pattern.compile("(?i)\\b(recommend|assign|pick|choose)\\b.*\\b(assignee|member|person|developer|dev)"),
            Pattern.compile("(?i)\\bfor\\s+this\\s+task\\b.*\\b(who|which)"),
            Pattern.compile("(?i)\\b(ai nên|giao cho|phân công)")
    };

    private static final Pattern[] WEEKLY = {
            Pattern.compile("(?i)\\b(weekly|week)\\s+(report|summary|update|recap|digest|review)"),
            Pattern.compile("(?i)\\b(week|7\\s*day|tuần)\\s+(qua|trước|này|vừa rồi)"),
            Pattern.compile("(?i)\\b(báo\\s*cáo|tóm\\s*tắt)\\s*tuần"),
            Pattern.compile("(?i)\\bthis\\s+week"),
            Pattern.compile("(?i)\\blast\\s+week")
    };

    private static final Pattern[] DAILY = {
            Pattern.compile("(?i)\\b(daily|today'?s?|this\\s+morning)\\s+(report|summary|update|standup|digest)"),
            Pattern.compile("(?i)\\b(standup|morning\\s+sync|daily\\s+stand)"),
            Pattern.compile("(?i)\\b(hôm\\s*nay|nay)\\s*(thì|nên|làm)"),
            Pattern.compile("(?i)\\bbáo\\s*cáo\\s*ngày")
    };

    private static final Pattern[] BLOCKERS = {
            Pattern.compile("(?i)\\b(blocker|blockage|blocking|stuck|stumbling|cản trở|đang kẹt|đang chặn|bị chặn)"),
            Pattern.compile("(?i)\\bwhat'?s\\s+(blocking|stuck|stopping)"),
            Pattern.compile("(?i)\\bwho\\s+is\\s+blocked"),
            Pattern.compile("(?i)\\bvướng\\s*mắc")
    };

    private static final Pattern[] DEADLINES = {
            Pattern.compile("(?i)\\b(upcoming|next|soon|coming)\\s+(deadline|due\\s*date|dues)"),
            Pattern.compile("(?i)\\bdeadline\\s+(this|next|coming|upcoming)"),
            Pattern.compile("(?i)\\bwhat'?s\\s+due\\s+(next|this|soon|tomorrow)"),
            Pattern.compile("(?i)\\bdue\\s+(soon|this\\s+week|tomorrow|next\\s+week)"),
            Pattern.compile("(?i)\\b(deadline|hạn|quá hạn|sắp đến hạn)"),
            Pattern.compile("(?i)\\b(sắp đến hạn|hạn chót|đến hạn)"),
    };

    private static final Pattern[] GOALS = {
            Pattern.compile("(?i)\\b(goal|okr|objective|key\\s+result|mục\\s*tiêu)"),
            Pattern.compile("(?i)\\bprogress\\s+(on\\s+)?(goal|okr)"),
            Pattern.compile("(?i)\\bhow\\s+are\\s+we\\s+doing\\s+on\\s+(our\\s+)?goals")
    };

    private static final Pattern[] PROJECT_HEALTH = {
            Pattern.compile("(?i)\\bproject\\s+(health|status|score|review)"),
            Pattern.compile("(?i)\\b(show|give)\\s+(me\\s+)?(the\\s+)?project\\s+(health|status)"),
            Pattern.compile("(?i)\\bhow\\s+(are|is)\\s+(the\\s+)?projects?\\s+(doing|going)")
    };

    public Intent classify(String question) {
        if (question == null || question.isBlank()) {
            return Intent.SUMMARIZE_WORKSPACE;
        }
        String q = question.toLowerCase(Locale.ROOT);

        if (matchesAny(q, SUMMARIZE))    return Intent.SUMMARIZE_WORKSPACE;
        if (matchesAny(q, RISKY))        return Intent.MOST_RISKY_PROJECT;
        if (matchesAny(q, OVERLOADED))   return Intent.WHO_IS_OVERLOADED;
        if (matchesAny(q, WHAT_TO_DO))   return Intent.WHAT_SHOULD_I_DO;
        if (matchesAny(q, SPRINT))       return Intent.GENERATE_SPRINT;
        if (matchesAny(q, ASSIGNEE))     return Intent.SUGGEST_ASSIGNEE;
        if (matchesAny(q, WEEKLY))       return Intent.WEEKLY_REPORT;
        if (matchesAny(q, DAILY))        return Intent.DAILY_REPORT;
        if (matchesAny(q, BLOCKERS))     return Intent.BLOCKERS;
        if (matchesAny(q, DEADLINES))    return Intent.UPCOMING_DEADLINES;
        if (matchesAny(q, GOALS))        return Intent.GOALS_PROGRESS;
        if (matchesAny(q, PROJECT_HEALTH)) return Intent.PROJECT_HEALTH;

        return Intent.GENERAL;
    }

    private boolean matchesAny(String text, Pattern[] patterns) {
        for (Pattern p : patterns) {
            if (p.matcher(text).find()) return true;
        }
        return false;
    }
}
