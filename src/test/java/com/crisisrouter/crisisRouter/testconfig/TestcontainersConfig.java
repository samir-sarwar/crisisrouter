package com.crisisrouter.crisisRouter.testconfig;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfig {

    static final PostgreSQLContainer<?> postgres;

    static {
        // Docker Desktop 29.x requires minimum API version 1.44;
        // docker-java reads this system property as "api.version"
        System.setProperty("api.version", "1.44");
        postgres = new PostgreSQLContainer<>(
                DockerImageName.parse("postgres:16"))
                .withDatabaseName("crisis_router_test")
                .withUsername("test")
                .withPassword("test");
        postgres.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
}
