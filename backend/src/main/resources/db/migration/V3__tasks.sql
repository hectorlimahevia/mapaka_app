CREATE TYPE task_type AS ENUM ('RESPONSIBILITY', 'EXTRA');
CREATE TYPE recurrence_type AS ENUM ('NONE', 'DAILY', 'WEEKLY', 'MONTHLY', 'CUSTOM');
CREATE TYPE task_completion_status AS ENUM ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED');

CREATE TABLE tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    family_id UUID NOT NULL REFERENCES families(id),
    name VARCHAR(150) NOT NULL,
    description TEXT,
    task_type task_type NOT NULL,
    icon VARCHAR(50),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    requires_approval BOOLEAN NOT NULL DEFAULT TRUE,
    repeatable BOOLEAN NOT NULL DEFAULT FALSE,
    recurrence_type recurrence_type NOT NULL DEFAULT 'NONE',
    max_completions_per_period INTEGER,
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX tasks_family_id_idx ON tasks (family_id);

CREATE TABLE task_rewards (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id UUID NOT NULL REFERENCES tasks(id),
    money_amount DECIMAL(10,2) NOT NULL DEFAULT 0 CHECK (money_amount >= 0),
    savings_amount DECIMAL(10,2) NOT NULL DEFAULT 0 CHECK (savings_amount >= 0),
    screen_minutes INTEGER NOT NULL DEFAULT 0 CHECK (screen_minutes >= 0),
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE task_assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id UUID NOT NULL REFERENCES tasks(id),
    child_id UUID NOT NULL REFERENCES child_profiles(id),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    available_from DATE,
    available_until DATE,
    CONSTRAINT task_assignments_task_child_key UNIQUE (task_id, child_id)
);

CREATE INDEX task_assignments_child_id_idx ON task_assignments (child_id);

CREATE TABLE task_completions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id UUID NOT NULL REFERENCES tasks(id),
    child_id UUID NOT NULL REFERENCES child_profiles(id),
    completed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    status task_completion_status NOT NULL DEFAULT 'PENDING',
    submitted_comment VARCHAR(255),
    reviewed_by UUID REFERENCES users(id),
    reviewed_at TIMESTAMPTZ,
    review_comment VARCHAR(255),
    reward_money DECIMAL(10,2) NOT NULL DEFAULT 0,
    reward_savings DECIMAL(10,2) NOT NULL DEFAULT 0,
    reward_screen_minutes INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX task_completions_child_id_status_idx ON task_completions (child_id, status);
CREATE INDEX task_completions_task_id_idx ON task_completions (task_id);
