package com.alejandrolunatech.disputes;

import org.springframework.boot.SpringApplication;

/** Runs the app against a throwaway PostgreSQL container: {@code ./mvnw spring-boot:test-run}. */
public class TestDisputesApplication {

    public static void main(String[] args) {
        SpringApplication.from(DisputesApplication::main).with(TestcontainersConfiguration.class).run(args);
    }
}
