package com.example.barberia_reservas.config;

import java.time.Clock;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ReservasProperties.class)
public class AppConfig {

    /**
     * El reloj se INYECTA en vez de llamar a LocalDate.now() por todas partes.
     * Así, en las pruebas se puede usar un reloj falso y fijar "hoy" en cualquier fecha.
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
