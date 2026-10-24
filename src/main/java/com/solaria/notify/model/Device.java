package com.solaria.notify.model;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.solaria.notify.model.enums.Platform;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// installationId é o Firebase Installation ID: um aparelho tem no máximo um dono por vez
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "devices")
public class Device {

    @Id
    private String installationId;

    @Indexed
    @Field("auth_id")
    private UUID authId;

    @Field("fcm_token")
    private String fcmToken;

    private Platform platform;

    @Field("app_version")
    private String appVersion;

    @Field("created_at")
    private Instant createdAt;

    @Field("updated_at")
    private Instant updatedAt;
}
