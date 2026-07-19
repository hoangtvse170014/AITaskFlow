-- TaskFlow PostgreSQL Init Script
-- Safe, idempotent-ish initialization with schema and seed data

-- ============================================
-- UUID CONSTANTS FOR DEMO DATA (predictable FKs)
-- ============================================
DO $$
BEGIN
    PERFORM set_config('app.demo_user_id', 'a1111111-1111-1111-1111-111111111111', true);
    PERFORM set_config('app.workspace_id', 'b2222222-2222-2222-2222-222222222222', true);
    PERFORM set_config('app.project_id', 'c3333333-3333-3333-3333-333333333333', true);
    PERFORM set_config('app.task1_id', 'd4444444-4444-4444-4444-444444444441', true);
    PERFORM set_config('app.task2_id', 'd4444444-4444-4444-4444-444444444442', true);
    PERFORM set_config('app.page1_id', 'e5555555-5555-5555-5555-555555555551', true);
    PERFORM set_config('app.goal1_id', 'f6666666-6666-6666-6666-666666666661', true);
END $$;

-- ============================================
-- TABLES
-- ============================================

-- Users
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    avatar_url VARCHAR(500),
    provider VARCHAR(50) DEFAULT 'LOCAL',
    provider_id VARCHAR(255),
    full_name VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Workspaces
CREATE TABLE IF NOT EXISTS workspaces (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(100) UNIQUE,
    logo VARCHAR(500),
    description TEXT,
    owner_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Workspace Members
CREATE TABLE IF NOT EXISTS workspace_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL,
    joined_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(workspace_id, user_id)
);

-- Projects
CREATE TABLE IF NOT EXISTS projects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    project_key VARCHAR(20) NOT NULL,
    description TEXT,
    color VARCHAR(7) DEFAULT '#6366f1',
    icon VARCHAR(50),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Tasks
CREATE TABLE IF NOT EXISTS tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    task_number INTEGER NOT NULL,
    title VARCHAR(500) NOT NULL,
    description TEXT,
    status VARCHAR(20) DEFAULT 'TODO',
    priority VARCHAR(20) DEFAULT 'MEDIUM',
    assignee_id UUID REFERENCES users(id) ON DELETE SET NULL,
    reporter_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    due_date DATE,
    labels TEXT,
    checklist TEXT,
    position INTEGER DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Pages
CREATE TABLE IF NOT EXISTS pages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    parent_page_id UUID REFERENCES pages(id) ON DELETE CASCADE,
    title VARCHAR(500) NOT NULL,
    icon VARCHAR(50),
    cover_url VARCHAR(500),
    slug VARCHAR(200),
    is_public BOOLEAN DEFAULT false,
    is_archived BOOLEAN DEFAULT false,
    is_deleted BOOLEAN DEFAULT false,
    favorite_order INTEGER,
    sidebar_order INTEGER,
    created_by UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    updated_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Blocks
CREATE TABLE IF NOT EXISTS blocks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    page_id UUID NOT NULL REFERENCES pages(id) ON DELETE CASCADE,
    parent_block_id UUID REFERENCES blocks(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,
    content TEXT,
    position INTEGER DEFAULT 0,
    created_by UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    updated_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Comments
CREATE TABLE IF NOT EXISTS comments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id UUID NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Activity Logs
CREATE TABLE IF NOT EXISTS activity_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id UUID NOT NULL,
    metadata TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Notifications
CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,
    message VARCHAR(500) NOT NULL,
    is_read BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Favorites
CREATE TABLE IF NOT EXISTS favorites (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    page_id UUID NOT NULL REFERENCES pages(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, page_id)
);

-- Goals
CREATE TABLE IF NOT EXISTS goals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    owner_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(500) NOT NULL,
    description TEXT,
    type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    period VARCHAR(50) NOT NULL,
    start_date DATE NOT NULL,
    due_date DATE NOT NULL,
    progress_percentage INTEGER DEFAULT 0,
    current_value DOUBLE PRECISION DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Key Results
CREATE TABLE IF NOT EXISTS key_results (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    goal_id UUID NOT NULL REFERENCES goals(id) ON DELETE CASCADE,
    assignee_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(500) NOT NULL,
    metric_type VARCHAR(50) NOT NULL,
    start_value DOUBLE PRECISION DEFAULT 0,
    target_value DOUBLE PRECISION DEFAULT 100,
    current_value DOUBLE PRECISION DEFAULT 0,
    due_date DATE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Permissions
CREATE TABLE IF NOT EXISTS permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) UNIQUE NOT NULL,
    description VARCHAR(500),
    resource_type VARCHAR(50) NOT NULL,
    action_type VARCHAR(50) NOT NULL
);

-- Roles
CREATE TABLE IF NOT EXISTS roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) UNIQUE NOT NULL,
    description VARCHAR(500),
    priority INTEGER NOT NULL,
    is_workspace_role BOOLEAN DEFAULT true
);

-- Role Permissions (junction table)
CREATE TABLE IF NOT EXISTS role_permissions (
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY(role_id, permission_id)
);

-- Member Workloads
CREATE TABLE IF NOT EXISTS member_workloads (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    member_id UUID NOT NULL REFERENCES workspace_members(id) ON DELETE CASCADE,
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    date DATE NOT NULL,
    open_tasks INTEGER DEFAULT 0,
    completed_tasks INTEGER DEFAULT 0,
    in_progress_tasks INTEGER DEFAULT 0,
    blocked_tasks INTEGER DEFAULT 0,
    total_hours_estimated DOUBLE PRECISION DEFAULT 0,
    total_hours_logged DOUBLE PRECISION DEFAULT 0,
    workload_percentage INTEGER DEFAULT 0,
    status VARCHAR(50) NOT NULL,
    UNIQUE(member_id, date)
);

-- Search History
CREATE TABLE IF NOT EXISTS search_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    query VARCHAR(500) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    result_count INTEGER DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Subtasks
CREATE TABLE IF NOT EXISTS subtasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    parent_task_id UUID NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    title VARCHAR(500) NOT NULL,
    is_completed BOOLEAN DEFAULT false,
    position INTEGER DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Task Comments
CREATE TABLE IF NOT EXISTS task_comments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id UUID NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Task Activity Logs
CREATE TABLE IF NOT EXISTS task_activity_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id UUID NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    action VARCHAR(100) NOT NULL,
    old_value TEXT,
    new_value TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Invitations
CREATE TABLE IF NOT EXISTS invitations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    email VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    token VARCHAR(100) UNIQUE NOT NULL,
    accepted BOOLEAN DEFAULT false,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Due Date Reminders
CREATE TABLE IF NOT EXISTS due_date_reminders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id UUID NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reminder_date DATE NOT NULL,
    sent BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Database Views
CREATE TABLE IF NOT EXISTS database_views (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    config JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Database Entities
CREATE TABLE IF NOT EXISTS database_entities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    view_id UUID NOT NULL REFERENCES database_views(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    rows INTEGER DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Database Columns
CREATE TABLE IF NOT EXISTS database_columns (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_id UUID NOT NULL REFERENCES database_entities(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    data_type VARCHAR(50) NOT NULL,
    is_primary BOOLEAN DEFAULT false,
    is_nullable BOOLEAN DEFAULT true,
    position INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Database Rows
CREATE TABLE IF NOT EXISTS database_rows (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_id UUID NOT NULL REFERENCES database_entities(id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Database Cells
CREATE TABLE IF NOT EXISTS database_cells (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    row_id UUID NOT NULL REFERENCES database_rows(id) ON DELETE CASCADE,
    column_id UUID NOT NULL REFERENCES database_columns(id) ON DELETE CASCADE,
    value TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================
-- INDEXES
-- ============================================

CREATE INDEX IF NOT EXISTS idx_tasks_project_status ON tasks(project_id, status);
CREATE INDEX IF NOT EXISTS idx_pages_workspace_title ON pages(workspace_id, title);
CREATE INDEX IF NOT EXISTS idx_blocks_page_position ON blocks(page_id, parent_block_id, position);
CREATE INDEX IF NOT EXISTS idx_workspace_members_workspace ON workspace_members(workspace_id);
CREATE INDEX IF NOT EXISTS idx_workspace_members_user ON workspace_members(user_id);
CREATE INDEX IF NOT EXISTS idx_activity_logs_workspace ON activity_logs(workspace_id);
CREATE INDEX IF NOT EXISTS idx_notifications_user ON notifications(user_id, is_read);
CREATE INDEX IF NOT EXISTS idx_key_results_goal ON key_results(goal_id);
CREATE INDEX IF NOT EXISTS idx_subtasks_parent ON subtasks(parent_task_id);
CREATE INDEX IF NOT EXISTS idx_comments_task ON comments(task_id);

-- ============================================
-- SEED DATA
-- ============================================

-- Demo User
INSERT INTO users (id, email, password, full_name, provider, created_at, updated_at)
VALUES (
    'a1111111-1111-1111-1111-111111111111',
    'demo@example.com',
    '$2a$10$demoencodedpasswordplaceholder',
    'Demo User',
    'LOCAL',
    NOW(),
    NOW()
) ON CONFLICT (email) DO NOTHING;

-- Demo Workspace
INSERT INTO workspaces (id, name, slug, description, owner_id, created_at, updated_at)
VALUES (
    'b2222222-2222-2222-2222-222222222222',
    'Demo Workspace',
    'demo-workspace',
    'This is a demo workspace for TaskFlow',
    'a1111111-1111-1111-1111-111111111111',
    NOW(),
    NOW()
) ON CONFLICT (slug) DO NOTHING;

-- Workspace Member (owner)
INSERT INTO workspace_members (workspace_id, user_id, role, joined_at)
VALUES (
    'b2222222-2222-2222-2222-222222222222',
    'a1111111-1111-1111-1111-111111111111',
    'ADMIN',
    NOW()
) ON CONFLICT (workspace_id, user_id) DO NOTHING;

-- Demo Project
INSERT INTO projects (id, workspace_id, name, project_key, description, color, icon, created_at, updated_at)
VALUES (
    'c3333333-3333-3333-3333-333333333333',
    'b2222222-2222-2222-2222-222222222222',
    'Demo Project',
    'DEMO',
    'A demo project to showcase TaskFlow features',
    '#6366f1',
    'rocket',
    NOW(),
    NOW()
);

-- Task 1
INSERT INTO tasks (id, project_id, task_number, title, description, status, priority, reporter_id, created_at, updated_at)
VALUES (
    'd4444444-4444-4444-4444-444444444441',
    'c3333333-3333-3333-3333-333333333333',
    1,
    'Set up project structure',
    'Create the initial folder structure and configuration files for the project.',
    'DONE',
    'HIGH',
    'a1111111-1111-1111-1111-111111111111',
    NOW(),
    NOW()
);

-- Task 2
INSERT INTO tasks (id, project_id, task_number, title, description, status, priority, assignee_id, reporter_id, due_date, created_at, updated_at)
VALUES (
    'd4444444-4444-4444-4444-444444444442',
    'c3333333-3333-3333-3333-333333333333',
    2,
    'Write documentation',
    'Document all API endpoints and key features.',
    'IN_PROGRESS',
    'MEDIUM',
    'a1111111-1111-1111-1111-111111111111',
    'a1111111-1111-1111-1111-111111111111',
    CURRENT_DATE + INTERVAL '7 days',
    NOW(),
    NOW()
);

-- Demo Page
INSERT INTO pages (id, workspace_id, title, slug, created_by, created_at, updated_at)
VALUES (
    'e5555555-5555-5555-5555-555555555551',
    'b2222222-2222-2222-2222-222222222222',
    'Getting Started',
    'getting-started',
    'a1111111-1111-1111-1111-111111111111',
    NOW(),
    NOW()
);

-- Block 1 (heading)
INSERT INTO blocks (id, page_id, type, content, position, created_by, created_at, updated_at)
VALUES (
    'f7777777-7777-7777-7777-777777777771',
    'e5555555-5555-5555-5555-555555555551',
    'heading',
    '# Welcome to TaskFlow',
    0,
    'a1111111-1111-1111-1111-111111111111',
    NOW(),
    NOW()
);

-- Block 2 (paragraph)
INSERT INTO blocks (id, page_id, type, content, position, created_by, created_at, updated_at)
VALUES (
    'f7777777-7777-7777-7777-777777777772',
    'e5555555-5555-5555-5555-555555555551',
    'paragraph',
    'This is your getting started guide. Here you will learn how to use TaskFlow effectively.',
    1,
    'a1111111-1111-1111-1111-111111111111',
    NOW(),
    NOW()
);

-- Demo Goal
INSERT INTO goals (id, workspace_id, owner_id, title, description, type, status, period, start_date, due_date, progress_percentage, created_at, updated_at)
VALUES (
    'f6666666-6666-6666-6666-666666666661',
    'b2222222-2222-2222-2222-222222222222',
    'a1111111-1111-1111-1111-111111111111',
    'Q3 Product Launch',
    'Launch the new product features by end of Q3',
    'quarterly',
    'in_progress',
    '2026-Q3',
    '2026-07-01',
    '2026-09-30',
    35,
    NOW(),
    NOW()
);

-- Key Result 1
INSERT INTO key_results (id, goal_id, assignee_id, title, metric_type, start_value, target_value, current_value, due_date, created_at, updated_at)
VALUES (
    'g8888888-8888-8888-8888-888888888881',
    'f6666666-6666-6666-6666-666666666661',
    'a1111111-1111-1111-1111-111111111111',
    'Complete API documentation',
    'percentage',
    0,
    100,
    45,
    '2026-09-30',
    NOW(),
    NOW()
);

-- Key Result 2
INSERT INTO key_results (id, goal_id, assignee_id, title, metric_type, start_value, target_value, current_value, due_date, created_at, updated_at)
VALUES (
    'g8888888-8888-8888-8888-888888888882',
    'f6666666-6666-6666-6666-666666666661',
    'a1111111-1111-1111-1111-111111111111',
    'User onboarding flow implementation',
    'percentage',
    0,
    100,
    25,
    '2026-09-30',
    NOW(),
    NOW()
);

-- Demo Notification
INSERT INTO notifications (user_id, type, message, is_read, created_at)
VALUES (
    'a1111111-1111-1111-1111-111111111111',
    'task_assigned',
    'You have been assigned to "Write documentation"',
    false,
    NOW()
);

-- Demo Activity Log
INSERT INTO activity_logs (workspace_id, user_id, action, entity_type, entity_id, created_at)
VALUES (
    'b2222222-2222-2222-2222-222222222222',
    'a1111111-1111-1111-1111-111111111111',
    'created',
    'task',
    'd4444444-4444-4444-4444-444444444441',
    NOW()
);

-- Demo Search History
INSERT INTO search_history (user_id, query, entity_type, result_count, created_at)
VALUES (
    'a1111111-1111-1111-1111-111111111111',
    'documentation',
    'tasks',
    3,
    NOW()
);

-- ============================================
-- SEED ROLES AND PERMISSIONS
-- ============================================

INSERT INTO permissions (id, name, description, resource_type, action_type)
VALUES
    ('h1111111-1111-1111-1111-111111111111', 'view_workspace', 'View workspace', 'workspace', 'read'),
    ('h1111111-1111-1111-1111-111111111112', 'manage_workspace', 'Manage workspace settings', 'workspace', 'write'),
    ('h1111111-1111-1111-1111-111111111113', 'create_project', 'Create projects', 'project', 'write'),
    ('h1111111-1111-1111-1111-111111111114', 'manage_members', 'Manage workspace members', 'member', 'write')
ON CONFLICT (name) DO NOTHING;

INSERT INTO roles (id, name, description, priority, is_workspace_role)
VALUES
    ('i1111111-1111-1111-1111-111111111111', 'ADMIN', 'Workspace administrator with full access', 1, true),
    ('i1111111-1111-1111-1111-111111111112', 'MEMBER', 'Regular workspace member', 2, true)
ON CONFLICT (name) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
VALUES
    ('i1111111-1111-1111-1111-111111111111', 'h1111111-1111-1111-1111-111111111111'),
    ('i1111111-1111-1111-1111-111111111111', 'h1111111-1111-1111-1111-111111111112'),
    ('i1111111-1111-1111-1111-111111111111', 'h1111111-1111-1111-1111-111111111113'),
    ('i1111111-1111-1111-1111-111111111111', 'h1111111-1111-1111-1111-111111111114'),
    ('i1111111-1111-1111-1111-111111111112', 'h1111111-1111-1111-1111-111111111111'),
    ('i1111111-1111-1111-1111-111111111112', 'h1111111-1111-1111-1111-111111111113')
ON CONFLICT DO NOTHING;
