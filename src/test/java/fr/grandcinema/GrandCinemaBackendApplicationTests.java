package fr.grandcinema;


import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Vérifie que toute l'application démarre : configuration, sécurité, et migrations Flyway
 * appliquées sur un vrai PostgreSQL (Testcontainers), indépendamment de la base locale.
 */
@SpringBootTest
@Testcontainers
class GrandCinemaBackendApplicationTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16");

    @Test
    void contextLoads() {
    }
}