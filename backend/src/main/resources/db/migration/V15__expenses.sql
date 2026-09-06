-- Gastos: el fill (si té permís) o el pare poden reflectir un gasto que descompta
-- directament del "per gastar" — mai de l'estalvi ni dels objectius. Quan el registra
-- el fill queda PENDING fins que un pare l'aprova (mateix patró que les tasques); quan
-- el registra el pare té efecte immediat (mateix patró que els ajustos manuals).
CREATE TYPE expense_status AS ENUM ('PENDING', 'APPROVED', 'REJECTED');

CREATE TABLE expenses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    child_id UUID NOT NULL REFERENCES child_profiles(id),
    amount DECIMAL(10,2) NOT NULL CHECK (amount > 0),
    reason VARCHAR(255) NOT NULL,
    status expense_status NOT NULL DEFAULT 'PENDING',
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    reviewed_by UUID REFERENCES users(id),
    reviewed_at TIMESTAMPTZ
);

CREATE INDEX expenses_child_id_idx ON expenses (child_id);

-- Permís per fill (com allowance_enabled/screen_time_enabled) perquè un pare el pugui
-- desactivar per als més petits sense afectar la resta de germans.
ALTER TABLE child_profiles ADD COLUMN can_log_expenses BOOLEAN NOT NULL DEFAULT TRUE;
