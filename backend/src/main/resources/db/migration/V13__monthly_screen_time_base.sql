-- El temps de pantalla base passa de generar-se diàriament (DAILY_BASE) a generar-se
-- un cop al mes, en el mateix acte que "Generar la paga del mes" (Prompt 16, punt 14/24
-- de la verificació: la implementació real mai es va migrar al disseny documentat).
ALTER TYPE screen_source_type ADD VALUE 'MONTHLY_BASE';
