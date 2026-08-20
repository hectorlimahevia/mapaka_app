CREATE TYPE screen_source_type AS ENUM (
    'DAILY_BASE', 'TASK', 'BONUS', 'PENALTY', 'USAGE', 'MANUAL_ADJUSTMENT', 'REVERSAL'
);

CREATE TABLE screen_time_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    child_id UUID NOT NULL REFERENCES child_profiles(id),
    weekday INTEGER CHECK (weekday BETWEEN 0 AND 6),
    base_minutes INTEGER NOT NULL CHECK (base_minutes >= 0),
    maximum_minutes INTEGER,
    rollover_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    rollover_max_minutes INTEGER,
    valid_from DATE NOT NULL,
    valid_until DATE,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX screen_time_rules_child_id_idx ON screen_time_rules (child_id);

CREATE TABLE screen_time_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    child_id UUID NOT NULL REFERENCES child_profiles(id),
    transaction_type transaction_type NOT NULL,
    minutes INTEGER NOT NULL CHECK (minutes > 0),
    description VARCHAR(255),
    source_type screen_source_type NOT NULL,
    source_id UUID,
    -- Data local de la família (no UTC) a la qual pertany el moviment; permet la
    -- restricció idempotent de generació diària de la secció 15 de Família+.pdf.
    occurred_on DATE NOT NULL DEFAULT CURRENT_DATE,
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    reversed_transaction_id UUID REFERENCES screen_time_transactions(id)
);

CREATE INDEX screen_time_transactions_child_id_created_at_idx ON screen_time_transactions (child_id, created_at);

CREATE UNIQUE INDEX screen_time_transactions_daily_base_key
    ON screen_time_transactions (child_id, occurred_on)
    WHERE source_type = 'DAILY_BASE';
