-- Permet iniciar una sessió de pantalla manualment des de la sessió autenticada del
-- pare/mare (sense targeta NFC ni dispositiu compatible): family_id/started_by
-- identifiquen la sessió quan no ve d'una etiqueta (screen_tag_id NULL), reaprofitant
-- tota la resta del motor (aturar, repartir entre germans, marcar saldo negatiu) tal
-- qual ja funciona per NFC.
ALTER TYPE screen_source_type ADD VALUE 'PARENT_SESSION';

ALTER TABLE screen_session
    ALTER COLUMN screen_tag_id DROP NOT NULL,
    ADD COLUMN family_id UUID REFERENCES families(id),
    ADD COLUMN started_by UUID REFERENCES users(id);

UPDATE screen_session ss
    SET family_id = st.family_id
    FROM screen_tag st
    WHERE ss.screen_tag_id = st.id;

ALTER TABLE screen_session
    ALTER COLUMN family_id SET NOT NULL;

-- Com a molt una sessió manual ACTIVE per família a la vegada (les sessions per
-- etiqueta ja tenen el seu propi índex per tag més amunt, a V7).
CREATE UNIQUE INDEX screen_session_one_active_manual_per_family
    ON screen_session (family_id)
    WHERE status = 'ACTIVE' AND screen_tag_id IS NULL;
