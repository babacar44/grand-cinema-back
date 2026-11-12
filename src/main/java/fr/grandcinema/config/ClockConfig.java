package fr.grandcinema.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Horloge injectable : les services utilisent LocalDateTime.now(clock).
 * En production, le fuseau Europe/Paris est garanti quel que soit le serveur ;
 * en test, on peut fixer l'heure.
 */
@Configuration
public class ClockConfig {

    @Bean
    Clock clock() {
        return Clock.system(ZoneId.of("Europe/Paris"));
    }
}
