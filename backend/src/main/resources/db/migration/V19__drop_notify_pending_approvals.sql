-- L'avís per correu de les aprovacions pendents mai es va arribar a implementar (no hi ha
-- cap enviament de correu ni se'n recull l'adreça): el valor es guardava però no es llegia.
ALTER TABLE families DROP COLUMN notify_pending_approvals_enabled;
