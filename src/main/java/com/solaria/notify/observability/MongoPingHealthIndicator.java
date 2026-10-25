package com.solaria.notify.observability;

import org.bson.Document;
import org.springframework.boot.health.contributor.AbstractHealthIndicator;
import org.springframework.boot.health.contributor.Health;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

/**
 * Health do MongoDB com {@code ping} apenas no banco do serviço
 *
 * <p>
 * O indicador padrão do Spring Boot roda {@code hello} em todos os bancos listados,
 * inclusive {@code local}, e o usuário do Atlas não tem permissão nele: o health ficaria
 * sempre {@code DOWN}. O padrão fica desligado ({@code management.health.mongo.enabled=false})
 * e este bean assume o nome {@code mongo}, usado pelo grupo de readiness
 * </p>
 */
@Component("mongoHealthIndicator")
public class MongoPingHealthIndicator extends AbstractHealthIndicator {

    private static final Document PING = new Document("ping", 1);

    private final MongoTemplate mongoTemplate;

    public MongoPingHealthIndicator(MongoTemplate mongoTemplate) {
        super("MongoDB health check failed");
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        mongoTemplate.executeCommand(PING);
        builder.up().withDetail("database", mongoTemplate.getDb().getName());
    }
}
