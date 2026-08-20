CREATE TYPE settlement_status AS ENUM ('OPEN', 'CLOSED', 'PAID', 'REOPENED');

CREATE TABLE monthly_settlements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    child_id UUID NOT NULL REFERENCES child_profiles(id),
    year INTEGER NOT NULL,
    month INTEGER NOT NULL CHECK (month BETWEEN 1 AND 12),
    base_allowance DECIMAL(10,2) NOT NULL DEFAULT 0,
    extra_earnings DECIMAL(10,2) NOT NULL DEFAULT 0,
    bonuses DECIMAL(10,2) NOT NULL DEFAULT 0,
    penalties DECIMAL(10,2) NOT NULL DEFAULT 0,
    savings DECIMAL(10,2) NOT NULL DEFAULT 0,
    payable_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
    status settlement_status NOT NULL DEFAULT 'OPEN',
    closed_at TIMESTAMPTZ,
    paid_at TIMESTAMPTZ,
    closed_by UUID REFERENCES users(id),
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT monthly_settlements_child_period_key UNIQUE (child_id, year, month)
);

CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX notifications_user_id_read_idx ON notifications (user_id, read);

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    family_id UUID NOT NULL REFERENCES families(id),
    user_id UUID REFERENCES users(id),
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id UUID NOT NULL,
    old_value JSONB,
    new_value JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ip_address VARCHAR(45)
);

CREATE INDEX audit_logs_family_id_created_at_idx ON audit_logs (family_id, created_at);
