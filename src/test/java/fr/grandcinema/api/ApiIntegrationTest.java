package fr.grandcinema.api;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests d'intégration de l'API : de la requête HTTP jusqu'à la base PostgreSQL (Testcontainers).
 * Ils vérifient les contrôleurs, la sécurité (JWT, rôles) et la gestion des erreurs.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ApiIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16");

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    // ---------- Catalogue (US 9 à 11) ----------

    @Test
    void lesFilmsALAfficheSontPublics() throws Exception {
        mvc.perform(get("/api/films"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(3))));
    }

    @Test
    void laFicheDUnFilmContientSesSeances() throws Exception {
        mvc.perform(get("/api/films/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.film.titre").exists())
                .andExpect(jsonPath("$.seances[0].prix").exists());
    }

    @Test
    void unFilmInexistantRenvoie404() throws Exception {
        mvc.perform(get("/api/films/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Film introuvable"));
    }

    // ---------- Comptes et sécurité (US 1 à 4) ----------

    @Test
    void inscriptionPuisEmailDejaUtilise() throws Exception {
        String corps = """
                {"nom":"Diallo","prenom":"Awa","email":"awa.diallo@exemple.fr","motDePasse":"Secret123!"}""";
        mvc.perform(post("/api/auth/inscription").contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.role").value("CLIENT"));

        mvc.perform(post("/api/auth/inscription").contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isConflict());
    }

    @Test
    void unMotDePasseTropFaibleEstRefuse() throws Exception {
        mvc.perform(post("/api/auth/inscription").contentType(MediaType.APPLICATION_JSON).content("""
                        {"nom":"A","prenom":"B","email":"faible@exemple.fr","motDePasse":"azerty"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erreurs.motDePasse").exists());
    }

    @Test
    void unMauvaisMotDePasseRenvoie401() throws Exception {
        mvc.perform(post("/api/auth/connexion").contentType(MediaType.APPLICATION_JSON).content("""
                        {"email":"client@grandcinema.fr","motDePasse":"mauvais"}"""))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Identifiants incorrects"));
    }

    @Test
    void uneRouteProtegeeSansJetonRenvoie401() throws Exception {
        mvc.perform(get("/api/reservations/mes")).andExpect(status().isUnauthorized());
    }

    @Test
    void unJetonFalsifieEstRefuse() throws Exception {
        mvc.perform(get("/api/auth/moi").header("Authorization", "Bearer jeton.falsifie.invalide"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unClientNAccedePasAuBackOffice() throws Exception {
        mvc.perform(get("/api/admin/films").header("Authorization", bearer("client@grandcinema.fr", "Client123!")))
                .andExpect(status().isForbidden());
    }

    @Test
    void leProfilDeLUtilisateurConnecte() throws Exception {
        mvc.perform(get("/api/auth/moi").header("Authorization", bearer("client@grandcinema.fr", "Client123!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("client@grandcinema.fr"));
    }

    // ---------- Réservation complète (US 16 à 20, 30, 31) ----------

    @Test
    void parcoursCompletDeReservation() throws Exception {
        String jeton = bearer("client@grandcinema.fr", "Client123!");
        Long seanceId = jdbc.queryForObject("select max(id) from seance", Long.class);
        Long placeId = jdbc.queryForObject(
                "select max(p.id) from place p join seance s on s.salle_id = p.salle_id where s.id = ?", Long.class, seanceId);

        mvc.perform(get("/api/seances/" + seanceId + "/plan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.places", not(empty())));

        String reponse = mvc.perform(post("/api/reservations").header("Authorization", jeton)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"seanceId\":" + seanceId + ",\"placeIds\":[" + placeId + "]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statut").value("EN_ATTENTE"))
                .andReturn().getResponse().getContentAsString();
        Integer reservationId = JsonPath.read(reponse, "$.id");

        // la même place ne peut plus être réservée (US 19)
        mvc.perform(post("/api/reservations").header("Authorization", bearer("agent@grandcinema.fr", "Agent123!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"seanceId\":" + seanceId + ",\"placeIds\":[" + placeId + "]}"))
                .andExpect(status().isConflict());

        mvc.perform(post("/api/reservations/" + reservationId + "/confirmer").header("Authorization", jeton))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("CONFIRMEE"))
                .andExpect(jsonPath("$.codeBillet").exists());

        mvc.perform(get("/api/reservations/mes").header("Authorization", jeton))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(reservationId)));

        mvc.perform(get("/api/reservations/" + reservationId).header("Authorization", jeton))
                .andExpect(status().isOk());
    }

    @Test
    void plusDeDixPlacesRenvoie400() throws Exception {
        mvc.perform(post("/api/reservations").header("Authorization", bearer("client@grandcinema.fr", "Client123!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"seanceId\":1,\"placeIds\":[1,2,3,4,5,6,7,8,9,10,11]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void abandonDUneReservationEnAttente() throws Exception {
        String jeton = bearer("client@grandcinema.fr", "Client123!");
        Long seanceId = jdbc.queryForObject("select min(id) from seance where debut > now()", Long.class);
        Long placeId = jdbc.queryForObject(
                "select max(p.id) from place p join seance s on s.salle_id = p.salle_id where s.id = ?", Long.class, seanceId);
        String reponse = mvc.perform(post("/api/reservations").header("Authorization", jeton)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"seanceId\":" + seanceId + ",\"placeIds\":[" + placeId + "]}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Integer id = JsonPath.read(reponse, "$.id");

        mvc.perform(delete("/api/reservations/" + id).header("Authorization", jeton))
                .andExpect(status().isNoContent());
    }

    // ---------- Back-office (US 36, 38) ----------

    @Test
    void lAdminGereLesFilmsEtProgrammeUneSeanceSansChevauchement() throws Exception {
        String jeton = bearer("admin@grandcinema.fr", "Admin123!");

        String film = mvc.perform(post("/api/admin/films").header("Authorization", jeton)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titre":"Film de test","dureeMinutes":100,"genre":"Drame","ageMinimum":0}"""))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Integer filmId = JsonPath.read(film, "$.id");

        mvc.perform(put("/api/admin/films/" + filmId).header("Authorization", jeton)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titre":"Film de test modifié","dureeMinutes":105,"genre":"Drame","ageMinimum":0}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titre").value("Film de test modifié"));

        String seance = "{\"filmId\":" + filmId + ",\"salleId\":5,\"debut\":\"2035-06-21T20:00:00\",\"version\":\"VF\"}";
        mvc.perform(post("/api/admin/seances").header("Authorization", jeton)
                        .contentType(MediaType.APPLICATION_JSON).content(seance))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fin", startsWith("2035-06-21T22:05")));   // 105 min + 20 min

        mvc.perform(post("/api/admin/seances").header("Authorization", jeton)
                        .contentType(MediaType.APPLICATION_JSON).content(seance))
                .andExpect(status().isConflict());                            // chevauchement refusé

        mvc.perform(get("/api/admin/films").header("Authorization", jeton))
                .andExpect(status().isOk());

        mvc.perform(delete("/api/admin/films/" + filmId).header("Authorization", jeton))
                .andExpect(status().isNoContent());
    }

    /** Se connecte et renvoie l'en-tête « Bearer <jeton> ». */
    private String bearer(String email, String motDePasse) throws Exception {
        String reponse = mvc.perform(post("/api/auth/connexion").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"motDePasse\":\"" + motDePasse + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(reponse, "$.token");
    }
}
