package com.dating.owoke.notification;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.health.contributor.Status;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class NotificationServiceApplicationTests {

    @Autowired
    private HealthEndpoint healthEndpoint;

    @Test
    void contextStartsAndHealthIsUp() {
        assertThat(healthEndpoint.health().getStatus()).isEqualTo(Status.UP);
    }
}
