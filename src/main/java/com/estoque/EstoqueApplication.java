package com.estoque;

import java.time.Clock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class EstoqueApplication {

    public static void main(String[] args) {
        SpringApplication.run(EstoqueApplication.class, args);
    }

    /**
     * Relógio usado para registrar a data e hora das movimentações (RN15).
     * Nos testes, pode ser substituído por um Clock.fixed(...) para tornar o horário previsível.
     */
    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }
}
