-- =====================================================================
--  Le Grand Cinéma – Schéma initial de la base de données (PostgreSQL)
--  Fichier de migration Flyway : src/main/resources/db/migration/V1__schema_initial.sql
-- =====================================================================

-- Extension nécessaire pour interdire le chevauchement de deux séances dans une même salle
CREATE EXTENSION IF NOT EXISTS btree_gist;

-- ---------------------------------------------------------------------
-- UTILISATEUR : visiteurs inscrits, clients, agents d'accueil, administrateurs (E1)
-- ---------------------------------------------------------------------
CREATE TABLE utilisateur (
    id             BIGSERIAL    PRIMARY KEY,
    nom            VARCHAR(100) NOT NULL,
    prenom         VARCHAR(100) NOT NULL,
    email          VARCHAR(255) NOT NULL UNIQUE,
    mot_de_passe   VARCHAR(255) NOT NULL,              -- hash BCrypt, jamais en clair
    role           VARCHAR(20)  NOT NULL DEFAULT 'CLIENT'
                   CHECK (role IN ('CLIENT', 'AGENT', 'ADMIN')),
    actif          BOOLEAN      NOT NULL DEFAULT TRUE,
    date_creation  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ---------------------------------------------------------------------
-- FILM : catalogue (E2, géré par l'admin en E6)
-- ---------------------------------------------------------------------
CREATE TABLE film (
    id             BIGSERIAL    PRIMARY KEY,
    titre          VARCHAR(200) NOT NULL,
    synopsis       TEXT,
    duree_minutes  INTEGER      NOT NULL CHECK (duree_minutes > 0),
    genre          VARCHAR(50),
    realisateur    VARCHAR(150),
    age_minimum    INTEGER      NOT NULL DEFAULT 0 CHECK (age_minimum >= 0),
    affiche_url    VARCHAR(500),
    actif          BOOLEAN      NOT NULL DEFAULT TRUE  -- archivage au lieu de suppression
);

-- ---------------------------------------------------------------------
-- TYPE_SALLE : Premium/IMAX, Standard, VIP, Événementielle + tarif (US 18, US 39)
-- ---------------------------------------------------------------------
CREATE TABLE type_salle (
    code           VARCHAR(20)   PRIMARY KEY,
    libelle        VARCHAR(50)   NOT NULL,
    prix           NUMERIC(6, 2) NOT NULL CHECK (prix >= 0)
);

-- ---------------------------------------------------------------------
-- SALLE : les 10 salles du cinéma
-- ---------------------------------------------------------------------
CREATE TABLE salle (
    id               BIGSERIAL   PRIMARY KEY,
    nom              VARCHAR(50) NOT NULL UNIQUE,
    type_salle_code  VARCHAR(20) NOT NULL REFERENCES type_salle (code),
    capacite         INTEGER     NOT NULL CHECK (capacite > 0),
    active           BOOLEAN     NOT NULL DEFAULT TRUE
);

-- ---------------------------------------------------------------------
-- PLACE : places physiques d'une salle (plan de salle, US 16)
-- ---------------------------------------------------------------------
CREATE TABLE place (
    id        BIGSERIAL  PRIMARY KEY,
    salle_id  BIGINT     NOT NULL REFERENCES salle (id) ON DELETE CASCADE,
    rangee    VARCHAR(2) NOT NULL,                     -- A, B, C...
    numero    INTEGER    NOT NULL CHECK (numero > 0),
    pmr       BOOLEAN    NOT NULL DEFAULT FALSE,       -- place adaptée (US 22)
    CONSTRAINT uk_place_salle UNIQUE (salle_id, rangee, numero)
);

-- ---------------------------------------------------------------------
-- SEANCE : projection d'un film dans une salle à un horaire (E2, E6)
-- ---------------------------------------------------------------------
CREATE TABLE seance (
    id        BIGSERIAL   PRIMARY KEY,
    film_id   BIGINT      NOT NULL REFERENCES film (id),
    salle_id  BIGINT      NOT NULL REFERENCES salle (id),
    debut     TIMESTAMP   NOT NULL,
    fin       TIMESTAMP   NOT NULL,                    -- durée du film + battement nettoyage
    version   VARCHAR(10) NOT NULL DEFAULT 'VF' CHECK (version IN ('VF', 'VOSTFR')),
    CONSTRAINT ck_seance_horaires CHECK (fin > debut),
    -- US 38 : deux séances ne peuvent pas se chevaucher dans la même salle
    CONSTRAINT ex_seance_chevauchement EXCLUDE USING gist (
        salle_id WITH =,
        tsrange(debut, fin) WITH &&
    )
);

-- ---------------------------------------------------------------------
-- RESERVATION : réservation d'un client pour une séance (E3, E5)
-- ---------------------------------------------------------------------
CREATE TABLE reservation (
    id               BIGSERIAL     PRIMARY KEY,
    utilisateur_id   BIGINT        NOT NULL REFERENCES utilisateur (id),
    seance_id        BIGINT        NOT NULL REFERENCES seance (id),
    statut           VARCHAR(20)   NOT NULL DEFAULT 'EN_ATTENTE'
                     CHECK (statut IN ('EN_ATTENTE', 'CONFIRMEE', 'ANNULEE', 'EXPIREE', 'UTILISEE')),
    montant_total    NUMERIC(8, 2) NOT NULL DEFAULT 0 CHECK (montant_total >= 0),
    code_billet      UUID          UNIQUE,             -- contenu du QR code (US 30), généré à la confirmation
    date_creation    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_expiration  TIMESTAMP     NOT NULL,           -- fin du blocage de 10 minutes (US 19)
    date_utilisation TIMESTAMP,                        -- scan à l'entrée (US 32)
    version          INTEGER       NOT NULL DEFAULT 0, -- verrouillage optimiste (JPA @Version)
    CONSTRAINT uk_reservation_seance UNIQUE (id, seance_id)  -- support de la clé étrangère composite
);

-- ---------------------------------------------------------------------
-- RESERVATION_PLACE : places réservées (association Réservation <-> Place)
-- ---------------------------------------------------------------------
CREATE TABLE reservation_place (
    id              BIGSERIAL     PRIMARY KEY,
    reservation_id  BIGINT        NOT NULL,
    seance_id       BIGINT        NOT NULL,
    place_id        BIGINT        NOT NULL REFERENCES place (id),
    prix            NUMERIC(6, 2) NOT NULL CHECK (prix >= 0),
    -- garantit que la séance est bien celle de la réservation
    CONSTRAINT fk_resa_place_reservation FOREIGN KEY (reservation_id, seance_id)
        REFERENCES reservation (id, seance_id) ON DELETE CASCADE,
    -- FILET DE SÉCURITÉ ANTI DOUBLE RÉSERVATION : une place ne peut être prise
    -- qu'une seule fois par séance. Les lignes sont supprimées quand une
    -- réservation expire ou est annulée, ce qui libère la place.
    CONSTRAINT uk_place_par_seance UNIQUE (seance_id, place_id)
);

-- ---------------------------------------------------------------------
-- Index pour les requêtes fréquentes
-- ---------------------------------------------------------------------
CREATE INDEX idx_seance_film      ON seance (film_id);
CREATE INDEX idx_seance_debut     ON seance (debut);
CREATE INDEX idx_reservation_user ON reservation (utilisateur_id);
CREATE INDEX idx_reservation_exp  ON reservation (statut, date_expiration); -- libération des places expirées
CREATE INDEX idx_resa_place_resa  ON reservation_place (reservation_id);
