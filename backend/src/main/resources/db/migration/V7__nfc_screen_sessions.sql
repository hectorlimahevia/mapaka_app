-- Funcionalitat de temps de pantalla per NFC (objecte físic compartit) —
-- mapaka_documento_global.md secció 6 i mapaka_prompts_code.md Prompt 3.
ALTER TYPE screen_source_type ADD VALUE 'NFC_SESSION';

CREATE TYPE screen_session_status AS ENUM ('ACTIVE', 'CLOSED');

CREATE TABLE screen_tag (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    family_id UUID NOT NULL REFERENCES families(id),
    token VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX screen_tag_family_id_idx ON screen_tag (family_id);

CREATE TABLE screen_session (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    screen_tag_id UUID NOT NULL REFERENCES screen_tag(id),
    started_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ended_at TIMESTAMPTZ,
    elapsed_seconds INTEGER,
    status screen_session_status NOT NULL DEFAULT 'ACTIVE'
);

-- Com a molt una sessió ACTIVE per etiqueta al mateix temps (Prompt 4: el mateix
-- toc serveix per iniciar i per aturar).
CREATE UNIQUE INDEX screen_session_one_active_per_tag
    ON screen_session (screen_tag_id)
    WHERE status = 'ACTIVE';

CREATE TABLE screen_session_participant (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL REFERENCES screen_session(id),
    child_id UUID NOT NULL REFERENCES child_profiles(id),
    assigned_seconds INTEGER NOT NULL CHECK (assigned_seconds >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT screen_session_participant_session_child_key UNIQUE (session_id, child_id)
);
