-- Idioma preferit de l'usuari (mapaka_prompts_code.md Prompt 5, i18n): es recupera en
-- iniciar sessió des de qualsevol dispositiu, no només el que hi ha desat a localStorage.
ALTER TABLE users
    ADD COLUMN locale VARCHAR(5) NOT NULL DEFAULT 'ca';
