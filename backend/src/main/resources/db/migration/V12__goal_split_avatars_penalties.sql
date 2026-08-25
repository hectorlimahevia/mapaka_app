-- Prompt 15: repartiment de diners en N parts (gastar / estalvi / cada objectiu actiu),
-- avatars personalitzables, penalització en tasques de Responsabilitat, i finalitzacions
-- de tasca en grup (col·laboració entre germans).

ALTER TYPE wallet_type ADD VALUE 'GOAL';
ALTER TYPE money_source_type ADD VALUE 'GOAL_CONTRIBUTION';
ALTER TYPE money_source_type ADD VALUE 'DONATION';
ALTER TYPE money_source_type ADD VALUE 'TASK_PENALTY';

ALTER TABLE savings_goals
    ADD COLUMN allocation_percentage NUMERIC(5,2) NOT NULL DEFAULT 0
        CHECK (allocation_percentage >= 0 AND allocation_percentage <= 100);

-- Donacions d'un tercer (avi, padrí...) cap a un objectiu concret — mai passen pel
-- repartiment gastar/estalvi/objectiu del fill.
CREATE TABLE donations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    savings_goal_id UUID NOT NULL REFERENCES savings_goals(id),
    family_id UUID NOT NULL REFERENCES families(id),
    donor_name VARCHAR(120),
    message VARCHAR(280),
    amount NUMERIC(10,2) NOT NULL CHECK (amount > 0),
    created_by_user_id UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX donations_savings_goal_id_idx ON donations (savings_goal_id);

ALTER TABLE tasks
    ADD COLUMN penalty_money_amount NUMERIC(10,2) NOT NULL DEFAULT 0,
    ADD COLUMN penalty_screen_minutes INTEGER NOT NULL DEFAULT 0;

-- Totes les files creades juntes en una mateixa finalització col·laborativa comparteixen
-- aquest valor (una finalització individual també en genera un, per no tractar el cas
-- d'1 sol participant com una excepció).
ALTER TABLE task_completions ADD COLUMN completion_group_id UUID;
CREATE INDEX task_completions_group_id_idx ON task_completions (completion_group_id);

-- avatar_icon NULL vol dir "mostra la inicial del nom". color_theme queda restringit a
-- una paleta tancada de tons saturats (mai blanc ni molt clar, perquè la icona blanca
-- de sobre sempre hi tingui contrast) — mateixa llista al backend i al frontend.
ALTER TABLE child_profiles ADD COLUMN avatar_icon VARCHAR(50);
ALTER TABLE child_profiles ADD CONSTRAINT child_profiles_color_theme_check
    CHECK (color_theme IS NULL OR color_theme IN (
        '#6C4DFF', '#FF5D8F', '#FFC93C', '#2ECC71', '#3AA0FF',
        '#F5765B', '#8E44AD', '#16A085', '#E91E63'
    ));
