package mg.improve.todo.integration;

import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
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
}