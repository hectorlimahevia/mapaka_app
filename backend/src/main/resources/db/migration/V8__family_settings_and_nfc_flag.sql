-- Configuració general de la família (mapaka_mockup.html — panell "Configuració"):
-- exactament els 3 interruptors aprovats a la maqueta, no tota la llista de la secció 46
-- de Família+.pdf (currency/timezone/etc. ja viuen a families i no calen aquí).
ALTER TABLE families
    ADD COLUMN task_approval_required BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN notify_pending_approvals_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN allow_savings_transfer BOOLEAN NOT NULL DEFAULT TRUE;

-- Permet marcar visualment a "Aprovacions" els repartiments NFC que han deixat algun
-- fill en saldo negatiu, sense haver de recalcular-ho a partir del ledger cada cop
-- (Prompt 7: "marcats visualment, no bloquegen res, és només informatiu").
ALTER TABLE screen_session_participant
    ADD COLUMN resulting_balance_negative BOOLEAN NOT NULL DEFAULT FALSE;
