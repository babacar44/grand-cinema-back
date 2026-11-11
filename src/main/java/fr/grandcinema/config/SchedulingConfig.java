package fr.grandcinema.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Active les tâches planifiées (libération des places dont le blocage a expiré). */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
