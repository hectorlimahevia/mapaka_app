-- Suport per al registre públic de família i la recuperació de PIN
-- (mapaka_documento_global.md secció 4, mapaka_prompts_code.md Prompt 6).
ALTER TABLE families
    ADD COLUMN recovery_code_hash VARCHAR(255) NULL,
    ADD COLUMN recovery_code_generated_at TIMESTAMPTZ NULL;

-- Nom per mostrar del PARENT al selector "Qui ets?" del login (els fills ja el tenen a
-- child_profiles.display_name) — ara que tots dos rols entren amb família+perfil+PIN.
ALTER TABLE users
    ADD COLUMN display_name VARCHAR(100) NULL;
