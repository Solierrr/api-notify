package com.solaria.notify.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Status;
import org.springframework.data.mongodb.UncategorizedMongoDbException;
import org.springframework.data.mongodb.core.MongoTemplate;

class MongoPingHealthIndicatorTests {

    @Test
    void pingOnServiceDatabaseIsUp() {
        MongoTemplate template = mock(MongoTemplate.class);
        MongoDatabase db = mock(MongoDatabase.class);
        when(template.getDb()).thenReturn(db);
        when(db.getName()).thenReturn("solaria_notify_db");

        var health = new MongoPingHealthIndicator(template).health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("database", "solaria_notify_db");
        verify(template).executeCommand(new Document("ping", 1));
    }

    @Test
    void failedPingIsDown() {
        MongoTemplate template = mock(MongoTemplate.class);
        when(template.executeCommand(any(Document.class)))
                .thenThrow(new UncategorizedMongoDbException("sem conexao", null));

        var health = new MongoPingHealthIndicator(template).health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    }
}
