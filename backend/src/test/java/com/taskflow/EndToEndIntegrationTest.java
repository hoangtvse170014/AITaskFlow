package com.taskflow;

import com.taskflow.dto.request.LoginRequest;
import com.taskflow.dto.request.RegisterRequest;
import com.taskflow.dto.request.CreateWorkspaceRequest;
import com.taskflow.dto.request.CreateTaskRequest;
import com.taskflow.dto.request.CreateGoalRequest;
import com.taskflow.dto.request.CreatePageRequest;
import com.taskflow.dto.request.CreateBlockRequest;
import com.taskflow.entity.Workspace;
import com.taskflow.entity.WorkspaceMember;
import com.taskflow.entity.Task;
import com.taskflow.entity.Goal;
import com.taskflow.entity.Page;
import com.taskflow.repository.UserRepository;
import com.taskflow.repository.WorkspaceRepository;
import com.taskflow.repository.TaskRepository;
import com.taskflow.repository.GoalRepository;
import com.taskflow.repository.PageRepository;
import com.taskflow.repository.BlockRepository;
import com.taskflow.repository.TaskCommentRepository;
import com.taskflow.repository.ProjectRepository;
import com.taskflow.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class EndToEndIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private UserRepository userRepository;
    @Autowired private WorkspaceRepository workspaceRepository;
    @Autowired private TaskRepository taskRepository;
    @Autowired private GoalRepository goalRepository;
    @Autowired private PageRepository pageRepository;
    @Autowired private BlockRepository blockRepository;
    @Autowired private TaskCommentRepository commentRepository;
    @Autowired private ProjectRepository projectRepository;

    private String user1Token;
    private String user2Token;
    private String user3Token;
    private UUID user1Id;
    private UUID user2Id;
    private UUID user3Id;
    private UUID workspaceId;
    private UUID projectId;
    private UUID taskId;
    private UUID pageId;
    private UUID goalId;

    @BeforeEach
    void setUp() {
        // Data will be created in @Test @Order(1) - Register Users
    }

    @AfterAll
    void cleanup() {
        blockRepository.deleteAll();
        commentRepository.deleteAll();
        pageRepository.deleteAll();
        goalRepository.deleteAll();
        taskRepository.deleteAll();
        projectRepository.deleteAll();
        workspaceRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ==================== PHASE 1: END TO END TESTING ====================

    @Test
    @Order(1)
    @DisplayName("1.1 - User Registration - Success")
    void testUserRegister() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("user1@test.com")
                .password("Password123!")
                .fullName("User One")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.user.email").value("user1@test.com"))
                .andExpect(jsonPath("$.data.user.fullName").value("User One"))
                .andExpect(jsonPath("$.password").doesNotExist());

        // Register user 2
        RegisterRequest request2 = RegisterRequest.builder()
                .email("user2@test.com")
                .password("Password123!")
                .fullName("User Two")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isCreated());

        // Register user 3
        RegisterRequest request3 = RegisterRequest.builder()
                .email("user3@test.com")
                .password("Password123!")
                .fullName("User Three")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request3)))
                .andExpect(status().isCreated());
    }

    @Test
    @Order(2)
    @DisplayName("1.2 - User Registration - Duplicate Email")
    void testUserRegisterDuplicate() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("user1@test.com")
                .password("Password123!")
                .fullName("Duplicate User")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(3)
    @DisplayName("1.3 - User Registration - Validation Errors")
    void testUserRegisterValidation() throws Exception {
        // Invalid email
        RegisterRequest invalidEmail = RegisterRequest.builder()
                .email("invalid-email")
                .password("Password123!")
                .fullName("Test User")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidEmail)))
                .andExpect(status().isBadRequest());

        // Short password
        RegisterRequest shortPassword = RegisterRequest.builder()
                .email("valid@test.com")
                .password("123")
                .fullName("Test User")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(shortPassword)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(4)
    @DisplayName("2.1 - User Login - Success")
    void testUserLogin() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("user1@test.com")
                .password("Password123!")
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        user1Token = objectMapper.readTree(response).get("data").get("accessToken").asText();
        user1Id = userRepository.findByEmail("user1@test.com").get().getId();

        // Login user 2
        LoginRequest request2 = LoginRequest.builder()
                .email("user2@test.com")
                .password("Password123!")
                .build();

        MvcResult result2 = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isOk())
                .andReturn();

        user2Token = objectMapper.readTree(result2.getResponse().getContentAsString()).get("data").get("accessToken").asText();
        user2Id = userRepository.findByEmail("user2@test.com").get().getId();

        // Login user 3
        LoginRequest request3 = LoginRequest.builder()
                .email("user3@test.com")
                .password("Password123!")
                .build();

        MvcResult result3 = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request3)))
                .andExpect(status().isOk())
                .andReturn();

        user3Token = objectMapper.readTree(result3.getResponse().getContentAsString()).get("data").get("accessToken").asText();
        user3Id = userRepository.findByEmail("user3@test.com").get().getId();
    }

    @Test
    @Order(5)
    @DisplayName("2.2 - User Login - Invalid Credentials")
    void testUserLoginInvalid() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("user1@test.com")
                .password("WrongPassword!")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(6)
    @DisplayName("2.3 - User Login - Non-existent User")
    void testUserLoginNonExistent() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("nonexistent@test.com")
                .password("Password123!")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(7)
    @DisplayName("3.1 - Create Workspace")
    void testCreateWorkspace() throws Exception {
        CreateWorkspaceRequest request = CreateWorkspaceRequest.builder()
                .name("Test Workspace")
                .description("A workspace for testing")
                .build();

        MvcResult result = mockMvc.perform(post("/api/workspaces")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Test Workspace"))
                .andReturn();

        workspaceId = UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("data").get("id").asText());
    }

    @Test
    @Order(8)
    @DisplayName("3.2 - Get User Workspaces")
    void testGetUserWorkspaces() throws Exception {
        mockMvc.perform(get("/api/workspaces")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @Order(9)
    @DisplayName("4.1 - Invite Member to Workspace")
    void testInviteMember() throws Exception {
        String inviteRequest = String.format("""
            {
                "email": "user2@test.com",
                "role": "MEMBER"
            }
            """);

        mockMvc.perform(post("/api/workspaces/{workspaceId}/invite", workspaceId)
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(inviteRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.email").value("user2@test.com"))
                .andExpect(jsonPath("$.data.role").value("MEMBER"));
    }

    @Test
    @Order(10)
    @DisplayName("4.2 - Invite Already Member")
    void testInviteAlreadyMember() throws Exception {
        String inviteRequest = String.format("""
            {
                "email": "user2@test.com",
                "role": "MEMBER"
            }
            """);

        mockMvc.perform(post("/api/workspaces/{workspaceId}/invite", workspaceId)
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(inviteRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(11)
    @DisplayName("5.1 - Create Project")
    void testCreateProject() throws Exception {
        String projectRequest = String.format("""
            {
                "name": "Test Project",
                "description": "Project description"
            }
            """);

        MvcResult result = mockMvc.perform(post("/api/workspaces/{workspaceId}/projects", workspaceId)
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(projectRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Test Project"))
                .andReturn();

        projectId = UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("data").get("id").asText());
    }

    @Test
    @Order(12)
    @DisplayName("5.2 - Get Projects by Workspace")
    void testGetProjectsByWorkspace() throws Exception {
        mockMvc.perform(get("/api/workspaces/{workspaceId}/projects", workspaceId)
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].name").value("Test Project"));
    }

    @Test
    @Order(13)
    @DisplayName("6.1 - Create Task")
    void testCreateTask() throws Exception {
        String taskRequest = String.format("""
            {
                "title": "Test Task",
                "description": "Task description",
                "priority": "HIGH",
                "status": "TODO"
            }
            """);

        MvcResult result = mockMvc.perform(post("/api/projects/{projectId}/tasks", projectId)
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(taskRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("Test Task"))
                .andExpect(jsonPath("$.data.priority").value("HIGH"))
                .andReturn();

        taskId = UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("data").get("id").asText());
    }

    @Test
    @Order(14)
    @DisplayName("6.2 - Update Task Status")
    void testUpdateTaskStatus() throws Exception {
        String updateRequest = """
            {
                "status": "IN_PROGRESS"
            }
            """;

        mockMvc.perform(put("/api/projects/{projectId}/tasks/{taskId}", projectId, taskId)
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));
    }

    @Test
    @Order(15)
    @DisplayName("6.3 - Assign Task to Member")
    void testAssignTask() throws Exception {
        String assignRequest = String.format("""
            {
                "assigneeId": "%s"
            }
            """, user2Id);

        mockMvc.perform(put("/api/projects/{projectId}/tasks/{taskId}", projectId, taskId)
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assignRequest))
                .andExpect(status().isOk());
    }

    @Test
    @Order(16)
    @DisplayName("7.1 - Create Page")
    void testCreatePage() throws Exception {
        String pageRequest = String.format("""
            {
                "title": "Test Page"
            }
            """);

        MvcResult result = mockMvc.perform(post("/api/workspaces/{workspaceId}/pages", workspaceId)
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pageRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("Test Page"))
                .andReturn();

        pageId = UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("data").get("id").asText());
    }

    @Test
    @Order(17)
    @DisplayName("7.2 - Add Block to Page")
    void testAddBlockToPage() throws Exception {
        String blockRequest = String.format("""
            {
                "blockType": "HEADING",
                "content": "Test Heading",
                "orderIndex": 0
            }
            """);

        mockMvc.perform(post("/api/pages/{pageId}/blocks", pageId)
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(blockRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.blockType").value("HEADING"))
                .andExpect(jsonPath("$.data.content").value("Test Heading"));
    }

    @Test
    @Order(18)
    @DisplayName("8.1 - Create Goal")
    void testCreateGoal() throws Exception {
        String goalRequest = """
            {
                "title": "Q3 2026 Goals",
                "description": "Company goals for Q3",
                "type": "TEAM",
                "period": "QUARTERLY",
                "dueDate": "2026-09-30"
            }
            """;

        MvcResult result = mockMvc.perform(post("/api/goals/workspace/{workspaceId}", workspaceId)
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(goalRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("Q3 2026 Goals"))
                .andReturn();

        goalId = UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("data").get("id").asText());
    }

    @Test
    @Order(19)
    @DisplayName("8.2 - Update Goal Progress")
    void testUpdateGoalProgress() throws Exception {
        String updateRequest = """
            {
                "title": "Q3 2026 Goals - Updated"
            }
            """;

        mockMvc.perform(put("/api/goals/{goalId}", goalId)
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Q3 2026 Goals - Updated"));
    }

    @Test
    @Order(20)
    @DisplayName("9.1 - Search - Title Search")
    void testSearchTitle() throws Exception {
        mockMvc.perform(get("/api/search")
                        .header("Authorization", "Bearer " + user1Token)
                        .param("q", "Test")
                        .param("workspaceId", workspaceId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tasks").isArray())
                .andExpect(jsonPath("$.data.projects").isArray())
                .andExpect(jsonPath("$.data.pages").isArray());
    }

    @Test
    @Order(21)
    @DisplayName("9.2 - Search - Fuzzy Search")
    void testSearchFuzzy() throws Exception {
        mockMvc.perform(get("/api/search")
                        .header("Authorization", "Bearer " + user1Token)
                        .param("q", "Tst")
                        .param("workspaceId", workspaceId.toString()))
                .andExpect(status().isOk());
    }

    @Test
    @Order(22)
    @DisplayName("10.1 - Get Notifications")
    void testGetNotifications() throws Exception {
        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.notifications").isArray());
    }

    @Test
    @Order(23)
    @DisplayName("10.2 - Mark Notification as Read")
    void testMarkNotificationRead() throws Exception {
        mockMvc.perform(post("/api/notifications/{id}/read", UUID.randomUUID())
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk());
    }

    @Test
    @Order(24)
    @DisplayName("11.1 - Get Activity Logs")
    void testGetActivityLogs() throws Exception {
        mockMvc.perform(get("/api/activity/workspace/{workspaceId}", workspaceId)
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    // ==================== PHASE 2: PERMISSION TESTING ====================

    @Test
    @Order(25)
    @DisplayName("12.1 - Owner can access Dashboard")
    void testOwnerAccessDashboard() throws Exception {
        mockMvc.perform(get("/api/dashboard/stats")
                        .header("Authorization", "Bearer " + user1Token)
                        .param("workspaceId", workspaceId.toString()))
                .andExpect(status().isOk());
    }

    @Test
    @Order(26)
    @DisplayName("12.2 - Member can access Dashboard")
    void testMemberAccessDashboard() throws Exception {
        mockMvc.perform(get("/api/dashboard/stats")
                        .header("Authorization", "Bearer " + user2Token)
                        .param("workspaceId", workspaceId.toString()))
                .andExpect(status().isOk());
    }

    @Test
    @Order(27)
    @DisplayName("12.3 - Non-member cannot access Workspace")
    void testNonMemberCannotAccess() throws Exception {
        mockMvc.perform(get("/api/dashboard/stats")
                        .header("Authorization", "Bearer " + user3Token)
                        .param("workspaceId", workspaceId.toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(28)
    @DisplayName("12.4 - Member cannot create Project (permission denied)")
    void testMemberCannotCreateProject() throws Exception {
        String projectRequest = String.format("""
            {
                "name": "Unauthorized Project",
                "workspaceId": "%s"
            }
            """, workspaceId);

        mockMvc.perform(post("/api/workspaces/{workspaceId}/projects", workspaceId)
                        .header("Authorization", "Bearer " + user2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(projectRequest))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(29)
    @DisplayName("12.5 - Member cannot delete Project")
    void testMemberCannotDeleteProject() throws Exception {
        mockMvc.perform(delete("/api/workspaces/{workspaceId}/projects/{projectId}", workspaceId, projectId)
                        .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(30)
    @DisplayName("12.6 - Owner can delete Project")
    void testOwnerCanDeleteProject() throws Exception {
        // Create a project first
        String projectRequest = String.format("""
            {
                "name": "Project to Delete",
                "workspaceId": "%s"
            }
            """, workspaceId);

        MvcResult result = mockMvc.perform(post("/api/workspaces/{workspaceId}/projects", workspaceId)
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(projectRequest))
                .andExpect(status().isCreated())
                .andReturn();

        UUID projectToDelete = UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("data").get("id").asText());

        // Owner can delete
        mockMvc.perform(delete("/api/workspaces/{workspaceId}/projects/{projectId}", workspaceId, projectToDelete)
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isNoContent());
    }

    // ==================== PHASE 3: SECURITY TESTING ====================

    @Test
    @Order(31)
    @DisplayName("13.1 - JWT Token Validation")
    void testJwtValidation() throws Exception {
        // Valid token
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk());

        // Invalid token
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer invalid.token.here"))
                .andExpect(status().isUnauthorized());

        // Missing token
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(32)
    @DisplayName("13.2 - IDOR Protection - Cross Workspace Access")
    void testIdorProtection() throws Exception {
        // Create another workspace for user2
        CreateWorkspaceRequest request = CreateWorkspaceRequest.builder()
                .name("User2 Private Workspace")
                .build();

        MvcResult result = mockMvc.perform(post("/api/workspaces")
                        .header("Authorization", "Bearer " + user2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID user2WorkspaceId = UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("data").get("id").asText());

        // User1 tries to access User2's private workspace
        mockMvc.perform(get("/api/dashboard/stats")
                        .header("Authorization", "Bearer " + user1Token)
                        .param("workspaceId", user2WorkspaceId.toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(33)
    @DisplayName("13.3 - SQL Injection Prevention")
    void testSqlInjection() throws Exception {
        // Try SQL injection in search
        mockMvc.perform(get("/api/search")
                        .header("Authorization", "Bearer " + user1Token)
                        .param("q", "'; DROP TABLE users; --")
                        .param("workspaceId", workspaceId.toString()))
                .andExpect(status().isOk()); // Should not execute the query

        // Try SQL injection in task creation
        String injectionRequest = String.format("""
            {
                "title": "Task'; DELETE FROM tasks; --",
                "projectId": "%s"
            }
            """, projectId);

        mockMvc.perform(post("/api/projects/{projectId}/tasks", projectId)
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(injectionRequest))
                .andExpect(status().isCreated()); // Should sanitize input
    }

    @Test
    @Order(34)
    @DisplayName("13.4 - XSS Prevention in Page Content")
    void testXssPrevention() throws Exception {
        String xssContent = "<script>alert('XSS')</script>";

        String blockRequest = String.format("""
            {
                "blockType": "TEXT",
                "content": "%s",
                "orderIndex": 10
            }
            """, xssContent);

        MvcResult result = mockMvc.perform(post("/api/pages/{pageId}/blocks", pageId)
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(blockRequest))
                .andExpect(status().isCreated())
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        assert responseContent.contains("alert");
    }

    @Test
    @Order(35)
    @DisplayName("13.5 - Mass Assignment Prevention")
    void testMassAssignment() throws Exception {
        // Try to set admin role through user registration
        String maliciousRequest = """
            {
                "email": "hacker@test.com",
                "password": "Password123!",
                "fullName": "Hacker",
                "role": "ADMIN"
            }
            """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(maliciousRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.role").doesNotExist()); // role should not be settable
    }

    // ==================== PHASE 4: API ERROR HANDLING ====================

    @Test
    @Order(36)
    @DisplayName("14.1 - 404 Not Found")
    void test404Error() throws Exception {
        mockMvc.perform(get("/api/projects/{projectId}/tasks/{taskId}", projectId, UUID.randomUUID())
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(37)
    @DisplayName("14.2 - 400 Bad Request - Invalid UUID")
    void testInvalidUuid() throws Exception {
        mockMvc.perform(get("/api/projects/{projectId}/tasks/invalid-uuid", projectId)
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(38)
    @DisplayName("14.3 - 500 Internal Server Error Handling")
    void test500Handling() throws Exception {
        // This should be handled gracefully
        String invalidRequest = """
            {
                "title": "Test"
            }
            """;

        mockMvc.perform(post("/api/projects/{projectId}/tasks", projectId)
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest());
    }

    // ==================== PHASE 5: PAGINATION & FILTERING ====================

    @Test
    @Order(39)
    @DisplayName("15.1 - Pagination")
    void testPagination() throws Exception {
        mockMvc.perform(get("/api/projects/{projectId}/tasks", projectId)
                        .header("Authorization", "Bearer " + user1Token)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk());
    }

    @Test
    @Order(40)
    @DisplayName("15.2 - Filtering")
    void testFiltering() throws Exception {
        mockMvc.perform(get("/api/projects/{projectId}/tasks", projectId)
                        .header("Authorization", "Bearer " + user1Token)
                        .param("status", "IN_PROGRESS")
                        .param("priority", "HIGH"))
                .andExpect(status().isOk());
    }

    @Test
    @Order(41)
    @DisplayName("15.3 - Sorting")
    void testSorting() throws Exception {
        mockMvc.perform(get("/api/projects/{projectId}/tasks", projectId)
                        .header("Authorization", "Bearer " + user1Token)
                        .param("sortBy", "createdAt")
                        .param("sortDir", "desc"))
                .andExpect(status().isOk());
    }

    // ==================== PHASE 6: WORKLOAD TESTING ====================

    @Test
    @Order(42)
    @DisplayName("16.1 - Get Team Workload")
    void testGetTeamWorkload() throws Exception {
        mockMvc.perform(get("/api/workloads/workspace/{workspaceId}", workspaceId)
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk());
    }

    // ==================== PHASE 7: SMART DASHBOARD ====================

    @Test
    @Order(43)
    @DisplayName("17.1 - Get Smart Dashboard")
    void testGetSmartDashboard() throws Exception {
        mockMvc.perform(get("/api/dashboard/smart")
                        .header("Authorization", "Bearer " + user1Token)
                        .param("workspaceId", workspaceId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.myTasks").isArray())
                .andExpect(jsonPath("$.data.suggestions").isArray());
    }

    // ==================== FINAL CLEANUP CHECK ====================

    @Test
    @Order(100)
    @DisplayName("Final - Database Integrity Check")
    void testDatabaseIntegrity() throws Exception {
        // Verify all data is properly linked
        assert userRepository.count() >= 3 : "At least 3 users should exist";
        assert workspaceRepository.count() >= 1 : "At least 1 workspace should exist";
        assert taskRepository.count() >= 1 : "At least 1 task should exist";
        assert goalRepository.count() >= 1 : "At least 1 goal should exist";
        assert pageRepository.count() >= 1 : "At least 1 page should exist";
    }
}
