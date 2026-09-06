-- Codi curt i unic per familia (mai secret, a diferencia del recovery code) perque una
-- familia es pugui trobar sempre al login encara que hi hagi moltes amb el mateix nom —
-- mateix alfabet que el recovery code (sense caracters ambigus 0/O/1/I).
ALTER TABLE families ADD COLUMN family_code VARCHAR(8);

CREATE OR REPLACE FUNCTION mapaka_tmp_gen_family_code() RETURNS text AS $$
DECLARE
  chars text := 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
  result text := '';
  i integer;
BEGIN
  FOR i IN 1..6 LOOP
    result := result || substr(chars, floor(random() * length(chars) + 1)::int, 1);
  END LOOP;
  RETURN result;
END;
$$ LANGUAGE plpgsql;

UPDATE families SET family_code = mapaka_tmp_gen_family_code() WHERE family_code IS NULL;

DROP FUNCTION mapaka_tmp_gen_family_code();

ALTER TABLE families ALTER COLUMN family_code SET NOT NULL;
ALTER TABLE families ADD CONSTRAINT families_family_code_key UNIQUE (family_code);
