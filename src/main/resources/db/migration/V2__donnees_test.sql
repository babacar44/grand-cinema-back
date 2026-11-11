-- =====================================================================
--  Le Grand Cinéma – Données de test (fictives)
--  Fichier de migration Flyway : src/main/resources/db/migration/V2__donnees_test.sql
--  Les comptes utilisateurs (admin / client) sont créés au démarrage de
--  l'application, avec un mot de passe chiffré en BCrypt.
-- =====================================================================

-- Types de salle et tarifs (US 18, US 39)
INSERT INTO type_salle (code, libelle, prix) VALUES
    ('PREMIUM_IMAX',   'Premium / IMAX',            14.50),
    ('STANDARD',       'Standard',                   9.90),
    ('VIP',            'VIP',                       18.00),
    ('EVENEMENTIELLE', 'Événementielle / modulable', 12.00);

-- Les 10 salles (1 700 places au total, comme dans l'énoncé)
INSERT INTO salle (nom, type_salle_code, capacite) VALUES
    ('Salle 1',  'PREMIUM_IMAX',   300),
    ('Salle 2',  'PREMIUM_IMAX',   300),
    ('Salle 3',  'STANDARD',       180),
    ('Salle 4',  'STANDARD',       180),
    ('Salle 5',  'STANDARD',       180),
    ('Salle 6',  'STANDARD',       180),
    ('Salle 7',  'VIP',            120),
    ('Salle 8',  'VIP',            120),
    ('Salle 9',  'EVENEMENTIELLE',  70),
    ('Salle 10', 'EVENEMENTIELLE',  70);

-- Génération automatique des places : 10 places par rangée (A, B, C...)
-- Les 2 premières places de la rangée A sont réservées aux personnes à mobilité réduite
INSERT INTO place (salle_id, rangee, numero, pmr)
SELECT s.id,
       chr(65 + ((n - 1) / 10)),            -- 65 = 'A'
       ((n - 1) % 10) + 1,
       (n <= 2)
FROM salle s
CROSS JOIN LATERAL generate_series(1, s.capacite) AS n;

-- Quelques films
INSERT INTO film (titre, synopsis, duree_minutes, genre, realisateur, age_minimum) VALUES
    ('Les Notes du Fleuve', 'Un pianiste de jazz retrouve sa ville natale le temps d''un festival.', 112, 'Drame',      'Claire Dumont', 0),
    ('Orbite 9',            'Un équipage tente de rejoindre la Terre après une panne critique.', 128, 'Science-fiction', 'Marc Leroy',   12),
    ('Le Grand Bal',        'Comédie musicale dans les coulisses d''un cabaret lyonnais.',         98, 'Comédie',    'Inès Martin',   0);

-- Séances des 7 prochains jours (fin = durée du film + 20 minutes de battement)
INSERT INTO seance (film_id, salle_id, debut, fin, version)
SELECT f.id,
       s.id,
       (CURRENT_DATE + d) + h.heure,
       (CURRENT_DATE + d) + h.heure + make_interval(mins => f.duree_minutes + 20),
       'VF'
FROM generate_series(1, 7) AS d
CROSS JOIN (VALUES (TIME '14:00'), (TIME '20:30')) AS h(heure)
JOIN film  f ON TRUE
JOIN salle s ON s.id = f.id;                 -- film 1 en salle 1, film 2 en salle 2, film 3 en salle 3
