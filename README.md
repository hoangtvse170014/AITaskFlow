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
├── docker-compose.dev.yml # PostgreSQL container for dev
├── init-scripts/          # Auto-run SQL scripts for Postgres init
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
git clone https://github.com/hoangtvse170014/AITaskFlow.git
cd AITaskFlow
```

#### 2. Start PostgreSQL with Docker

```bash
docker compose up -d
```

This starts PostgreSQL on port `5432` and automatically runs `init-scripts/00-init.sql` on first startup to create schema and seed demo data.

Default DB settings:
- Database: `taskflow`
- Username: `postgres`
- Password: `postgres`

To reset DB demo data:
```bash
docker compose down -v
docker compose up -d
```

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

The backend will start on `http://localhost:8081` in dev profile, or `http://localhost:8080` in prod profile.

#### 4. Start Frontend

```bash
cd frontend

# Install dependencies
npm install

# Start development server
npm run dev
```

The frontend will start on `http://localhost:3000`

### Environment Variables

Create `.env.local` in `frontend/` if needed:

```env
NEXT_PUBLIC_API_URL=http://localhost:8080/api
```

Backend reads from `application.yml` and supports env overrides:
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `GROQ_API_KEY`

Do NOT commit secrets. Use local env vars or Docker env files.

## Database

- Schema and seed are versioned in `init-scripts/00-init.sql`.
- `docker-compose.yml` mounts `init-scripts` into `/docker-entrypoint-initdb.d` so Postgres auto-initializes.
- For production, use a proper migration strategy and do not ship seed data.

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

## License

This project is licensed under the MIT License.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## Acknowledgments

- Design inspired by [Linear](https://linear.app)
- UI components from [shadcn/ui](https://ui.shadcn.com)
- Icons by [Lucide](https://lucide.dev)
