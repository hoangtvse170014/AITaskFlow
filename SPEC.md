# TaskFlow - Modern Task Management Application

## 1. Project Overview

### 1.1 Project Name
**TaskFlow** - A modern, lightweight task management application

### 1.2 Project Type
Full-stack web application (SaaS-style)

### 1.3 Core Functionality
A Jira/Linear-inspired task management system with Kanban boards, workspace collaboration, and real-time task tracking.

### 1.4 Target Users
- Small to medium teams
- Freelancers and solo users
- Project managers
- Development teams

### 1.5 Technology Stack

#### Frontend
| Technology | Version | Purpose |
|------------|---------|---------|
| Next.js | 15.x | React framework |
| TypeScript | 5.x | Type safety |
| TailwindCSS | 3.x | Styling |
| shadcn/ui | latest | UI components |
| Zustand | 4.x | State management |
| dnd-kit | 6.x | Drag and drop |
| React Query | 5.x | Data fetching |
| Lucide React | latest | Icons |

#### Backend
| Technology | Version | Purpose |
|------------|---------|---------|
| Spring Boot | 3.2.x | Framework |
| Java | 21 | Language |
| PostgreSQL | 15.x | Database |
| Spring Security | 6.x | Security |
| Spring Data JPA | 3.x | ORM |
| JWT | - | Authentication |
| Lombok | - | Boilerplate reduction |

---

## 2. Architecture Design

### 2.1 System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        CLIENT LAYER                         │
│  Next.js 15 App (Port 3000) - TypeScript + TailwindCSS     │
└─────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────┐
│                       API GATEWAY                            │
│              REST API (Port 8080) - Spring Boot             │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌─────────────┐  │
│  │   Auth   │  │ Workspace│  │  Project │  │    Task     │  │
│  │  Module  │  │  Module  │  │  Module  │  │   Module    │  │
│  └──────────┘  └──────────┘  └──────────┘  └─────────────┘  │
└─────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────┐
│                      DATA LAYER                              │
│              PostgreSQL 15 - Database                        │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 Clean Architecture Layers

#### Backend Structure (Spring Boot)
```
backend/
├── src/main/java/com/taskflow/
│   ├── config/           # Configuration classes
│   │   ├── SecurityConfig.java
│   │   ├── JwtConfig.java
│   │   └── CorsConfig.java
│   ├── controller/       # REST Controllers
│   │   ├── AuthController.java
│   │   ├── WorkspaceController.java
│   │   ├── ProjectController.java
│   │   └── TaskController.java
│   ├── dto/              # Data Transfer Objects
│   │   ├── request/
│   │   └── response/
│   ├── entity/           # JPA Entities
│   │   ├── User.java
│   │   ├── Workspace.java
│   │   ├── WorkspaceMember.java
│   │   ├── Project.java
│   │   ├── Task.java
│   │   ├── Comment.java
│   │   └── ActivityLog.java
│   ├── repository/       # JPA Repositories
│   ├── service/          # Business Logic
│   │   ├── impl/
│   │   └── interfaces/
│   ├── security/         # Security Components
│   │   ├── JwtTokenProvider.java
│   │   ├── JwtAuthenticationFilter.java
│   │   └── UserDetailsServiceImpl.java
│   └── exception/        # Exception Handling
│       ├── GlobalExceptionHandler.java
│       └── CustomExceptions.java
```

#### Frontend Structure (Next.js)
```
frontend/
├── src/
│   ├── app/              # Next.js App Router
│   │   ├── (auth)/       # Auth pages group
│   │   │   ├── login/
│   │   │   └── register/
│   │   ├── (dashboard)/  # Dashboard pages group
│   │   │   ├── layout.tsx
│   │   │   ├── page.tsx  # Dashboard home
│   │   │   ├── projects/
│   │   │   └── settings/
│   │   ├── api/          # API routes (if needed)
│   │   ├── layout.tsx
│   │   └── globals.css
│   ├── components/       # React components
│   │   ├── ui/           # shadcn/ui components
│   │   ├── auth/         # Auth-related components
│   │   ├── layout/       # Layout components
│   │   │   ├── Sidebar.tsx
│   │   │   ├── Header.tsx
│   │   │   └── MainLayout.tsx
│   │   ├── projects/     # Project components
│   │   ├── tasks/        # Task components
│   │   │   ├── KanbanBoard.tsx
│   │   │   ├── KanbanColumn.tsx
│   │   │   ├── TaskCard.tsx
│   │   │   └── TaskModal.tsx
│   │   └── dashboard/    # Dashboard components
│   │       ├── StatsCard.tsx
│   │       └── ProgressChart.tsx
│   ├── hooks/            # Custom React hooks
│   ├── lib/              # Utilities
│   │   ├── api.ts        # API client
│   │   ├── utils.ts      # Utility functions
│   │   └── constants.ts
│   ├── store/            # Zustand stores
│   │   ├── authStore.ts
│   │   ├── workspaceStore.ts
│   │   ├── projectStore.ts
│   │   └── taskStore.ts
│   ├── types/            # TypeScript types
│   └── styles/
├── public/
├── tailwind.config.ts
└── next.config.js
```

---

## 3. Database Schema

### 3.1 Entity Relationship Diagram

```
┌─────────────┐       ┌──────────────────┐       ┌─────────────┐
│    User     │───────│ WorkspaceMember  │───────│  Workspace  │
└─────────────┘  1:N  └──────────────────┘  1:N  └─────────────┘
       │                                           │
       │ 1:N                                        │ 1:N
       ▼                                           ▼
┌─────────────┐                           ┌─────────────────┐
│   Comment   │                           │    Project      │
└─────────────┘                           └─────────────────┘
       │                                          │
       │ 1:N                                     │ 1:N
       ▼                                         ▼
┌─────────────┐       ┌──────────────────┐
│ActivityLog  │───────│      Task        │
└─────────────┘ 1:N   └──────────────────┘
```

### 3.2 Table Definitions

#### users
| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | Unique identifier |
| email | VARCHAR(255) | UNIQUE, NOT NULL | User email |
| password | VARCHAR(255) | NOT NULL | Hashed password |
| full_name | VARCHAR(100) | NOT NULL | Display name |
| avatar_url | VARCHAR(500) | NULLABLE | Profile image URL |
| created_at | TIMESTAMP | NOT NULL | Creation timestamp |
| updated_at | TIMESTAMP | NOT NULL | Last update timestamp |

#### workspaces
| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | Unique identifier |
| name | VARCHAR(100) | NOT NULL | Workspace name |
| slug | VARCHAR(100) | UNIQUE, NOT NULL | URL-friendly name |
| description | TEXT | NULLABLE | Workspace description |
| owner_id | UUID | FK -> users.id | Owner user ID |
| created_at | TIMESTAMP | NOT NULL | Creation timestamp |
| updated_at | TIMESTAMP | NOT NULL | Last update timestamp |

#### workspace_members
| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | Unique identifier |
| workspace_id | UUID | FK -> workspaces.id | Workspace reference |
| user_id | UUID | FK -> users.id | User reference |
| role | VARCHAR(20) | NOT NULL | OWNER, ADMIN, MEMBER |
| invited_at | TIMESTAMP | NOT NULL | Invitation timestamp |
| joined_at | TIMESTAMP | NULLABLE | Join timestamp |

#### projects
| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | Unique identifier |
| workspace_id | UUID | FK -> workspaces.id | Parent workspace |
| name | VARCHAR(100) | NOT NULL | Project name |
| key | VARCHAR(10) | NOT NULL | Project key (e.g., PROJ) |
| description | TEXT | NULLABLE | Project description |
| color | VARCHAR(7) | NOT NULL | Hex color code |
| icon | VARCHAR(50) | NULLABLE | Emoji or icon name |
| created_at | TIMESTAMP | NOT NULL | Creation timestamp |
| updated_at | TIMESTAMP | NOT NULL | Last update timestamp |

#### tasks
| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | Unique identifier |
| project_id | UUID | FK -> projects.id | Parent project |
| task_number | INTEGER | NOT NULL | Auto-increment per project |
| title | VARCHAR(255) | NOT NULL | Task title |
| description | TEXT | NULLABLE | Detailed description |
| status | VARCHAR(20) | NOT NULL | TODO, IN_PROGRESS, REVIEW, DONE |
| priority | VARCHAR(20) | NOT NULL | LOW, MEDIUM, HIGH, URGENT |
| assignee_id | UUID | FK -> users.id, NULLABLE | Assigned user |
| reporter_id | UUID | FK -> users.id | Reporter user |
| due_date | DATE | NULLABLE | Due date |
| labels | JSONB | DEFAULT '[]' | Array of label objects |
| checklist | JSONB | DEFAULT '[]' | Array of checklist items |
| position | INTEGER | NOT NULL | Order position in column |
| created_at | TIMESTAMP | NOT NULL | Creation timestamp |
| updated_at | TIMESTAMP | NOT NULL | Last update timestamp |

#### comments
| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | Unique identifier |
| task_id | UUID | FK -> tasks.id | Parent task |
| author_id | UUID | FK -> users.id | Comment author |
| content | TEXT | NOT NULL | Comment content |
| created_at | TIMESTAMP | NOT NULL | Creation timestamp |
| updated_at | TIMESTAMP | NOT NULL | Last update timestamp |

#### activity_logs
| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | Unique identifier |
| task_id | UUID | FK -> tasks.id | Related task |
| user_id | UUID | FK -> users.id | Acting user |
| action | VARCHAR(50) | NOT NULL | Action type |
| field_changed | VARCHAR(50) | NULLABLE | Changed field name |
| old_value | TEXT | NULLABLE | Previous value |
| new_value | TEXT | NULLABLE | New value |
| created_at | TIMESTAMP | NOT NULL | Timestamp |

---

## 4. API Specification

### 4.1 Authentication APIs

#### POST /api/auth/register
Register a new user account.

**Request:**
```json
{
  "email": "user@example.com",
  "password": "securePassword123",
  "fullName": "John Doe"
}
```

**Response (201):**
```json
{
  "success": true,
  "data": {
    "user": {
      "id": "uuid",
      "email": "user@example.com",
      "fullName": "John Doe",
      "avatarUrl": null
    },
    "accessToken": "jwt_token_here",
    "refreshToken": "refresh_token_here"
  }
}
```

#### POST /api/auth/login
Authenticate user and get tokens.

**Request:**
```json
{
  "email": "user@example.com",
  "password": "securePassword123"
}
```

**Response (200):**
```json
{
  "success": true,
  "data": {
    "user": {
      "id": "uuid",
      "email": "user@example.com",
      "fullName": "John Doe",
      "avatarUrl": null
    },
    "accessToken": "jwt_token_here",
    "refreshToken": "refresh_token_here"
  }
}
```

#### POST /api/auth/refresh
Refresh access token.

**Request:**
```json
{
  "refreshToken": "refresh_token_here"
}
```

#### GET /api/auth/me
Get current authenticated user.

**Response (200):**
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "email": "user@example.com",
    "fullName": "John Doe",
    "avatarUrl": null
  }
}
```

### 4.2 Workspace APIs

#### GET /api/workspaces
Get all workspaces for current user.

**Response (200):**
```json
{
  "success": true,
  "data": [
    {
      "id": "uuid",
      "name": "My Workspace",
      "slug": "my-workspace",
      "description": "Personal workspace",
      "role": "OWNER",
      "memberCount": 1,
      "projectCount": 3
    }
  ]
}
```

#### POST /api/workspaces
Create a new workspace.

**Request:**
```json
{
  "name": "New Workspace",
  "description": "Workspace description"
}
```

#### GET /api/workspaces/{workspaceId}
Get workspace details.

#### PUT /api/workspaces/{workspaceId}
Update workspace.

#### DELETE /api/workspaces/{workspaceId}
Delete workspace.

#### POST /api/workspaces/{workspaceId}/invite
Invite member to workspace.

**Request:**
```json
{
  "email": "member@example.com",
  "role": "MEMBER"
}
```

#### GET /api/workspaces/{workspaceId}/members
Get workspace members.

#### DELETE /api/workspaces/{workspaceId}/members/{memberId}
Remove workspace member.

### 4.3 Project APIs

#### GET /api/workspaces/{workspaceId}/projects
Get all projects in workspace.

**Response (200):**
```json
{
  "success": true,
  "data": [
    {
      "id": "uuid",
      "name": "Website Redesign",
      "key": "WR",
      "description": "Redesign company website",
      "color": "#6366f1",
      "icon": "globe",
      "taskCount": 15,
      "completedTaskCount": 8
    }
  ]
}
```

#### POST /api/workspaces/{workspaceId}/projects
Create a new project.

**Request:**
```json
{
  "name": "Website Redesign",
  "key": "WR",
  "description": "Redesign company website",
  "color": "#6366f1",
  "icon": "globe"
}
```

#### GET /api/workspaces/{workspaceId}/projects/{projectId}
Get project details.

#### PUT /api/workspaces/{workspaceId}/projects/{projectId}
Update project.

#### DELETE /api/workspaces/{workspaceId}/projects/{projectId}
Delete project.

### 4.4 Task APIs

#### GET /api/projects/{projectId}/tasks
Get all tasks in project (with filters).

**Query Parameters:**
- `status`: Filter by status
- `priority`: Filter by priority
- `assigneeId`: Filter by assignee

**Response (200):**
```json
{
  "success": true,
  "data": {
    "tasks": [
      {
        "id": "uuid",
        "taskNumber": 1,
        "title": "Design homepage",
        "description": "Create homepage mockups",
        "status": "TODO",
        "priority": "HIGH",
        "assignee": {
          "id": "uuid",
          "fullName": "John Doe",
          "avatarUrl": null
        },
        "reporter": {
          "id": "uuid",
          "fullName": "Jane Doe"
        },
        "dueDate": "2026-06-15",
        "labels": [],
        "checklist": [],
        "position": 0,
        "createdAt": "2026-05-27T10:00:00Z",
        "updatedAt": "2026-05-27T10:00:00Z"
      }
    ],
    "groupedByStatus": {
      "TODO": [...],
      "IN_PROGRESS": [...],
      "REVIEW": [...],
      "DONE": [...]
    }
  }
}
```

#### POST /api/projects/{projectId}/tasks
Create a new task.

**Request:**
```json
{
  "title": "Design homepage",
  "description": "Create homepage mockups",
  "status": "TODO",
  "priority": "HIGH",
  "assigneeId": "uuid",
  "dueDate": "2026-06-15",
  "labels": [{"name": "Design", "color": "#6366f1"}],
  "checklist": [{"text": "Create wireframes", "completed": false}]
}
```

#### GET /api/projects/{projectId}/tasks/{taskId}
Get task details.

**Response (200):**
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "taskNumber": 1,
    "title": "Design homepage",
    "description": "Full description with markdown support",
    "status": "TODO",
    "priority": "HIGH",
    "assignee": {...},
    "reporter": {...},
    "dueDate": "2026-06-15",
    "labels": [...],
    "checklist": [...],
    "comments": [...],
    "activityLogs": [...],
    "createdAt": "...",
    "updatedAt": "..."
  }
}
```

#### PUT /api/projects/{projectId}/tasks/{taskId}
Update task.

**Request:**
```json
{
  "title": "Updated title",
  "description": "Updated description",
  "status": "IN_PROGRESS",
  "priority": "URGENT",
  "assigneeId": "uuid",
  "dueDate": "2026-06-20",
  "labels": [...],
  "checklist": [...]
}
```

#### PATCH /api/projects/{projectId}/tasks/{taskId}/position
Update task position (for drag/drop).

**Request:**
```json
{
  "status": "IN_PROGRESS",
  "position": 2
}
```

#### DELETE /api/projects/{projectId}/tasks/{taskId}
Delete task.

### 4.5 Comment APIs

#### GET /api/tasks/{taskId}/comments
Get task comments.

#### POST /api/tasks/{taskId}/comments
Add comment to task.

**Request:**
```json
{
  "content": "This looks great! Just a few suggestions..."
}
```

#### PUT /api/tasks/{taskId}/comments/{commentId}
Update comment.

#### DELETE /api/tasks/{taskId}/comments/{commentId}
Delete comment.

### 4.6 Dashboard APIs

#### GET /api/dashboard/stats
Get dashboard statistics for workspace.

**Response (200):**
```json
{
  "success": true,
  "data": {
    "totalTasks": 45,
    "completedTasks": 28,
    "inProgressTasks": 10,
    "todoTasks": 7,
    "delayedTasks": 3,
    "completionRate": 62,
    "tasksByPriority": {
      "URGENT": 5,
      "HIGH": 12,
      "MEDIUM": 20,
      "LOW": 8
    },
    "tasksByStatus": {
      "TODO": 7,
      "IN_PROGRESS": 10,
      "REVIEW": 5,
      "DONE": 28
    },
    "recentTasks": [...],
    "overdueTasks": [...]
  }
}
```

---

## 5. UI/UX Design Specification

### 5.1 Design System

#### Color Palette

**Dark Mode (Primary):**
| Name | Hex | Usage |
|------|-----|-------|
| Background | #09090b | Main background |
| Surface | #18181b | Cards, modals |
| Surface Elevated | #27272a | Hover states |
| Border | #3f3f46 | Borders |
| Border Hover | #52525b | Border on hover |
| Text Primary | #fafafa | Main text |
| Text Secondary | #a1a1aa | Secondary text |
| Text Muted | #71717a | Muted text |

**Accent Colors:**
| Name | Hex | Usage |
|------|-----|-------|
| Primary | #6366f1 | Indigo - Primary actions |
| Primary Hover | #4f46e5 | Hover state |
| Success | #22c55e | Success states |
| Warning | #f59e0b | Warning states |
| Error | #ef4444 | Error states |

**Priority Colors:**
| Priority | Color |
|----------|-------|
| Urgent | #ef4444 |
| High | #f97316 |
| Medium | #eab308 |
| Low | #22c55e |

**Status Colors:**
| Status | Color |
|---------|-------|
| Todo | #71717a |
| In Progress | #3b82f6 |
| Review | #a855f7 |
| Done | #22c55e |

#### Typography

| Element | Font | Size | Weight |
|---------|------|------|--------|
| H1 | Inter | 30px | 700 |
| H2 | Inter | 24px | 600 |
| H3 | Inter | 20px | 600 |
| H4 | Inter | 16px | 600 |
| Body | Inter | 14px | 400 |
| Small | Inter | 12px | 400 |
| Caption | Inter | 11px | 500 |

#### Spacing System
- Base unit: 4px
- XS: 4px
- SM: 8px
- MD: 16px
- LG: 24px
- XL: 32px
- 2XL: 48px

#### Border Radius
- SM: 6px
- MD: 8px
- LG: 12px
- XL: 16px
- Full: 9999px

#### Shadows
- SM: 0 1px 2px rgba(0, 0, 0, 0.3)
- MD: 0 4px 6px rgba(0, 0, 0, 0.3)
- LG: 0 10px 15px rgba(0, 0, 0, 0.3)
- XL: 0 20px 25px rgba(0, 0, 0, 0.4)

### 5.2 Layout Specifications

#### Sidebar (Collapsed)
- Width: 64px
- Background: Surface (#18181b)
- Icon size: 20px
- Item padding: 12px

#### Sidebar (Expanded)
- Width: 240px
- Same background
- Item padding: 12px 16px

#### Main Content Area
- Max width: 1400px
- Padding: 24px
- Gap between sections: 24px

#### Task Cards
- Padding: 16px
- Border radius: 12px
- Background: Surface (#18181b)
- Border: 1px solid Border (#3f3f46)
- Hover: Border changes to Border Hover (#52525b)

#### Kanban Columns
- Width: 320px
- Min width: 280px
- Gap: 16px
- Column header padding: 12px 16px

### 5.3 Component Specifications

#### Button Variants
1. **Primary**: Background Primary, white text
2. **Secondary**: Background Surface, text primary, border
3. **Ghost**: Transparent, text primary, hover background
4. **Destructive**: Background Error, white text

#### Input Fields
- Height: 40px
- Padding: 0 12px
- Border radius: 8px
- Background: Surface
- Border: 1px solid Border
- Focus: Border Primary

#### Modal/Dialog
- Max width: 640px (Task modal: 800px)
- Border radius: 16px
- Background: Surface
- Overlay: rgba(0, 0, 0, 0.7)

---

## 6. Feature Specifications

### 6.1 Authentication Flow

1. **Registration:**
   - Email validation (format + uniqueness)
   - Password requirements: min 8 chars, 1 uppercase, 1 number
   - Auto-login after registration
   - Create default workspace for new user

2. **Login:**
   - Email/password authentication
   - JWT token storage in httpOnly cookie + memory
   - Redirect to last visited workspace or dashboard

3. **Session:**
   - Access token expires in 15 minutes
   - Refresh token expires in 7 days
   - Auto-refresh before expiry

### 6.2 Workspace Management

1. **Create Workspace:**
   - Name (required, 3-50 chars)
   - Auto-generate slug from name
   - Creator becomes OWNER

2. **Invite Members:**
   - Invite by email
   - Role selection: ADMIN, MEMBER
   - Email notification (mocked)
   - Pending invitations list

3. **Member Management:**
   - View all members
   - Change member role
   - Remove members (OWNER/ADMIN only)

### 6.3 Project Management

1. **Create Project:**
   - Name (required)
   - Key (2-6 chars, uppercase, unique in workspace)
   - Description (optional)
   - Color picker (preset colors)
   - Icon/emoji selector

2. **Project Dashboard:**
   - Project overview card
   - Quick stats (tasks, members)
   - Recent activity feed
   - Quick actions (new task, settings)

### 6.4 Task Management

1. **Create Task:**
   - Title (required)
   - Description (markdown supported)
   - Status (default: TODO)
   - Priority (default: MEDIUM)
   - Assignee (optional)
   - Due date (optional)
   - Labels (optional)
   - Checklist items (optional)

2. **Task Card Display:**
   - Task number (PROJ-1)
   - Title (truncated at 2 lines)
   - Priority indicator (color dot)
   - Assignee avatar (or initials)
   - Due date (with overdue styling)
   - Label chips (max 3 visible)

3. **Quick Actions (on card):**
   - Click to open detail modal
   - Drag to move
   - Status dropdown
   - Priority dropdown

### 6.5 Kanban Board

1. **Columns:**
   - TODO (gray indicator)
   - IN PROGRESS (blue indicator)
   - REVIEW (purple indicator)
   - DONE (green indicator)

2. **Drag and Drop:**
   - Visual feedback during drag
   - Smooth animation on drop
   - Auto-scroll when near edges
   - Position persistence

3. **Column Features:**
   - Task count badge
   - Add task button
   - Collapse/expand

### 6.6 Task Detail Modal

1. **Header:**
   - Task number and project key
   - Title (editable inline)
   - Close button

2. **Body Sections:**
   - **Left column (70%):**
     - Description (markdown editor)
     - Checklist with progress bar
     - Comments section
   
   - **Right sidebar (30%):**
     - Status selector
     - Priority selector
     - Assignee selector
     - Reporter display
     - Due date picker
     - Labels manager
     - Created/Updated timestamps

3. **Activity Log:**
   - Timeline view
   - Shows all field changes
   - Auto-updates on save

### 6.7 Dashboard

1. **Stats Cards:**
   - Total Tasks (with trend)
   - Completed (with percentage)
   - In Progress
   - Delayed (overdue)

2. **Charts:**
   - Tasks by status (bar chart)
   - Tasks by priority (pie/donut chart)
   - Weekly completion trend (line chart)

3. **Lists:**
   - Recent tasks
   - Overdue tasks
   - My assigned tasks

---

## 7. File Structure Summary

### Backend Files (Java/Spring Boot)
```
backend/
├── pom.xml
├── Dockerfile
├── docker-compose.yml
├── src/main/
│   ├── java/com/taskflow/
│   │   ├── TaskFlowApplication.java
│   │   ├── config/
│   │   ├── controller/
│   │   ├── dto/
│   │   ├── entity/
│   │   ├── repository/
│   │   ├── service/
│   │   ├── security/
│   │   └── exception/
│   └── resources/
│       └── application.yml
└── src/test/
```

### Frontend Files (Next.js/TypeScript)
```
frontend/
├── package.json
├── tailwind.config.ts
├── next.config.js
├── Dockerfile
├── docker-compose.yml
├── src/
│   ├── app/
│   ├── components/
│   ├── hooks/
│   ├── lib/
│   ├── store/
│   ├── types/
│   └── styles/
└── public/
```

---

## 8. Development Phases

### Phase 1: Project Setup (COMPLETED)
- [x] Backend: Spring Boot project structure
- [x] Frontend: Next.js project with TypeScript
- [x] Database: PostgreSQL with Docker
- [x] Documentation: This SPEC.md

### Phase 2: Backend Core (NOT STARTED)
- [x] Database entities and migrations
- [x] Authentication (JWT)
- [x] Workspace APIs
- [x] Project APIs
- [x] Task APIs
- [x] Comment APIs
- [x] Dashboard APIs

### Phase 3: Frontend Core (NOT STARTED)
- [x] Project setup with TailwindCSS
- [x] shadcn/ui components
- [x] Authentication pages
- [x] Layout components (Sidebar, Header)
- [x] API integration

### Phase 4: Features (NOT STARTED)
- [x] Kanban board with dnd-kit
- [x] Task detail modal
- [x] Dashboard with charts
- [x] Activity log
- [x] Comments system

### Phase 5: Polish (NOT STARTED)
- [x] Animations and transitions
- [x] Responsive design
- [x] Error handling
- [x] Loading states
- [x] Empty states

---

## 9. Acceptance Criteria

1. User can register and login with JWT authentication
2. User can create workspaces and invite members
3. User can create projects within workspaces
4. User can create, edit, and delete tasks
5. User can drag and drop tasks between columns
6. Kanban board updates in real-time
7. Task detail modal shows all task information
8. Dashboard displays accurate statistics
9. UI is fully responsive and works on mobile
10. Dark mode is the default theme
11. All animations are smooth (60fps)
12. No console errors in production build

---

## 10. Non-Goals (v1 MVP)

- Real-time collaboration (WebSockets)
- Email notifications
- File attachments
- Custom fields
- Time tracking
- Gantt charts
- AI features
- Mobile native apps
- SSO/OAuth providers
- Public workspaces
