package mg.improve.todo.integration;

import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
@TestInstance (TestInstance.Lifecycle.PER_CLASS)
public abstract class ConfIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16")
                    .withDatabaseName("todo_list")
                    .withUsername("postgres")
                    .withPassword("postgres");

    @DynamicPropertySource
    static void testProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add(
                "jwt.secret",
                () -> "integration-test-secret-key-that-is-at-least-256-bits-long-0123456789");
        registry.add("jwt.expiration.access", () -> 900000L);
        registry.add("jwt.expiration.refresh", () -> 604800000L);
        registry.add("app.cors.allowed-origin", () -> "http://localhost:3000");
    }
}