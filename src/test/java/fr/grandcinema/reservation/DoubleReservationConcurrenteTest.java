package fr.grandcinema.reservation;

import fr.grandcinema.dto.reservation.ReservationRequest;
import fr.grandcinema.exception.ConflitException;
import fr.grandcinema.repository.reservation.ReservationPlaceRepository;
import fr.grandcinema.service.ReservationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test d'intégration du point de friction n°1 du projet (Bloc 2) : la double réservation.
 * Deux clients tentent de réserver la MÊME place au MÊME instant sur une vraie base PostgreSQL
 * (lancée dans Docker par Testcontainers). Une seule réservation doit réussir.
 */
@SpringBootTest
@Testcontainers
class DoubleReservationConcurrenteTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16");

    @Autowired ReservationService reservationService;
    @Autowired ReservationPlaceRepository reservationPlaceRepository;
    @Autowired JdbcTemplate jdbc;

    @Test
    void deuxClientsSurLaMemePlace_unSeulReussit() throws Exception {
        Long seanceId = jdbc.queryForObject("select min(id) from seance where debut > now()", Long.class);
        Long placeId = jdbc.queryForObject(
                "select min(p.id) from place p join seance s on s.salle_id = p.salle_id where s.id = ?", Long.class, seanceId);
        List<String> clients = List.of("client@grandcinema.fr", "agent@grandcinema.fr");

        AtomicInteger succes = new AtomicInteger();
        AtomicInteger refus = new AtomicInteger();
        CountDownLatch depart = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);

        List<Future<Object>> taches = clients.stream().map(email -> pool.submit(() -> {
            depart.await();                       // les deux clients partent exactement en même temps
            try {
                reservationService.reserver(email, new ReservationRequest(seanceId, List.of(placeId)));
                succes.incrementAndGet();
            } catch (ConflitException e) {
                refus.incrementAndGet();
            }
            return null;
        })).toList();

        depart.countDown();
        for (Future<?> t : taches) {
            t.get(30, TimeUnit.SECONDS);
        }
        pool.shutdown();

        assertThat(succes.get()).isEqualTo(1);
        assertThat(refus.get()).isEqualTo(1);
        assertThat(reservationPlaceRepository.findPlaceIdsOccupees(seanceId)).containsExactly(placeId);
    }
}
