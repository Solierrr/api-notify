package com.solaria.notify.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.solaria.notify.model.enums.NotificationType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Histórico expira sozinho: o índice TTL remove o documento quando expireAt (createdAt + 90 dias) passa
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "notifications")
@CompoundIndex(def = "{'auth_id': 1, 'created_at': -1}")
public class Notification {

    @Id
    private UUID id;

    @Field("auth_id")
    private UUID authId;

    private NotificationType type;

    private String title;

    private String body;

    private Map<String, String> data;

    @Field("source_service")
    private String sourceService;

    @Field("created_at")
    private Instant createdAt;

    @Field("read_at")
    private Instant readAt;

    @Indexed(expireAfter = "0s")
    @Field("expire_at")
    private Instant expireAt;
}
