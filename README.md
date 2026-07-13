# TaskFlow - Modern Task Management Application

A modern, lightweight task management application inspired by Jira, Linear, and Notion. Built with Next.js 15, Spring Boot 3, and PostgreSQL.

![TaskFlow](https://img.shields.io/badge/TaskFlow-v1.0.0-6366f1)
![Next.js](https://img.shields.io/badge/Next.js-15-black)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green)
![Java](https://img.shields.io/badge/Java-21-orange)
![TypeScript](https://img.shields.io/badge/TypeScript-5.6-blue)

## Features

- **Authentication**: JWT-based authentication with secure token refresh
- **Workspace Management**: Create and manage multiple workspaces
- **Project Management**: Organize tasks within projects with custom colors and icons
- **Kanban Board**: Drag-and-drop task management with smooth animations
- **Task Details**: Full task management with comments, activity logs, and checklists
- **Dashboard**: Real-time statistics and progress tracking
- **Dark Mode**: Modern dark theme by default

## Tech Stack

### Frontend
- **Next.js 15** - React framework with App Router
- **TypeScript** - Type-safe development
- **TailwindCSS** - Utility-first CSS framework
- **shadcn/ui** - High-quality UI components
- **Zustand** - State management
- **dnd-kit** - Drag and drop functionality
- **React Query** - Data fetching and caching
- **React Hook Form** - Form handling

### Backend
- **Spring Boot 3.2** - Modern Java framework
- **Java 21** - Latest LTS Java version
- **PostgreSQL** - Robust relational database
- **Spring Security** - Security framework
- **JWT** - Authentication tokens
- **JPA/Hibernate** - ORM for database operations

## Project Structure

```
EXE121/
├── backend/                 # Spring Boot application
│   ├── src/main/java/com/taskflow/
│   │   ├── config/        # Configuration classes
│   │   ├── controller/    # REST controllers
│   │   ├── dto/           # Data transfer objects
│   │   ├── entity/        # JPA entities
│   │   ├── exception/     # Exception handling
│   │   ├── repository/    # JPA repositories
│   │   ├── security/      # Security components
│   │   └── service/       # Business logic
│   └── src/main/resources/
│       └── application.yml
├── frontend/               # Next.js application
│   ├── src/
│   │   ├── app/          # App router pages
│   │   ├── components/    # React components
│   │   │   ├── layout/   # Layout components
│   │   │   ├── tasks/    # Task components
│   │   │   ├── ui/       # UI components
│   │   │   └── providers/# Context providers
│   │   ├── lib/           # Utilities and API
│   │   ├── store/         # Zustand stores
│   │   └── types/         # TypeScript types
│   └── public/
├── docker-compose.yml     # PostgreSQL container
├── SPEC.md               # Detailed specification
└── README.md             # This file
```

## Getting Started

### Prerequisites

- **Node.js 18+** (for frontend)
- **Java 21** (for backend)
- **Maven 3.8+** (for backend)
- **Docker** (for PostgreSQL)

### Quick Start

#### 1. Clone the repository

```bash
cd EXE121
```

#### 2. Start PostgreSQL

```bash
docker-compose up -d
```

This will start PostgreSQL on port `5432` with:
- Database: `taskflow`
- Username: `taskflow`
- Password: `taskflow123`

#### 3. Start Backend

```bash
cd backend

# Build the project
./mvnw clean package -DskipTests

# Run the application
./mvnw spring-boot:run
```

Or run the JAR file:

```bash
java -jar target/taskflow-backend-1.0.0.jar
```

The backend will start on `http://localhost:8080`

#### 4. Start Frontend

```bash
cd frontend

# Install dependencies
npm install

# Start development server
npm run dev
```

The frontend will start on `http://localhost:3000`

### Default Configuration

#### Backend (application.yml)
```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/taskflow
    username: taskflow
    password: taskflow123

jwt:
  secret: TaskFlowSecretKey2026VeryLongSecretKeyForJWTSigning1234567890
  access-token-expiration: 900000  # 15 minutes
  refresh-token-expiration: 604800000  # 7 days
```

#### Frontend (.env.local)
```
NEXT_PUBLIC_API_URL=http://localhost:8080/api
```

## API Endpoints

### Authentication
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/register` | Register new user |
| POST | `/api/auth/login` | Login user |
| POST | `/api/auth/refresh` | Refresh access token |
| GET | `/api/auth/me` | Get current user |

### Workspaces
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/workspaces` | List all workspaces |
| POST | `/api/workspaces` | Create workspace |
| GET | `/api/workspaces/{id}` | Get workspace |
| PUT | `/api/workspaces/{id}` | Update workspace |
| DELETE | `/api/workspaces/{id}` | Delete workspace |
| POST | `/api/workspaces/{id}/invite` | Invite member |
| GET | `/api/workspaces/{id}/members` | List members |

### Projects
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/workspaces/{id}/projects` | List projects |
| POST | `/api/workspaces/{id}/projects` | Create project |
| GET | `/api/workspaces/{id}/projects/{pid}` | Get project |
| PUT | `/api/workspaces/{id}/projects/{pid}` | Update project |
| DELETE | `/api/workspaces/{id}/projects/{pid}` | Delete project |

### Tasks
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/projects/{id}/tasks` | List tasks |
| POST | `/api/projects/{id}/tasks` | Create task |
| GET | `/api/projects/{id}/tasks/{tid}` | Get task |
| PUT | `/api/projects/{id}/tasks/{tid}` | Update task |
| PATCH | `/api/projects/{id}/tasks/{tid}/position` | Move task |
| DELETE | `/api/projects/{id}/tasks/{tid}` | Delete task |

### Dashboard
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/dashboard/stats?workspaceId=` | Get statistics |

## Database Schema

### Users
| Column | Type | Description |
|--------|------|-------------|
| id | UUID | Primary key |
| email | VARCHAR(255) | Unique email |
| password | VARCHAR(255) | Hashed password |
| full_name | VARCHAR(100) | Display name |
| avatar_url | VARCHAR(500) | Profile image URL |

### Workspaces
| Column | Type | Description |
|--------|------|-------------|
| id | UUID | Primary key |
| name | VARCHAR(100) | Workspace name |
| slug | VARCHAR(100) | URL-friendly name |
| owner_id | UUID | Owner user reference |

### Projects
| Column | Type | Description |
|--------|------|-------------|
| id | UUID | Primary key |
| workspace_id | UUID | Parent workspace |
| name | VARCHAR(100) | Project name |
| key | VARCHAR(10) | Project key (e.g., PROJ) |
| color | VARCHAR(7) | Hex color code |

### Tasks
| Column | Type | Description |
|--------|------|-------------|
| id | UUID | Primary key |
| project_id | UUID | Parent project |
| task_number | INTEGER | Auto-increment per project |
| title | VARCHAR(255) | Task title |
| status | VARCHAR(20) | TODO, IN_PROGRESS, REVIEW, DONE |
| priority | VARCHAR(20) | LOW, MEDIUM, HIGH, URGENT |
| assignee_id | UUID | Assigned user |
| due_date | DATE | Due date |

## UI/UX Design

### Color Palette

| Name | Hex | Usage |
|------|-----|-------|
| Background | #09090b | Main background |
| Surface | #18181b | Cards, modals |
| Primary | #6366f1 | Primary actions (Indigo) |
| Success | #22c55e | Success states |
| Warning | #f59e0b | Warning states |
| Error | #ef4444 | Error states |

### Task Priorities
| Priority | Color |
|----------|-------|
| Urgent | #ef4444 (Red) |
| High | #f97316 (Orange) |
| Medium | #eab308 (Yellow) |
| Low | #22c55e (Green) |

### Task Statuses
| Status | Color |
|---------|-------|
| To Do | #71717a (Gray) |
| In Progress | #3b82f6 (Blue) |
| Review | #a855f7 (Purple) |
| Done | #22c55e (Green) |

## Development

### Backend Development

```bash
cd backend

# Run in development mode
./mvnw spring-boot:run

# Run tests
./mvnw test

# Build JAR
./mvnw clean package
```

### Frontend Development

```bash
cd frontend

# Install dependencies
npm install

# Run in development mode
npm run dev

# Build for production
npm run build

# Start production server
npm start
```

## Environment Variables

### Frontend (.env.local)
```env
NEXT_PUBLIC_API_URL=http://localhost:8080/api
```

### Backend (application.yml)
Configure in `src/main/resources/application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/taskflow
    username: taskflow
    password: taskflow123
```

## License

This project is licensed under the MIT License.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## Acknowledgments

- Design inspired by [Linear](https://linear.app)
- UI components from [shadcn/ui](https://ui.shadcn.com)
- Icons by [Lucide](https://lucide.dev)
