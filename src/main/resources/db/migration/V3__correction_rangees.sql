-- =====================================================================
--  Correction des données de test : 20 places par rangée au lieu de 10
--  (avec 10 places par rangée, les salles de 300 places dépassaient la rangée Z).
--  Une migration déjà appliquée (V2) ne se modifie jamais : on corrige avec une nouvelle version.
-- =====================================================================

-- Étape 1 : décaler temporairement les numéros pour éviter tout conflit
-- avec la contrainte d'unicité (salle_id, rangee, numero) pendant la renumérotation
UPDATE place SET numero = numero + 1000;

-- Étape 2 : renuméroter : 20 places par rangée (A, B, C...)
UPDATE place p
SET rangee = chr((65 + (x.rang - 1) / 20)::int),
    numero = (((x.rang - 1) % 20) + 1)::int,
    pmr    = (x.rang <= 2)
FROM (SELECT id, row_number() OVER (PARTITION BY salle_id ORDER BY id) AS rang FROM place) x
WHERE p.id = x.id;
