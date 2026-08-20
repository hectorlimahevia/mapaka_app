CREATE TYPE allowance_status AS ENUM ('DRAFT', 'CONFIRMED', 'CANCELLED');
CREATE TYPE wallet_type AS ENUM ('SPENDING', 'SAVINGS');
CREATE TYPE transaction_type AS ENUM ('CREDIT', 'DEBIT');
CREATE TYPE money_source_type AS ENUM (
    'MONTHLY_ALLOWANCE', 'TASK', 'BONUS', 'PENALTY', 'PURCHASE',
    'SAVINGS_TRANSFER', 'MANUAL_ADJUSTMENT', 'SETTLEMENT', 'REVERSAL'
);

CREATE TABLE allowance_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    family_id UUID NOT NULL REFERENCES families(id),
    child_id UUID REFERENCES child_profiles(id),
    min_age INTEGER,
    max_age INTEGER,
    monthly_amount DECIMAL(10,2) NOT NULL CHECK (monthly_amount >= 0),
    spending_percentage DECIMAL(5,2) NOT NULL CHECK (spending_percentage >= 0),
    savings_percentage DECIMAL(5,2) NOT NULL CHECK (savings_percentage >= 0),
    effective_from DATE NOT NULL,
    effective_to DATE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT allowance_rules_percentages_sum_100 CHECK (spending_percentage + savings_percentage = 100)
);

CREATE INDEX allowance_rules_family_id_idx ON allowance_rules (family_id);
CREATE INDEX allowance_rules_child_id_idx ON allowance_rules (child_id);

CREATE TABLE monthly_allowances (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    child_id UUID NOT NULL REFERENCES child_profiles(id),
    year INTEGER NOT NULL,
    month INTEGER NOT NULL CHECK (month BETWEEN 1 AND 12),
    gross_amount DECIMAL(10,2) NOT NULL,
    spending_amount DECIMAL(10,2) NOT NULL,
    savings_amount DECIMAL(10,2) NOT NULL,
    allowance_rule_id UUID NOT NULL REFERENCES allowance_rules(id),
    status allowance_status NOT NULL DEFAULT 'DRAFT',
    generated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    confirmed_at TIMESTAMPTZ,
    confirmed_by UUID REFERENCES users(id),
    CONSTRAINT monthly_allowances_child_period_key UNIQUE (child_id, year, month),
    CONSTRAINT monthly_allowances_amounts_sum CHECK (spending_amount + savings_amount = gross_amount)
);

CREATE TABLE money_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    child_id UUID NOT NULL REFERENCES child_profiles(id),
    wallet_type wallet_type NOT NULL,
    transaction_type transaction_type NOT NULL,
    amount DECIMAL(10,2) NOT NULL CHECK (amount > 0),
    description VARCHAR(255),
    source_type money_source_type NOT NULL,
    source_id UUID,
    transfer_reference_id UUID,
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    reversed_transaction_id UUID REFERENCES money_transactions(id)
);

CREATE INDEX money_transactions_child_id_created_at_idx ON money_transactions (child_id, created_at);
