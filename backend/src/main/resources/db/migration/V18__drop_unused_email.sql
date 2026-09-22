-- Neteja del camp email, vestigi del disseny original (login d'adult per email+password,
-- Família+.pdf secció 38) que mai es va arribar a implementar al frontend ni s'omple en cap
-- flux real (registre, alta d'un segon pare): tots dos rols inicien sessió sempre per
-- family_id + username + PIN.
ALTER TABLE users DROP CONSTRAINT users_identifier_present;
DROP INDEX users_email_key;
ALTER TABLE users DROP COLUMN email;
ALTER TABLE users ALTER COLUMN username SET NOT NULL;
