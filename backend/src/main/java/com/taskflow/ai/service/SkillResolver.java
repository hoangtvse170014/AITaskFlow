package com.taskflow.ai.service;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Maps task category keywords to a canonical skill bucket, and skill buckets to
 * roles that are considered a match. Used by the batch task assignment service
 * to enforce the routing rules:
 *   Backend   -> Backend
 *   Frontend  -> Frontend
 *   UI        -> UI
 *   Document  -> Documentation
 */
@Component
public class SkillResolver {

    public static final String SKILL_BACKEND = "BACKEND";
    public static final String SKILL_FRONTEND = "FRONTEND";
    public static final String SKILL_UI = "UI";
    public static final String SKILL_DOCUMENTATION = "DOCUMENTATION";
    public static final String SKILL_FULLSTACK = "FULLSTACK";
    public static final String SKILL_DEVOPS = "DEVOPS";
    public static final String SKILL_QA = "QA";
    public static final String SKILL_OTHER = "OTHER";

    private static final Pattern BACKEND_PATTERN = Pattern.compile(
            "(?i)\\b(backend|api|server|spring|database|sql|postgresql|mysql|redis|kafka|" +
                    "microservice|endpoint|rest|graphql|websocket|auth|jwt|docker|backend dev|" +
                    "java|node\\.js|nestjs|express|go\\b|rust|python|flask|django|fastapi)\\b");

    private static final Pattern FRONTEND_PATTERN = Pattern.compile(
            "(?i)\\b(frontend|react|vue|angular|svelte|next\\.js|nuxt|redux|state management|" +
                    "javascript|typescript|css|html|webpack|vite|babel|spa|pwa|web app|" +
                    "frontend dev|client side|client-side)\\b");

    private static final Pattern UI_PATTERN = Pattern.compile(
            "(?i)\\b(ui|ux|design|figma|sketch|wireframe|mockup|prototype|styling|tailwind|" +
                    "sass|scss|material ui|ant design|chakra|bootstrap|responsive|mobile " +
                    "first|visual design|interaction design)\\b");

    private static final Pattern DOC_PATTERN = Pattern.compile(
            "(?i)\\b(document|documentation|readme|wiki|spec|specification|rfc|" +
                    "api doc|user guide|technical writing|changelog|release notes|" +
                    "swagger|openapi|architecture doc|design doc|adr)\\b");

    private static final Pattern DEVOPS_PATTERN = Pattern.compile(
            "(?i)\\b(devops|ci/cd|jenkins|github actions|kubernetes|k8s|helm|terraform|" +
                    "ansible|aws|gcp|azure|cloud|monitoring|prometheus|grafana|elk|sre|infra)\\b");

    private static final Pattern QA_PATTERN = Pattern.compile(
            "(?i)\\b(qa|test|testing|unittest|integration test|e2e|selenium|cypress|" +
                    "playwright|junit|jest|mocha|coverage|bug report|test plan)\\b");

    public String detectSkill(String title, String description, String categoryHint) {
        if (categoryHint != null && !categoryHint.isBlank()) {
            String hint = categoryHint.trim().toUpperCase();
            return switch (hint) {
                case "BACKEND", "BE", "SERVER" -> SKILL_BACKEND;
                case "FRONTEND", "FE", "CLIENT" -> SKILL_FRONTEND;
                case "UI", "DESIGN" -> SKILL_UI;
                case "DOC", "DOCUMENT", "DOCUMENTATION" -> SKILL_DOCUMENTATION;
                case "DEVOPS", "INFRA" -> SKILL_DEVOPS;
                case "QA", "TEST", "TESTING" -> SKILL_QA;
                case "FULLSTACK", "FS" -> SKILL_FULLSTACK;
                default -> SKILL_OTHER;
            };
        }
        String haystack = ((title == null ? "" : title) + " " + (description == null ? "" : description)).toLowerCase();
        if (haystack.isBlank()) return SKILL_OTHER;

        // Documentation wins if it has explicit doc language AND no strong code keyword.
        boolean backend = BACKEND_PATTERN.matcher(haystack).find();
        boolean frontend = FRONTEND_PATTERN.matcher(haystack).find();
        boolean ui = UI_PATTERN.matcher(haystack).find();
        boolean doc = DOC_PATTERN.matcher(haystack).find();
        boolean devops = DEVOPS_PATTERN.matcher(haystack).find();
        boolean qa = QA_PATTERN.matcher(haystack).find();

        int codeHits = (backend ? 1 : 0) + (frontend ? 1 : 0) + (devops ? 1 : 0);
        if (codeHits == 0 && doc) return SKILL_DOCUMENTATION;
        if (doc && !ui) return SKILL_DOCUMENTATION;

        if (backend && frontend) return SKILL_FULLSTACK;
        if (backend) return SKILL_BACKEND;
        if (frontend) return SKILL_FRONTEND;
        if (ui) return SKILL_UI;
        if (devops) return SKILL_DEVOPS;
        if (qa) return SKILL_QA;
        if (doc) return SKILL_DOCUMENTATION;
        return SKILL_OTHER;
    }

    /**
     * Returns the set of role names (lowercased) that match a given skill bucket.
     * The role names are compared against WorkspaceMember.role.name.
     */
    public Set<String> rolesForSkill(String skill) {
        return switch (skill) {
            case SKILL_BACKEND -> Set.of("backend", "backend developer", "backend dev", "be", "server", "server-side");
            case SKILL_FRONTEND -> Set.of("frontend", "frontend developer", "frontend dev", "fe", "client-side", "web developer");
            case SKILL_UI -> Set.of("ui", "ui designer", "ux", "ux/ui", "designer", "ui/ux", "ui designer");
            case SKILL_DOCUMENTATION -> Set.of("technical writer", "writer", "documentation", "docs", "document writer", "content");
            case SKILL_DEVOPS -> Set.of("devops", "sre", "infrastructure", "platform");
            case SKILL_QA -> Set.of("qa", "qa engineer", "tester", "test engineer", "quality");
            case SKILL_FULLSTACK -> Set.of("fullstack", "full-stack", "full stack", "full stack developer");
            default -> Set.of();
        };
    }

    public boolean roleMatchesSkill(String roleName, String skill) {
        if (roleName == null || skill == null) return false;
        String n = roleName.toLowerCase();
        for (String candidate : rolesForSkill(skill)) {
            if (n.contains(candidate)) return true;
        }
        return false;
    }
}