CREATE TYPE savings_goal_status AS ENUM ('ACTIVE', 'COMPLETED', 'CANCELLED');
CREATE TYPE adjustment_type AS ENUM ('BONUS', 'PENALTY', 'MANUAL');

CREATE TABLE savings_goals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    child_id UUID NOT NULL REFERENCES child_profiles(id),
    name VARCHAR(150) NOT NULL,
    target_amount DECIMAL(10,2) NOT NULL CHECK (target_amount > 0),
    image_url VARCHAR(255),
    status savings_goal_status NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at TIMESTAMPTZ
);

CREATE INDEX savings_goals_child_id_idx ON savings_goals (child_id);

-- Bonificacions i penalitzacions independents del sistema de tasques (secció 17).
CREATE TABLE adjustments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    child_id UUID NOT NULL REFERENCES child_profiles(id),
    adjustment_type adjustment_type NOT NULL,
    money_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
    savings_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
    screen_minutes INTEGER NOT NULL DEFAULT 0,
    reason VARCHAR(255) NOT NULL,
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX adjustments_child_id_idx ON adjustments (child_id);
