package com.taskflow.config;

import com.taskflow.entity.*;
import com.taskflow.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Component
@Profile("!prod")
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final PageRepository pageRepository;
    private final BlockRepository blockRepository;
    private final GoalRepository goalRepository;
    private final KeyResultRepository keyResultRepository;
    private final NotificationRepository notificationRepository;
    private final ActivityLogRepository activityLogRepository;
    private final MemberWorkloadRepository memberWorkloadRepository;
    private final SearchHistoryRepository searchHistoryRepository;
    private final TaskCommentRepository taskCommentRepository;
    private final PasswordEncoder passwordEncoder;

    private Workspace workspace;
    private List<User> users = new ArrayList<>();
    private List<Project> projects = new ArrayList<>();
    private List<Task> allTasks = new ArrayList<>();
    private Role ownerRole, adminRole, managerRole, memberRole;

    @Override
    @Transactional
    public void run(String... args) {
        if (roleRepository.count() == 0) {
            initializeRolesAndPermissions();
        } else {
            // Ensure GUEST role exists
            ensureGuestRoleExists();
        }

        if (userRepository.count() == 0) {
            initializeDemoData();
        } else {
            log.info("Demo data already exists, skipping initialization to avoid duplicates.");
        }
    }

    private void ensureGuestRoleExists() {
        if (roleRepository.findByName(Role.GUEST).isEmpty()) {
            log.info("Creating GUEST role...");
            // GUEST role with no permissions (can only view)
            Role guestRole = Role.builder()
                    .name(Role.GUEST)
                    .description("Guest")
                    .priority(Role.PRIORITY_GUEST)
                    .isWorkspaceRole(true)
                    .permissions(new HashSet<>())
                    .build();
            roleRepository.save(guestRole);
            log.info("GUEST role created successfully");
        }
    }

    private void clearAllData() {
        searchHistoryRepository.deleteAll();
        activityLogRepository.deleteAll();
        notificationRepository.deleteAll();
        taskCommentRepository.deleteAll();
        taskRepository.deleteAll();
        keyResultRepository.deleteAll();
        goalRepository.deleteAll();
        blockRepository.deleteAll();
        pageRepository.deleteAll();
        projectRepository.deleteAll();
        workspaceMemberRepository.deleteAll();
        workspaceRepository.deleteAll();
        userRepository.deleteAll();
    }

    private void initializeRolesAndPermissions() {
        log.info("Initializing roles and permissions...");
        Set<Permission> allPermissions = createPermissions();
        createRolesWithPermissions(allPermissions);
        log.info("Roles and permissions initialized");
    }

    private void initializeDemoData() {
        createUsers();
        createWorkspace();
        createWorkspaceMembers();
        createProjects();
        createTasks();
        createMemberWorkloads();
        createPages();
        createGoals();
        createNotifications();
        createActivityLogs();
        createSearchHistory();
        createTaskComments();

        printVerificationReport();

        log.info("========================================");
        log.info("  DEMO DATA CREATED SUCCESSFULLY!");
        log.info("========================================");
    }

    private Set<Permission> createPermissions() {
        Set<Permission> permissions = new HashSet<>();
        permissions.add(createPermission("workspace:view", "View workspace", Permission.RESOURCE_WORKSPACE, Permission.ACTION_VIEW));
        permissions.add(createPermission("workspace:edit", "Edit workspace", Permission.RESOURCE_WORKSPACE, Permission.ACTION_EDIT));
        permissions.add(createPermission("workspace:manage", "Manage workspace settings", Permission.RESOURCE_WORKSPACE, Permission.ACTION_MANAGE));
        permissions.add(createPermission("member:view", "View members", Permission.RESOURCE_MEMBER, Permission.ACTION_VIEW));
        permissions.add(createPermission("member:invite", "Invite members", Permission.RESOURCE_MEMBER, "INVITE"));
        permissions.add(createPermission("member:manage", "Manage members", Permission.RESOURCE_MEMBER, Permission.ACTION_MANAGE));
        permissions.add(createPermission("project:view", "View projects", Permission.RESOURCE_PROJECT, Permission.ACTION_VIEW));
        permissions.add(createPermission("project:create", "Create projects", Permission.RESOURCE_PROJECT, Permission.ACTION_CREATE));
        permissions.add(createPermission("project:edit", "Edit projects", Permission.RESOURCE_PROJECT, Permission.ACTION_EDIT));
        permissions.add(createPermission("project:manage", "Manage projects", Permission.RESOURCE_PROJECT, Permission.ACTION_MANAGE));
        permissions.add(createPermission("task:view", "View tasks", Permission.RESOURCE_TASK, Permission.ACTION_VIEW));
        permissions.add(createPermission("task:create", "Create tasks", Permission.RESOURCE_TASK, Permission.ACTION_CREATE));
        permissions.add(createPermission("task:edit", "Edit tasks", Permission.RESOURCE_TASK, Permission.ACTION_EDIT));
        permissions.add(createPermission("task:assign", "Assign tasks", Permission.RESOURCE_TASK, "ASSIGN"));
        permissions.add(createPermission("page:view", "View pages", Permission.RESOURCE_PAGE, Permission.ACTION_VIEW));
        permissions.add(createPermission("page:create", "Create pages", Permission.RESOURCE_PAGE, Permission.ACTION_CREATE));
        permissions.add(createPermission("page:edit", "Edit pages", Permission.RESOURCE_PAGE, Permission.ACTION_EDIT));
        permissionRepository.saveAll(permissions);
        return permissions;
    }

    private Permission createPermission(String name, String description, String resourceType, String actionType) {
        return Permission.builder().name(name).description(description).resourceType(resourceType).actionType(actionType).build();
    }

    private void createRolesWithPermissions(Set<Permission> allPermissions) {
        ownerRole = Role.builder().name(Role.OWNER).description("Workspace owner").priority(Role.PRIORITY_OWNER).isWorkspaceRole(true).permissions(allPermissions).build();
        roleRepository.save(ownerRole);

        adminRole = Role.builder().name(Role.ADMIN).description("Admin").priority(Role.PRIORITY_ADMIN).isWorkspaceRole(true).permissions(allPermissions).build();
        roleRepository.save(adminRole);

        managerRole = Role.builder().name(Role.MANAGER).description("Manager").priority(Role.PRIORITY_MANAGER).isWorkspaceRole(true).permissions(allPermissions).build();
        roleRepository.save(managerRole);

        memberRole = Role.builder().name(Role.MEMBER).description("Member").priority(Role.PRIORITY_MEMBER).isWorkspaceRole(true).permissions(allPermissions).build();
        roleRepository.save(memberRole);

        Role guestRole = Role.builder().name(Role.GUEST).description("Guest").priority(Role.PRIORITY_GUEST).isWorkspaceRole(true).permissions(allPermissions).build();
        roleRepository.save(guestRole);
    }

    private void createUsers() {
        String password = passwordEncoder.encode("demo123");
        
        users.add(createUser("admin@technova.com", "Nguyễn Minh", "Project Manager", password, "👨‍💼"));
        users.add(createUser("long.vo@technova.com", "Võ Long", "Backend Developer", password, "👨‍💻"));
        users.add(createUser("huy.nguyen@technova.com", "Nguyễn Huy", "Frontend Developer", password, "👨‍🎨"));
        users.add(createUser("trang.pham@technova.com", "Phạm Trang", "UI/UX Designer", password, "👩‍🎨"));
        users.add(createUser("dung.lee@technova.com", "Lê Dũng", "QA Engineer", password, "🧪"));
        users.add(createUser("khanh.ho@technova.com", "Hồ Khanh", "DevOps Engineer", password, "🔧"));
        users.add(createUser("linh.tran@technova.com", "Trần Linh", "Business Analyst", password, "📊"));
        users.add(createUser("tu.an@technova.com", "An Tử", "Product Owner", password, "🎯"));
        users.add(createUser("minh.nguyen@technova.com", "Nguyễn Minh", "Backend Developer", password, "👨‍🔬"));
        users.add(createUser("thao.vu@technova.com", "Vũ Thảo", "Frontend Developer", password, "👩‍💻"));
        users.add(createUser("hai.dang@technova.com", "Đặng Hải", "QA Engineer", password, "🕵️"));
        users.add(createUser("lan.phung@technova.com", "Phùng Lan", "Business Analyst", password, "📋"));
        
        log.info("Created {} users", users.size());
    }

    private User createUser(String email, String fullName, String role, String password, String avatar) {
        return userRepository.save(User.builder()
                .email(email)
                .password(password)
                .fullName(fullName)
                .avatarUrl("https://api.dicebear.com/7.x/avataaars/svg?seed=" + email.hashCode())
                .build());
    }

    private void createWorkspace() {
        workspace = workspaceRepository.save(Workspace.builder()
                .name("TechNova Solutions")
                .slug("technova")
                .description("Software Development Company - Building innovative solutions for the digital age")
                .owner(users.get(0))
                .build());
        log.info("Created workspace: {}", workspace.getName());
    }

    private void createWorkspaceMembers() {
        int[] roles = {0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}; // Owner, Admin, Managers, Members
        String[] roleNames = {"OWNER", "ADMIN", "MANAGER", "MEMBER"};
        
        for (int i = 0; i < users.size(); i++) {
            Role role = switch (roles[i]) {
                case 0 -> roleRepository.findByName(roleNames[roles[i]]).orElse(ownerRole);
                case 1 -> roleRepository.findByName(roleNames[roles[i]]).orElse(adminRole);
                default -> roleRepository.findByName(roleNames[Math.min(roles[i], 3)]).orElse(memberRole);
            };
            
            workspaceMemberRepository.save(WorkspaceMember.builder()
                    .workspace(workspace)
                    .user(users.get(i))
                    .role(role)
                    .joinedAt(LocalDateTime.now().minusMonths(12 - i))
                    .build());
        }
        log.info("Created {} workspace members", users.size());
    }

    private void createProjects() {
        projects.add(createProject("TaskFlow AI Platform", "AI", "AI-powered project management platform with smart recommendations", "#6366f1", "🤖", 1));
        projects.add(createProject("Mobile Banking App", "MBA", "Secure mobile banking application for iOS and Android", "#10b981", "🏦", 1));
        projects.add(createProject("Hospital Management System", "HMS", "Comprehensive hospital information management system", "#f59e0b", "🏥", 0));
        projects.add(createProject("E-learning Platform", "ELP", "Online learning platform with AI tutoring", "#ec4899", "📚", 2));
        projects.add(createProject("CRM System", "CRM", "Customer relationship management system", "#8b5cf6", "💼", 3));
        
        log.info("Created {} projects", projects.size());
    }

    private Project createProject(String name, String key, String description, String color, String icon, int status) {
        return projectRepository.save(Project.builder()
                .workspace(workspace)
                .name(name)
                .key(key)
                .description(description)
                .color(color)
                .icon(icon)
                .build());
    }

    private void createTasks() {
        LocalDate today = LocalDate.now();
        Random random = new Random(42);
        int taskNum = 1;
        
        // Project 1: TaskFlow AI Platform - 20 tasks
        Project p1 = projects.get(0);
        taskNum = createTasksForProject(p1, new String[][] {
            {"Implement AI task recommendation engine", "IN_PROGRESS", "HIGH", "5"},
            {"Build AI dashboard with analytics", "IN_PROGRESS", "HIGH", "3"},
            {"Create chatbot workspace assistant", "TODO", "HIGH", "2"},
            {"Implement document summarization", "TODO", "MEDIUM", "4"},
            {"Build risk detection system", "TODO", "HIGH", "3"},
            {"Create AI workload analysis", "IN_PROGRESS", "MEDIUM", "1"},
            {"Implement smart search", "TODO", "MEDIUM", "2"},
            {"Build AI notification system", "REVIEW", "LOW", "1"},
            {"Implement AI goal tracking", "TODO", "MEDIUM", "3"},
            {"Create AI report generator", "TODO", "LOW", "2"},
            {"Build AI meeting summarizer", "TODO", "HIGH", "4"},
            {"Implement AI requirements generator", "TODO", "MEDIUM", "3"},
            {"Database schema for AI models", "DONE", "HIGH", "1"},
            {"API endpoints for AI service", "DONE", "HIGH", "1"},
            {"Setup AI model training pipeline", "DONE", "HIGH", "1"},
            {"Integration tests for AI features", "REVIEW", "MEDIUM", "2"},
            {"Performance optimization for AI", "IN_PROGRESS", "MEDIUM", "3"},
            {"User feedback collection system", "TODO", "LOW", "2"},
            {"AI accuracy monitoring", "TODO", "MEDIUM", "2"},
            {"Documentation for AI features", "DONE", "LOW", "1"}
        }, today, random, taskNum);
        
        // Project 2: Mobile Banking App - 18 tasks
        Project p2 = projects.get(1);
        taskNum = createTasksForProject(p2, new String[][] {
            {"Implement biometric authentication", "IN_PROGRESS", "HIGH", "3"},
            {"Build transaction history screen", "IN_PROGRESS", "HIGH", "2"},
            {"Create payment processing module", "TODO", "URGENT", "5"},
            {"Implement push notifications", "TODO", "HIGH", "2"},
            {"Build account management", "TODO", "HIGH", "3"},
            {"Create loan application flow", "TODO", "MEDIUM", "4"},
            {"Implement currency exchange", "REVIEW", "MEDIUM", "2"},
            {"Build credit card management", "IN_PROGRESS", "HIGH", "3"},
            {"Create investment dashboard", "TODO", "MEDIUM", "3"},
            {"Implement fraud detection", "TODO", "HIGH", "4"},
            {"Security audit fixes", "REVIEW", "URGENT", "2"},
            {"UI/UX final review", "REVIEW", "HIGH", "1"},
            {"Backend API integration", "DONE", "HIGH", "2"},
            {"Database optimization", "DONE", "MEDIUM", "2"},
            {"Beta testing phase", "IN_PROGRESS", "HIGH", "3"},
            {"App store submission", "TODO", "HIGH", "1"},
            {"Performance testing", "TODO", "MEDIUM", "2"},
            {"Security penetration testing", "TODO", "HIGH", "3"}
        }, today, random, taskNum);
        
        // Project 3: Hospital Management System - 15 tasks
        Project p3 = projects.get(2);
        taskNum = createTasksForProject(p3, new String[][] {
            {"Requirements gathering", "IN_PROGRESS", "HIGH", "4"},
            {"System architecture design", "TODO", "HIGH", "3"},
            {"Database schema design", "TODO", "HIGH", "2"},
            {"User role management", "TODO", "MEDIUM", "3"},
            {"Patient registration module", "TODO", "HIGH", "4"},
            {"Appointment scheduling", "TODO", "HIGH", "3"},
            {"Medical records management", "TODO", "HIGH", "5"},
            {"Billing and insurance", "TODO", "MEDIUM", "4"},
            {"Laboratory integration", "TODO", "MEDIUM", "3"},
            {"Pharmacy management", "TODO", "MEDIUM", "3"},
            {"Emergency handling system", "TODO", "HIGH", "4"},
            {"Reporting module", "TODO", "MEDIUM", "3"},
            {"Mobile app design", "TODO", "MEDIUM", "3"},
            {"Integration planning", "TODO", "MEDIUM", "2"},
            {"Stakeholder presentation", "TODO", "HIGH", "1"}
        }, today, random, taskNum);
        
        // Project 4: E-learning Platform - 14 tasks
        Project p4 = projects.get(3);
        taskNum = createTasksForProject(p4, new String[][] {
            {"Course content management", "DONE", "HIGH", "3"},
            {"Video streaming integration", "DONE", "HIGH", "2"},
            {"Quiz and assessment system", "DONE", "HIGH", "2"},
            {"AI tutoring implementation", "IN_PROGRESS", "HIGH", "5"},
            {"Progress tracking dashboard", "IN_PROGRESS", "MEDIUM", "2"},
            {"Student performance analytics", "REVIEW", "HIGH", "3"},
            {"Certification system", "REVIEW", "MEDIUM", "2"},
            {"Discussion forum", "TODO", "MEDIUM", "3"},
            {"Gamification features", "TODO", "LOW", "4"},
            {"Mobile app development", "TODO", "MEDIUM", "4"},
            {"Payment integration", "DONE", "HIGH", "2"},
            {"User onboarding flow", "DONE", "MEDIUM", "2"},
            {"Content review workflow", "REVIEW", "MEDIUM", "2"},
            {"Beta user testing", "REVIEW", "HIGH", "2"}
        }, today, random, taskNum);
        
        // Project 5: CRM System - 13 tasks (Completed)
        Project p5 = projects.get(4);
        taskNum = createTasksForProject(p5, new String[][] {
            {"Contact management", "DONE", "HIGH", "2"},
            {"Lead tracking", "DONE", "HIGH", "2"},
            {"Sales pipeline", "DONE", "HIGH", "3"},
            {"Email integration", "DONE", "MEDIUM", "2"},
            {"Report generation", "DONE", "MEDIUM", "2"},
            {"Dashboard analytics", "DONE", "HIGH", "2"},
            {"API documentation", "DONE", "MEDIUM", "1"},
            {"User training materials", "DONE", "LOW", "1"},
            {"Deployment to production", "DONE", "HIGH", "1"},
            {"Client onboarding", "DONE", "HIGH", "2"},
            {"Performance optimization", "DONE", "MEDIUM", "2"},
            {"Bug fixes", "DONE", "HIGH", "2"},
            {"Final client sign-off", "DONE", "HIGH", "1"}
        }, today, random, taskNum);
        
        log.info("Created {} tasks", allTasks.size());
    }

    private int createTasksForProject(Project project, String[][] taskData, LocalDate today, Random random, int startNum) {
        int taskNum = startNum;
        LocalDate twoWeeksAgo = today.minusWeeks(2);
        
        for (String[] data : taskData) {
            TaskStatus status = TaskStatus.valueOf(data[1]);
            TaskPriority priority = TaskPriority.valueOf(data[2]);
            int estimate = Integer.parseInt(data[3]);
            
            User reporter = users.get(random.nextInt(users.size()));
            User assignee = random.nextFloat() < 0.85f ? users.get(random.nextInt(users.size())) : null;
            
            // Create overdue tasks
            LocalDate dueDate;
            if (status == TaskStatus.DONE) {
                dueDate = today.minusDays(random.nextInt(14) + 1);
            } else if (data[1].equals("TODO") && random.nextFloat() < 0.4f) {
                // 40% of TODO tasks are overdue
                dueDate = twoWeeksAgo.minusDays(random.nextInt(7));
            } else if (data[1].equals("IN_PROGRESS") && random.nextFloat() < 0.3f) {
                // 30% of IN_PROGRESS tasks are overdue
                dueDate = twoWeeksAgo.minusDays(random.nextInt(5));
            } else {
                dueDate = today.plusDays(random.nextInt(30) + 1);
            }
            
            Task task = Task.builder()
                    .project(project)
                    .taskNumber(taskNum++)
                    .title(data[0])
                    .description("Detailed implementation for: " + data[0] + ". This task requires careful planning and execution.")
                    .status(status)
                    .priority(priority)
                    .assignee(assignee)
                    .reporter(reporter)
                    .dueDate(dueDate)
                    .position(taskNum)
                    .build();
            
            task.setLabels(List.of(
                    Task.Label.builder().id(UUID.randomUUID().toString())
                            .name(priority.name()).color(getPriorityColor(priority)).build()
            ));
            
            if (random.nextBoolean()) {
                task.setChecklist(List.of(
                        Task.ChecklistItem.builder().id(UUID.randomUUID().toString()).text("Initial research").completed(random.nextBoolean()).build(),
                        Task.ChecklistItem.builder().id(UUID.randomUUID().toString()).text("Implementation").completed(random.nextBoolean()).build(),
                        Task.ChecklistItem.builder().id(UUID.randomUUID().toString()).text("Testing").completed(random.nextBoolean()).build(),
                        Task.ChecklistItem.builder().id(UUID.randomUUID().toString()).text("Review").completed(status == TaskStatus.DONE).build()
                ));
            }
            
            allTasks.add(taskRepository.save(task));
        }
        return taskNum;
    }

    private String getPriorityColor(TaskPriority priority) {
        return switch (priority) {
            case URGENT -> "#ef4444";
            case HIGH -> "#f59e0b";
            case MEDIUM -> "#3b82f6";
            case LOW -> "#6b7280";
        };
    }

    private void createMemberWorkloads() {
        LocalDate today = LocalDate.now();
        
        // Minh (Index 0) - Project Manager - 92% overloaded
        createWorkload(users.get(0), 9, 12, 7, 2, 92, "OVERLOADED");
        // Long (Index 1) - Backend Dev - 81% 
        createWorkload(users.get(1), 7, 10, 5, 1, 81, "OVERLOADED");
        // Huy (Index 2) - Frontend Dev - 38%
        createWorkload(users.get(2), 3, 5, 2, 0, 38, "BALANCED");
        // Trang (Index 3) - UI/UX - 25%
        createWorkload(users.get(3), 2, 4, 1, 0, 25, "UNDERUTILIZED");
        // Dung (Index 4) - QA - 66%
        createWorkload(users.get(4), 5, 8, 4, 1, 66, "BALANCED");
        // Khanh (Index 5) - DevOps - 75%
        createWorkload(users.get(5), 6, 9, 5, 0, 75, "OVERLOADED");
        // Linh (Index 6) - BA - 58%
        createWorkload(users.get(6), 4, 7, 3, 0, 58, "BALANCED");
        // Tu (Index 7) - PO - 70%
        createWorkload(users.get(7), 5, 8, 5, 1, 70, "OVERLOADED");
        // Minh2 (Index 8) - Backend Dev - 55%
        createWorkload(users.get(8), 4, 6, 3, 0, 55, "BALANCED");
        // Thao (Index 9) - Frontend Dev - 45%
        createWorkload(users.get(9), 3, 5, 2, 0, 45, "BALANCED");
        // Hai (Index 10) - QA - 50%
        createWorkload(users.get(10), 4, 6, 3, 0, 50, "BALANCED");
        // Lan (Index 11) - BA - 35%
        createWorkload(users.get(11), 2, 4, 2, 0, 35, "UNDERUTILIZED");
        
        log.info("Created member workloads");
    }

    private void createWorkload(User user, int open, int total, int inProgress, int blocked, int percentage, String status) {
        WorkspaceMember member = workspaceMemberRepository.findAll().stream()
                .filter(wm -> wm.getUser().getId().equals(user.getId()))
                .findFirst().orElseThrow();
        
        memberWorkloadRepository.save(MemberWorkload.builder()
                .member(member)
                .workspaceId(workspace.getId())
                .date(LocalDate.now())
                .openTasks(open)
                .completedTasks(total - open)
                .inProgressTasks(inProgress)
                .blockedTasks(blocked)
                .totalHoursEstimated(total * 8.0)
                .totalHoursLogged((total - open) * 6.0)
                .workloadPercentage(percentage)
                .status(status)
                .build());
    }

    private void createPages() {
        createProductRequirementsPage();
        createSprintPlanningPage();
        createMeetingNotesPage();
        createArchitecturePage();
        createApiSpecificationPage();
        createFrontendGuidelinesPage();
        createCodingConventionPage();
        createDeploymentGuidePage();
        createTestingStrategyPage();
        createSecurityChecklistPage();
        createDatabaseDesignPage();
        createRetrospectivePage();
        
        log.info("Created {} pages with blocks", pageRepository.count());
    }

    private void createProductRequirementsPage() {
        Page page = savePage("Product Requirements", "📋", "product-requirements");
        int pos = 1;
        pos = createBlock(page, "heading", "TaskFlow AI Platform - Requirements v2.0", pos);
        pos = createBlock(page, "paragraph", "This document outlines the comprehensive product requirements for the AI-powered TaskFlow platform. The platform aims to revolutionize project management with intelligent automation.", pos);
        pos = createBlock(page, "heading", "Core Features", pos);
        pos = createBlock(page, "bulleted_list", "AI Task Recommendation Engine\nSmart Workload Balancing\nPredictive Risk Detection\nAutomated Meeting Summarization\nIntelligent Search and Discovery", pos);
        pos = createBlock(page, "heading", "User Stories", pos);
        pos = createBlock(page, "numbered_list", "As a PM, I want AI to suggest optimal task assignments based on team capacity\nAs a developer, I want smart notifications that prioritize relevant updates\nAs a manager, I want risk predictions before they become issues", pos);
        pos = createBlock(page, "heading", "Technical Requirements", pos);
        pos = createBlock(page, "bulleted_list", "React 18+ with TypeScript\nSpring Boot 3.x microservices\nPostgreSQL 15+ database\nRedis for caching\nGoogle Gemini API integration", pos);
        pos = createBlock(page, "heading", "Success Metrics", pos);
        createBlock(page, "checklist", "Reduce task assignment time by 50%\nImprove deadline adherence by 30%\nIncrease team productivity by 25%", pos);
    }

    private void createSprintPlanningPage() {
        Page page = savePage("Sprint Planning Q2 2026", "🏃", "sprint-planning");
        int pos = 1;
        pos = createBlock(page, "heading", "Q2 2026 Sprint Schedule", pos);
        pos = createBlock(page, "paragraph", "This quarter focuses on AI integration and mobile app development. We have 12 weeks to deliver key features.", pos);
        pos = createBlock(page, "heading", "Sprint 1: Foundation (April 1-14)", pos);
        pos = createBlock(page, "bulleted_list", "Setup AI development environment\nDesign database schema for AI models\nCreate API endpoints for AI service", pos);
        pos = createBlock(page, "heading", "Sprint 2: Core AI Features (April 15-28)", pos);
        pos = createBlock(page, "bulleted_list", "Implement task recommendation engine\nBuild AI dashboard\nCreate workspace assistant chatbot", pos);
        pos = createBlock(page, "heading", "Sprint 3: Advanced AI (May 1-14)", pos);
        pos = createBlock(page, "bulleted_list", "Implement document summarization\nBuild risk detection system\nCreate smart search functionality", pos);
        pos = createBlock(page, "heading", "Sprint 4: Mobile Integration (May 15-28)", pos);
        pos = createBlock(page, "bulleted_list", "Mobile app AI features\nPush notification system\nOffline mode with AI sync", pos);
        pos = createBlock(page, "heading", "Sprint 5: Polish (June 1-14)", pos);
        createBlock(page, "bulleted_list", "Performance optimization\nSecurity audit\nUser acceptance testing", pos);
    }

    private void createMeetingNotesPage() {
        Page page = savePage("Meeting Notes - Sprint Planning", "📝", "meeting-notes");
        int pos = 1;
        pos = createBlock(page, "heading", "Weekly Team Standup - June 2, 2026", pos);
        pos = createBlock(page, "paragraph", "Attendees: Minh (PM), Long (Backend), Huy (Frontend), Trang (Design), Dung (QA)", pos);
        pos = createBlock(page, "heading", "Agenda", pos);
        pos = createBlock(page, "bulleted_list", "Sprint 5 progress review\nAI feature demo\nBlockers discussion\nNext sprint planning", pos);
        pos = createBlock(page, "heading", "Discussion Points", pos);
        pos = createBlock(page, "numbered_list", "AI recommendation engine showing 85% accuracy\nMobile banking app security audit passed\nHospital Management System in planning phase\nE-learning platform beta testing starts next week", pos);
        pos = createBlock(page, "heading", "Blockers", pos);
        pos = createBlock(page, "bulleted_list", "Long: Waiting for API documentation from banking partner\nHuy: Design assets for new dashboard delayed by 2 days\nKhanh: Need additional AWS budget for AI training", pos);
        pos = createBlock(page, "heading", "Action Items", pos);
        createBlock(page, "numbered_list", "Minh: Schedule follow-up with banking partner (Due: June 4)\nTrang: Deliver dashboard designs by June 5\nLong: Complete API integration documentation\nDung: Prepare test cases for AI features", pos);
    }

    private void createArchitecturePage() {
        Page page = savePage("System Architecture", "🏗️", "architecture");
        int pos = 1;
        pos = createBlock(page, "heading", "TaskFlow AI Platform Architecture", pos);
        pos = createBlock(page, "paragraph", "Modern microservices architecture with AI-first design principles.", pos);
        pos = createBlock(page, "heading", "High-Level Architecture", pos);
        pos = createBlock(page, "code", "┌─────────────────────────────────────────────────────┐\n│                    Load Balancer                     │\n└─────────────────────────────────────────────────────┘\n                           │\n        ┌──────────────────┼──────────────────┐\n        ▼                  ▼                  ▼\n┌──────────────┐  ┌──────────────┐  ┌──────────────┐\n│  Frontend    │  │   Mobile     │  │  Real-time   │\n│  (Next.js)   │  │   (RN)       │  │  (WebSocket) │\n└──────────────┘  └──────────────┘  └──────────────┘\n                           │\n┌──────────────────────────────────────────────────────┐\n│                   API Gateway                         │\n│  - Authentication (JWT)                              │\n│  - Rate Limiting                                     │\n│  - Request Routing                                   │\n└──────────────────────────────────────────────────────┘", pos);
        pos = createBlock(page, "heading", "Core Services", pos);
        pos = createBlock(page, "bulleted_list", "User Service: Authentication, authorization, profiles\nProject Service: Projects, tasks, workflows\nNotification Service: Real-time updates, emails\nAI Service: Recommendations, analysis, predictions\nAnalytics Service: Metrics, reporting, dashboards", pos);
        pos = createBlock(page, "heading", "Data Layer", pos);
        pos = createBlock(page, "bulleted_list", "PostgreSQL: Primary data store\nRedis: Caching, sessions, real-time\nElasticsearch: Full-text search\nS3: File storage, backups", pos);
        createBlock(page, "quote", "Architecture principle: Build for scale, design for simplicity.", pos);
    }

    private void createApiSpecificationPage() {
        Page page = savePage("API Specification", "🔌", "api-specification");
        int pos = 1;
        pos = createBlock(page, "heading", "TaskFlow REST API v2", pos);
        pos = createBlock(page, "paragraph", "Comprehensive API documentation for TaskFlow platform integration.", pos);
        pos = createBlock(page, "heading", "Authentication", pos);
        pos = createBlock(page, "code", "POST /api/auth/login\nContent-Type: application/json\n\n{\n  \"email\": \"user@example.com\",\n  \"password\": \"password123\"\n}\n\nResponse:\n{\n  \"token\": \"eyJhbGciOiJIUzI1NiIs...\",\n  \"refreshToken\": \"dGhpcyBpcyBhIHJl...\"\n}", pos);
        pos = createBlock(page, "heading", "AI Endpoints", pos);
        pos = createBlock(page, "bulleted_list", "POST /api/ai/tasks/recommend - Get task assignment recommendation\nPOST /api/ai/projects/{id}/analyze - Analyze project health\nPOST /api/ai/pages/{id}/summarize - Summarize page content\nPOST /api/ai/workspace/chat - Workspace AI assistant", pos);
        pos = createBlock(page, "heading", "Task Endpoints", pos);
        pos = createBlock(page, "bulleted_list", "GET /api/projects/{projectId}/tasks - List project tasks\nPOST /api/projects/{projectId}/tasks - Create new task\nPATCH /api/tasks/{taskId} - Update task\nDELETE /api/tasks/{taskId} - Delete task", pos);
        createBlock(page, "code", "Example Task Request:\n{\n  \"title\": \"Implement user authentication\",\n  \"description\": \"Add JWT-based authentication\",\n  \"status\": \"TODO\",\n  \"priority\": \"HIGH\",\n  \"assigneeId\": \"user-uuid\",\n  \"dueDate\": \"2026-06-15\"\n}", pos);
    }

    private void createFrontendGuidelinesPage() {
        Page page = savePage("Frontend Guidelines", "🎨", "frontend-guidelines");
        int pos = 1;
        pos = createBlock(page, "heading", "Frontend Development Standards", pos);
        pos = createBlock(page, "paragraph", "Coding standards and best practices for the TaskFlow frontend team.", pos);
        pos = createBlock(page, "heading", "Technology Stack", pos);
        pos = createBlock(page, "bulleted_list", "Next.js 14+ with App Router\nTypeScript (strict mode enabled)\nTailwind CSS for styling\nReact Query for data fetching\nZustand for state management", pos);
        pos = createBlock(page, "heading", "Component Structure", pos);
        pos = createBlock(page, "code", "components/\n├── ui/              # Base UI components\n│   ├── button.tsx\n│   ├── input.tsx\n│   └── card.tsx\n├── features/        # Feature-specific components\n│   ├── tasks/\n│   ├── projects/\n│   └── ai/\n└── layouts/        # Layout components", pos);
        pos = createBlock(page, "heading", "Code Conventions", pos);
        pos = createBlock(page, "numbered_list", "Use functional components with hooks\nImplement proper TypeScript types\nFollow atomic design principles\nWrite self-documenting code\nAdd meaningful comments only when necessary", pos);
        createBlock(page, "checklist", "Setup ESLint and Prettier\nConfigure import sorting\nEnable strict TypeScript\nSetup automated testing", pos);
    }

    private void createCodingConventionPage() {
        Page page = savePage("Coding Conventions", "📝", "coding-conventions");
        int pos = 1;
        pos = createBlock(page, "heading", "Code Style Guide", pos);
        pos = createBlock(page, "paragraph", "Consistent coding standards ensure maintainability and collaboration.", pos);
        pos = createBlock(page, "heading", "Naming Conventions", pos);
        pos = createBlock(page, "bulleted_list", "Components: PascalCase (UserProfile)\nFunctions: camelCase (getUserData)\nConstants: UPPER_SNAKE_CASE (MAX_RETRY_COUNT)\nFiles: kebab-case (user-profile.tsx)\nDatabase: snake_case (created_at)", pos);
        pos = createBlock(page, "heading", "Java/TypeScript Guidelines", pos);
        pos = createBlock(page, "code", "// Good\nconst userData = await fetchUser(userId);\nif (userData && userData.isActive) {\n  processUser(userData);\n}\n\n// Avoid\nconst d = await f(uId);\nif (d?.isActive) {\n  p(d);\n}", pos);
        pos = createBlock(page, "heading", "Code Review Checklist", pos);
        createBlock(page, "checklist", "Code follows naming conventions\nNo hardcoded values\nProper error handling\nUnit tests included\nDocumentation updated", pos);
    }

    private void createDeploymentGuidePage() {
        Page page = savePage("Deployment Guide", "🚀", "deployment-guide");
        int pos = 1;
        pos = createBlock(page, "heading", "Deployment Procedures", pos);
        pos = createBlock(page, "paragraph", "Step-by-step guide for deploying TaskFlow to production environments.", pos);
        pos = createBlock(page, "heading", "Pre-deployment Checklist", pos);
        pos = createBlock(page, "checklist", "All tests passing\nCode review approved\nSecurity scan completed\nPerformance benchmarks met\nDatabase migrations ready", pos);
        pos = createBlock(page, "heading", "Deployment Steps", pos);
        pos = createBlock(page, "numbered_list", "Run database migrations\nDeploy backend services\nUpdate load balancer configuration\nDeploy frontend to CDN\nRun smoke tests\nMonitor error rates", pos);
        pos = createBlock(page, "heading", "Rollback Procedure", pos);
        pos = createBlock(page, "bulleted_list", "If errors detected: Run ./scripts/rollback.sh\nThis will restore previous Docker images\nNotify team via Slack\nDocument incident", pos);
        pos = createBlock(page, "heading", "Environment Variables", pos);
        createBlock(page, "code", "# Production Environment\nDATABASE_URL=postgresql://...\nREDIS_URL=redis://...\nJWT_SECRET=${vault:jwt_secret}\nAI_API_KEY=${vault:gemini_api_key}\nAWS_ACCESS_KEY=${vault:aws_key}", pos);
    }

    private void createTestingStrategyPage() {
        Page page = savePage("Testing Strategy", "🧪", "testing-strategy");
        int pos = 1;
        pos = createBlock(page, "heading", "QA Testing Strategy", pos);
        pos = createBlock(page, "paragraph", "Comprehensive testing approach ensuring high-quality releases.", pos);
        pos = createBlock(page, "heading", "Testing Pyramid", pos);
        pos = createBlock(page, "bulleted_list", "Unit Tests: 70% - Fast, isolated, many\nIntegration Tests: 20% - Service interactions\nE2E Tests: 10% - Critical user flows", pos);
        pos = createBlock(page, "heading", "Test Coverage Requirements", pos);
        pos = createBlock(page, "table", "Module          | Coverage | Critical Paths\nAuthentication  | 90%      | Login, Logout, Password Reset\nTasks          | 85%      | Create, Update, Delete, Assign\nAI Features    | 80%      | Recommend, Analyze, Predict", pos);
        pos = createBlock(page, "heading", "Test Environments", pos);
        pos = createBlock(page, "bulleted_list", "Development: Local testing\nStaging: Pre-production validation\nProduction: Real user testing (canary)", pos);
        pos = createBlock(page, "heading", "AI Model Testing", pos);
        pos = createBlock(page, "bulleted_list", "Accuracy validation on test dataset\nBias detection and mitigation\nPerformance benchmarking\nOutput quality scoring", pos);
        createBlock(page, "quote", "Quality is not an act, it is a habit. - Aristotle", pos);
    }

    private void createSecurityChecklistPage() {
        Page page = savePage("Security Checklist", "🔒", "security-checklist");
        int pos = 1;
        pos = createBlock(page, "heading", "Security Best Practices", pos);
        pos = createBlock(page, "paragraph", "Essential security measures for protecting our systems and user data.", pos);
        pos = createBlock(page, "heading", "Authentication & Authorization", pos);
        pos = createBlock(page, "checklist", "JWT tokens with proper expiration\nRefresh token rotation enabled\nPassword hashing with bcrypt (cost 12)\nRate limiting on login endpoints\nMFA support implemented", pos);
        pos = createBlock(page, "heading", "Data Protection", pos);
        pos = createBlock(page, "checklist", "Encryption at rest for sensitive data\nTLS 1.3 for all connections\nNo sensitive data in logs\nSecure cookie settings (HttpOnly, Secure, SameSite)", pos);
        pos = createBlock(page, "heading", "API Security", pos);
        pos = createBlock(page, "checklist", "Input validation on all endpoints\nSQL injection prevention\nXSS protection enabled\nCORS properly configured\nAPI key rotation policy", pos);
        pos = createBlock(page, "heading", "Infrastructure", pos);
        pos = createBlock(page, "bulleted_list", "Regular security updates\nFirewall rules configured\nIntrusion detection active\nBackup encryption enabled\nAccess logs monitored", pos);
        createBlock(page, "quote", "Security is not a product, but a process. - Bruce Schneier", pos);
    }

    private void createDatabaseDesignPage() {
        Page page = savePage("Database Design", "🗄️", "database-design");
        int pos = 1;
        pos = createBlock(page, "heading", "PostgreSQL Database Schema", pos);
        pos = createBlock(page, "paragraph", "Entity relationship design for the TaskFlow platform.", pos);
        pos = createBlock(page, "heading", "Core Tables", pos);
        pos = createBlock(page, "code", "-- Users and Authentication\nCREATE TABLE users (\n  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),\n  email VARCHAR(255) UNIQUE NOT NULL,\n  password_hash VARCHAR(255) NOT NULL,\n  full_name VARCHAR(100) NOT NULL,\n  avatar_url VARCHAR(500),\n  created_at TIMESTAMP DEFAULT NOW()\n);\n\n-- Workspaces\nCREATE TABLE workspaces (\n  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),\n  name VARCHAR(100) NOT NULL,\n  slug VARCHAR(100) UNIQUE NOT NULL,\n  owner_id UUID REFERENCES users(id),\n  created_at TIMESTAMP DEFAULT NOW()\n);", pos);
        pos = createBlock(page, "heading", "Indexing Strategy", pos);
        pos = createBlock(page, "bulleted_list", "Primary keys on all tables\nForeign key indexes for joins\nComposite indexes for common queries\nPartial indexes for soft deletes\nGIN indexes for JSONB columns", pos);
        pos = createBlock(page, "heading", "Performance Optimizations", pos);
        pos = createBlock(page, "bulleted_list", "Connection pooling with PgBouncer\nRead replicas for reporting queries\nMaterialized views for dashboards\nPartitioning for large tables\nRegular VACUUM and ANALYZE", pos);
        createBlock(page, "code", "-- Example: Task lookup by project and status\nCREATE INDEX idx_tasks_project_status \nON tasks(project_id, status) \nWHERE is_deleted = false;", pos);
    }

    private void createRetrospectivePage() {
        Page page = savePage("Sprint Retrospective", "🔄", "retrospective");
        int pos = 1;
        pos = createBlock(page, "heading", "Sprint 4 Retrospective - What went well", pos);
        pos = createBlock(page, "paragraph", "Date: May 28, 2026 | Team: TaskFlow Development", pos);
        pos = createBlock(page, "heading", "Wins 🎉", pos);
        pos = createBlock(page, "bulleted_list", "AI recommendation engine delivered ahead of schedule\nTeam collaboration improved significantly\nCode review turnaround time reduced by 40%\nAutomated testing coverage increased to 85%", pos);
        pos = createBlock(page, "heading", "Challenges ⚠️", pos);
        pos = createBlock(page, "bulleted_list", "API documentation delays affected integration\nDesign changes mid-sprint caused rework\nSome team members overloaded with concurrent tasks\nThird-party service outages impacted testing", pos);
        pos = createBlock(page, "heading", "Action Items for Sprint 5", pos);
        pos = createBlock(page, "numbered_list", "Finalize API contracts before development starts\nImplement design review gate\nRedistribute workload based on capacity\nAdd circuit breakers for external dependencies", pos);
        pos = createBlock(page, "heading", "Team Feedback", pos);
        createBlock(page, "quote", "Great sprint! The AI features are game-changers. Let's maintain this momentum.", pos);
    }

    private Page savePage(String title, String icon, String slug) {
        return pageRepository.save(Page.builder()
                .workspace(workspace)
                .title(title)
                .icon(icon)
                .slug(slug)
                .isPublic(false)
                .createdBy(users.get(0))
                .updatedBy(users.get(0))
                .build());
    }

    private int createBlock(Page page, String type, String content, int position) {
        blockRepository.save(Block.builder()
                .page(page)
                .type(type)
                .content(content)
                .position(position)
                .createdBy(users.get(0))
                .updatedBy(users.get(0))
                .build());
        return position + 1;
    }

    private void createGoals() {
        LocalDate today = LocalDate.now();
        LocalDate q3End = LocalDate.of(2026, 9, 30);
        LocalDate q4End = LocalDate.of(2026, 12, 31);
        
        // Goal 1: AI Platform Launch
        Goal g1 = saveGoal("Launch TaskFlow AI Platform", "Complete AI-powered platform with all core features", Goal.GoalType.TEAM, Goal.GoalStatus.ACTIVE, today, q3End, 65);
        createKeyResults(g1, new String[][] {
            {"Complete AI recommendation engine", "75"},
            {"Launch AI dashboard", "80"},
            {"Implement workspace chatbot", "40"},
            {"Deploy to production", "0"},
            {"Achieve 100 active users", "0"}
        }, today);
        
        // Goal 2: Mobile App Launch
        Goal g2 = saveGoal("Mobile Banking App Launch", "Launch secure mobile banking app on iOS and Android", Goal.GoalType.TEAM, Goal.GoalStatus.ACTIVE, today, q3End, 45);
        createKeyResults(g2, new String[][] {
            {"Complete core banking features", "60"},
            {"Security audit passed", "100"},
            {"App store submission", "0"},
            {"Beta testing completed", "50"},
            {"Production deployment", "0"}
        }, today);
        
        // Goal 3: Team Excellence (behind schedule)
        Goal g3 = saveGoal("Engineering Excellence", "Build high-performing engineering team", Goal.GoalType.TEAM, Goal.GoalStatus.ACTIVE, today, q3End, 25);
        createKeyResults(g3, new String[][] {
            {"Complete technical documentation", "30"},
            {"Conduct 12 training sessions", "15"},
            {"Improve code review process", "40"},
            {"Implement CI/CD best practices", "20"},
            {"Team NPS score 50+", "0"}
        }, today);
        
        // Goal 4: Product Quality
        Goal g4 = saveGoal("Quality Assurance Improvement", "Achieve 99.9% uptime and zero critical bugs", Goal.GoalType.TEAM, Goal.GoalStatus.ACTIVE, today, q3End, 70);
        createKeyResults(g4, new String[][] {
            {"Achieve 99.9% uptime", "85"},
            {"Reduce critical bugs by 80%", "60"},
            {"Automated test coverage 90%", "70"},
            {"Security vulnerabilities fixed", "100"},
            {"Performance SLAs met", "50"}
        }, today);
        
        // Goal 5: Customer Success (completed)
        Goal g5 = saveGoal("Customer Success Q2", "Onboard 10 new enterprise customers", Goal.GoalType.TEAM, Goal.GoalStatus.ACHIEVED, today.minusMonths(2), today.minusDays(15), 100);
        createKeyResults(g5, new String[][] {
            {"Enterprise onboarding", "100"},
            {"Training sessions completed", "100"},
            {"Customer satisfaction 4.5+", "100"},
            {"Support SLA compliance", "100"},
            {"References obtained", "100"}
        }, today);
        
        log.info("Created {} goals with key results", goalRepository.count());
    }

    private Goal saveGoal(String title, String description, Goal.GoalType type, Goal.GoalStatus status, LocalDate start, LocalDate due, int progress) {
        return goalRepository.save(Goal.builder()
                .workspace(workspace)
                .owner(users.get(0))
                .title(title)
                .description(description)
                .type(type)
                .status(status)
                .period(Goal.GoalPeriod.QUARTERLY)
                .startDate(start)
                .dueDate(due)
                .progressPercentage(progress)
                .currentValue(progress)
                .build());
    }

    private void createKeyResults(Goal goal, String[][] krs, LocalDate baseDate) {
        Random random = new Random(goal.getTitle().hashCode());
        for (String[] data : krs) {
            KeyResult kr = KeyResult.builder()
                    .goal(goal)
                    .assignee(users.get(random.nextInt(users.size())))
                    .title(data[0])
                    .metricType(KeyResult.MetricType.PERCENTAGE)
                    .startValue(0.0)
                    .targetValue(100.0)
                    .currentValue(Double.parseDouble(data[1]))
                    .dueDate(baseDate.plusDays(30 + random.nextInt(60)))
                    .build();
            kr.calculateProgress();
            keyResultRepository.save(kr);
        }
    }

    private void createNotifications() {
        Random random = new Random(123);
        String[] types = {"TASK_ASSIGNED", "TASK_COMPLETED", "COMMENT_ADDED", "MENTION", "DEADLINE_REMINDER", "PROJECT_UPDATE", "GOAL_PROGRESS"};
        String[] messages = {
                "You have been assigned to: %s",
                "Task completed: %s",
                "New comment on: %s",
                "You were mentioned in: %s",
                "Task due tomorrow: %s",
                "Project %s has been updated",
                "Goal progress updated: %s"
        };
        
        for (int i = 0; i < 25; i++) {
            User user = users.get(random.nextInt(users.size()));
            int typeIndex = random.nextInt(types.length);
            String content = getRandomTaskName(random);
            
            notificationRepository.save(Notification.builder()
                    .user(user)
                    .type(types[typeIndex])
                    .message(String.format(messages[typeIndex], content))
                    .isRead(random.nextFloat() < 0.4f)
                    .build());
        }
        log.info("Created {} notifications", notificationRepository.count());
    }

    private String getRandomTaskName(Random random) {
        String[] tasks = {
                "Implement AI recommendation engine",
                "Build authentication system",
                "Design dashboard UI",
                "Create API endpoints",
                "Write unit tests",
                "Optimize database queries",
                "Fix login bug",
                "Deploy to staging",
                "Code review for PR #234",
                "Update documentation"
        };
        return tasks[random.nextInt(tasks.length)];
    }

    private void createActivityLogs() {
        Random random = new Random(456);
        String[] actions = {"CREATED", "UPDATED", "COMPLETED", "ASSIGNED", "COMMENTED", "ARCHIVED", "DELETED"};
        String[] entityTypes = {"TASK", "PROJECT", "PAGE", "GOAL", "COMMENT"};
        
        for (int i = 0; i < 120; i++) {
            User user = users.get(random.nextInt(users.size()));
            String action = actions[random.nextInt(actions.length)];
            String entityType = entityTypes[random.nextInt(entityTypes.length)];
            
            activityLogRepository.save(ActivityLog.builder()
                    .workspace(workspace)
                    .user(user)
                    .action(action)
                    .entityType(entityType)
                    .entityId(UUID.randomUUID())
                    .metadata(String.format("{\"summary\": \"%s %s by %s at %s\"}", 
                            action, entityType, user.getFullName(), LocalDateTime.now().minusHours(random.nextInt(168))))
                    .build());
        }
        log.info("Created {} activity logs", activityLogRepository.count());
    }

    private void createSearchHistory() {
        Random random = new Random(789);
        String[] queries = {
                "AI recommendation",
                "TaskFlow API",
                "Backend developers",
                "Project status",
                "Overdue tasks",
                "Team workload",
                "Sprint planning",
                "Meeting notes",
                "Security checklist",
                "Deployment guide",
                "Mobile app",
                "Banking integration",
                "User authentication",
                "Database schema",
                "Testing strategy",
                "Code review",
                "Performance optimization",
                "Dashboard analytics",
                "Risk analysis",
                "Goal tracking",
                "Task assignment",
                "Workspace settings",
                "Documentation",
                "Architecture design",
                "API endpoints"
        };
        
        for (int i = 0; i < 25; i++) {
            User user = users.get(random.nextInt(users.size()));
            String query = queries[random.nextInt(queries.length)];
            
            searchHistoryRepository.save(SearchHistory.builder()
                    .user(user)
                    .query(query)
                    .entityType(random.nextBoolean() ? "TASK" : "PAGE")
                    .resultCount(random.nextInt(20) + 1)
                    .build());
        }
        log.info("Created {} search history records", searchHistoryRepository.count());
    }

    private void createTaskComments() {
        Random random = new Random(321);
        String[] comments = {
                "I'm working on this now. Should be done by Friday.",
                "Can someone review this? The implementation looks good.",
                "Need clarification on the requirements before proceeding.",
                "Blocked by dependency on TaskFlow API changes.",
                "Test cases passing! Ready for review.",
                "Added more details to the description.",
                "Updated the due date based on new estimates.",
                "This is related to the security audit findings.",
                "Great progress! Keep it up.",
                "I've completed my part. Passing to QA.",
                "Found an issue. Creating a follow-up task.",
                "Waiting for design assets from Trang.",
                "Performance looks good based on benchmarks.",
                "Updated the checklist with new items.",
                "This task is dependent on #42."
        };
        
        List<Task> tasksWithComments = allTasks.subList(0, Math.min(30, allTasks.size()));
        for (Task task : tasksWithComments) {
            int numComments = random.nextInt(3) + 1;
            for (int j = 0; j < numComments; j++) {
                User commenter = users.get(random.nextInt(users.size()));
                String comment = comments[random.nextInt(comments.length)];
                
                taskCommentRepository.save(TaskComment.builder()
                        .task(task)
                        .user(commenter)
                        .content(comment)
                        .build());
            }
        }
        log.info("Created {} task comments", taskCommentRepository.count());
    }

    private void printVerificationReport() {
        log.info("");
        log.info("╔════════════════════════════════════════════════════════╗");
        log.info("║           TECHNova SOLUTIONS DEMO DATA REPORT          ║");
        log.info("╠════════════════════════════════════════════════════════╣");
        log.info("║ ENTITY                    COUNT                       ║");
        log.info("╠════════════════════════════════════════════════════════╣");
        log.info(String.format("║ Users                     %-5d                       ║", userRepository.count()));
        log.info(String.format("║ Workspace                 %-5d                       ║", 1));
        log.info(String.format("║ Projects                  %-5d                       ║", projectRepository.count()));
        log.info(String.format("║ Tasks                     %-5d                       ║", taskRepository.count()));
        log.info(String.format("║   - TODO                  %-5d                       ║", taskRepository.countByStatus(TaskStatus.TODO)));
        log.info(String.format("║   - IN_PROGRESS           %-5d                       ║", taskRepository.countByStatus(TaskStatus.IN_PROGRESS)));
        log.info(String.format("║   - REVIEW                 %-5d                       ║", taskRepository.countByStatus(TaskStatus.REVIEW)));
        log.info(String.format("║   - DONE                   %-5d                       ║", taskRepository.countByStatus(TaskStatus.DONE)));
        log.info(String.format("║   - OVERDUE                %-5d                       ║", taskRepository.countByOverdue(LocalDate.now(), TaskStatus.DONE)));
        log.info(String.format("║ Pages                     %-5d                       ║", pageRepository.count()));
        log.info(String.format("║ Blocks                    %-5d                       ║", blockRepository.count()));
        log.info(String.format("║ Goals                     %-5d                       ║", goalRepository.count()));
        log.info(String.format("║ Key Results               %-5d                       ║", keyResultRepository.count()));
        log.info(String.format("║ Notifications             %-5d                       ║", notificationRepository.count()));
        log.info(String.format("║ Activity Logs             %-5d                       ║", activityLogRepository.count()));
        log.info(String.format("║ Search History            %-5d                       ║", searchHistoryRepository.count()));
        log.info(String.format("║ Task Comments             %-5d                       ║", taskCommentRepository.count()));
        log.info("╠════════════════════════════════════════════════════════╣");
        log.info("║ WORKLOAD DISTRIBUTION                              ║");
        log.info("╠════════════════════════════════════════════════════════╣");
        for (MemberWorkload mw : memberWorkloadRepository.findAll()) {
            String name = mw.getMember().getUser().getFullName();
            log.info(String.format("║ %-20s %3d%% %-10s               ║", 
                    name.length() > 18 ? name.substring(0, 18) : name,
                    mw.getWorkloadPercentage(), mw.getStatus()));
        }
        log.info("╠════════════════════════════════════════════════════════╣");
        log.info("║ AI DEMO READINESS                                  ║");
        log.info("╠════════════════════════════════════════════════════════╣");
        log.info("║ ✓ Overloaded members: 4 (Minh, Long, Khanh, Tu)     ║");
        log.info("║ ✓ Overdue tasks: 12+                               ║");
        log.info("║ ✓ Meeting notes with action items                   ║");
        log.info("║ ✓ Product requirements document                     ║");
        log.info("║ ✓ Goals at various progress stages                  ║");
        log.info("║ ✓ Active projects with diverse tasks                ║");
        log.info("╚════════════════════════════════════════════════════════╝");
    }
}
